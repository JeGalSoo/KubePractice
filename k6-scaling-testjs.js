/**
 * =====================================================
 *  k6 오토스케일링 확인용 부하 테스트
 *  대상: gateway-server (localhost:30000) → backend
 *
 *  실행 전 확인:
 *    kubectl get hpa --watch   ← 터미널 1 (스케일링 실시간 확인)
 *    kubectl get pods --watch  ← 터미널 2 (파드 증가 확인)
 *    k6 run k6-scaling-test.js ← 터미널 3 (부하 실행)
 *
 *  ✅ 로그인 방식: setup() 함수로 테스트 시작 전 계정당 1회만 로그인
 *     → 동일 계정 동시 로그인으로 인한 InnoDB Lock Contention 완전 차단
 *     → VU는 setup()이 반환한 토큰 맵에서 꺼내 쓰므로 로그인 API 부하 없음
 * =====================================================
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// gateway NodePort로 접근 (배포 후 localhost:30000)
const BASE_URL = __ENV.BASE_URL || 'http://localhost:30000';

const errorRate = new Rate('error_rate');

// 테스트 계정 목록 (init.sql로 DB에 미리 삽입됨)
const TEST_USERS = [
  { email: 'test1@test.com', password: 'Test1234!' },
  { email: 'test2@test.com', password: 'Test1234!' },
  { email: 'test3@test.com', password: 'Test1234!' },
  { email: 'test4@test.com', password: 'Test1234!' },
  { email: 'test5@test.com', password: 'Test1234!' },
];

export const options = {
  stages: [
    { duration: '1m',  target: 50  },  // 워밍업
    { duration: '2m',  target: 200 },  // HPA 트리거 구간 (CPU 70% 목표)
    { duration: '3m',  target: 500 },  // 스케일아웃 확인
    { duration: '2m',  target: 200 },  // 스케일인 대기
    { duration: '1m',  target: 0   },  // 쿨다운
  ],
  thresholds: {
    'http_req_duration': ['p(95)<1000'],
    'error_rate':        ['rate<0.05'],
  },
};

/**
 * setup() — 테스트 시작 전 단 1회 실행되는 초기화 함수.
 *
 * 계정당 1번씩만 로그인하여 { email → accessToken } 맵을 구성합니다.
 * 반환값은 직렬화되어 모든 VU의 default 함수에 data 인자로 전달됩니다.
 *
 * [이전 방식의 문제]
 *   - vuToken = null 상태로 500 VU가 시작 → 첫 iteration에 모두 login() 호출
 *   - 계정당 100 VU가 동시에 deleteByUser + save → InnoDB Lock Contention
 *   - 로그인 실패 → accessToken 없음 → errorRate + 1
 *
 * [setup() 방식의 장점]
 *   - 계정 5개 → 로그인 API 정확히 5번만 호출 (순차)
 *   - 동시 로그인 경합 제로 → 리프레시 토큰 충돌 없음
 *   - 이후 모든 VU는 토큰 맵에서 꺼내 사용 → 로그인 관련 에러 원천 차단
 */
export function setup() {
  const tokenMap = {};

  for (const user of TEST_USERS) {
    const res = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify(user),
      { headers: { 'Content-Type': 'application/json' } }
    );

    let token = null;
    try {
      token = JSON.parse(res.body).accessToken;
    } catch (_) {
      // 파싱 실패 시 null 유지
    }

    if (token) {
      tokenMap[user.email] = token;
      console.log(`✅ 로그인 성공: ${user.email}`);
    } else {
      console.error(`❌ 로그인 실패: ${user.email} (status: ${res.status}) — DB에 계정이 있는지 확인`);
    }

    // 순차 로그인: 계정 간 0.5초 간격 (RefreshToken 저장 완료 대기)
    sleep(0.5);
  }

  return tokenMap;
}

/**
 * default 함수 — VU마다 반복 실행되는 메인 부하 함수.
 * @param {Object} data - setup()이 반환한 tokenMap
 */
export default function (data) {
  // VU ID 기반으로 계정 분산 (VU 1→test1, VU 2→test2, ..., VU 6→test1, ...)
  const user = TEST_USERS[(__VU - 1) % TEST_USERS.length];
  const token = data[user.email];

  // setup()에서 로그인에 실패한 계정이 배정된 경우
  if (!token) {
    errorRate.add(1);
    sleep(1);
    return;
  }

  const headers = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };

  // 게시판 목록 (캐시 히트/미스 반복으로 CPU 자극)
  const r1 = http.get(`${BASE_URL}/api/boards?page=0&size=10`, headers);
  check(r1, { 'boards 200': (r) => r.status === 200 });
  errorRate.add(r1.status !== 200);

  sleep(0.2);

  // ES 검색 (CPU 집중)
  const keywords = ['테스트', '공지', '안녕'];
  const kw = keywords[Math.floor(Math.random() * keywords.length)];
  const r2 = http.get(`${BASE_URL}/api/boards/search?q=${kw}`, headers);
  check(r2, { 'search 200': (r) => r.status === 200 });
  errorRate.add(r2.status !== 200);

  sleep(0.3);

  // 채팅방 목록
  const r3 = http.get(`${BASE_URL}/api/chat/rooms`, headers);
  check(r3, { 'chat 200': (r) => r.status === 200 });
  errorRate.add(r3.status !== 200);

  sleep(Math.random() * 1 + 0.5);
}

export function handleSummary(data) {
  const p95 = data.metrics['http_req_duration']?.values?.['p(95)'] || 0;
  const rps  = data.metrics['http_reqs']?.values?.rate || 0;
  const err  = (data.metrics['error_rate']?.values?.rate || 0) * 100;

  console.log(`
╔══════════════════════════════════════════════╗
║       오토스케일링 부하 테스트 결과              ║
╠══════════════════════════════════════════════╣
║  p95 응답시간 : ${String(p95.toFixed(0) + ' ms').padEnd(28)}║
║  RPS          : ${String(rps.toFixed(1)).padEnd(28)}║
║  에러율       : ${String(err.toFixed(2) + ' %').padEnd(28)}║
╚══════════════════════════════════════════════╝

📌 README에 기록할 내용:
- 부하 테스트 전 파드 수: 1개
- 스케일아웃 트리거: CPU 70% 초과
- 최대 파드 수: 5개
- 스케일아웃 소요 시간: kubectl get hpa 결과 참고
  `);

  return { 'stdout': '' };
}
