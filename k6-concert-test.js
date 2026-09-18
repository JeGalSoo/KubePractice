/**
 * ============================================================
 *  k6 콘서트 티켓팅 부하 테스트 (reservation-server 전용)
 *  작성일: 2025
 *
 *  [서버 구성]
 *   Gateway  : http://localhost:8080  (JWT 인증 → X-User-Id 헤더 전달)
 *   Backend  : http://localhost:8080/api/auth/login  (토큰 발급)
 *   Reservation: http://localhost:8080/api/concerts/**
 *     ├── GET  /api/concerts                      공연 목록
 *     ├── GET  /api/concerts/{id}/seats?sectionId 좌석 목록
 *     ├── POST /api/concerts/{id}/reserve/db-lock    전략 A
 *     ├── POST /api/concerts/{id}/reserve/redis-lock 전략 B
 *     ├── POST /api/concerts/{id}/reserve/kafka      전략 C
 *     ├── POST /api/concerts/{id}/queue/enter        대기열 진입
 *     └── GET  /api/concerts/{id}/queue/status       대기 순위 조회
 *
 *  [실행 방법]
 *   # 전략 A — DB 비관적 락 (베이스라인)
 *   $env:STRATEGY="db-lock";  $env:SCENARIO="ticketing"; k6 run k6-concert-test.js
 *
 *   # 전략 B — Redisson 분산 락
 *   $env:STRATEGY="redis-lock"; $env:SCENARIO="ticketing"; k6 run k6-concert-test.js
 *
 *   # 전략 C — Kafka 비동기
 *   $env:STRATEGY="kafka"; $env:SCENARIO="ticketing"; k6 run k6-concert-test.js
 *
 *   # 대기열 시나리오 (전략과 무관)
 *   $env:SCENARIO="queue"; k6 run k6-concert-test.js
 *
 *   # 전략 비교 전체 (순차 실행)
 *   foreach ($s in @("db-lock","redis-lock","kafka")) {
 *     $env:STRATEGY=$s; $env:SCENARIO="ticketing"; k6 run k6-concert-test.js
 *   }
 *
 *  [사전 준비]
 *   1. docker-compose up -d (MySQL, Redis, Kafka 실행)
 *   2. config-server → gateway → backend → reservation-server 순으로 기동
 *   3. Flyway V2 마이그레이션으로 테스트 데이터 자동 삽입됨
 *      (콘서트 3개, Section id=1,4,7 각 100좌석)
 *   4. init.sql의 test1~5@test.com 계정 확인
 * ============================================================
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';

// ============================================================
// 환경 변수 설정
// ============================================================
const BASE_URL   = __ENV.BASE_URL   || 'http://localhost:8080';
const STRATEGY   = __ENV.STRATEGY   || 'redis-lock';  // db-lock | redis-lock | kafka
const CONCERT_ID = __ENV.CONCERT_ID || '1';
const SECTION_ID = __ENV.SECTION_ID || '1';           // Concert 1의 S석 (section_id=1)
const SCENARIO   = __ENV.SCENARIO   || 'ticketing';   // ticketing | queue | mixed

// ============================================================
// 커스텀 메트릭
// ============================================================
const errorRate        = new Rate('custom_error_rate');         // 실제 오류 (5xx, 4xx 중 409 제외)
const seatConflictRate = new Rate('custom_seat_conflict_rate'); // 409 좌석 충돌 (정상 동시성 결과)
const reserveSuccess   = new Rate('custom_reserve_success');    // 예약 성공률
const reserveDuration  = new Trend('custom_reserve_duration_ms', true); // 예약 응답시간
const queueDuration    = new Trend('custom_queue_duration_ms', true);   // 대기열 응답시간
const totalReserved    = new Counter('custom_total_reserved');  // 총 예약 성공 수
const queueSize        = new Gauge('custom_queue_size');        // 실시간 대기열 인원

// ============================================================
// 테스트 계정 (init.sql + Flyway V2 데이터와 일치)
// ============================================================
const TEST_USERS = [
  { email: 'test1@test.com', password: 'Test1234!', deviceId: 'k6-device-1' },
  { email: 'test2@test.com', password: 'Test1234!', deviceId: 'k6-device-2' },
  { email: 'test3@test.com', password: 'Test1234!', deviceId: 'k6-device-3' },
  { email: 'test4@test.com', password: 'Test1234!', deviceId: 'k6-device-4' },
  { email: 'test5@test.com', password: 'Test1234!', deviceId: 'k6-device-5' },
];

// ============================================================
// 시나리오별 옵션
// ============================================================
const SCENARIO_OPTIONS = {

  // 티켓팅 오픈 시뮬레이션 (Thundering Herd)
  ticketing: {
    stages: [
      { duration: '20s', target: 30  },  // 워밍업: 서버 JIT 컴파일 유도
      { duration: '30s', target: 200 },  // 급격한 트래픽 증가 (오픈 시각 시뮬레이션)
      { duration: '2m',  target: 500 },  // 최대 부하: 티켓팅 피크
      { duration: '1m',  target: 200 },  // 완만한 감소
      { duration: '20s', target: 0   },  // 쿨다운
  },
    thresholds: {
      'http_req_duration':       ['p(95)<3000'],  // 95%가 3초 이내
      'custom_error_rate':       ['rate<0.01'],   // 실제 오류 1% 미만
      'custom_reserve_success':  ['rate>0.10'],   // 예약 성공률 10% 이상 (좌석 제한)
    },
  },

  // 대기열 시스템 테스트
  queue: {
    stages: [
      { duration: '10s', target: 100 },
      { duration: '1m',  target: 1000 },  // 1000명 동시 대기열 진입
      { duration: '2m',  target: 1000 },  // 유지
      { duration: '20s', target: 0   },
    ],
    thresholds: {
      'http_req_duration':    ['p(95)<500'],
      'custom_error_rate':    ['rate<0.01'],
      'custom_queue_duration_ms': ['p(95)<300'],
    },
  },

  // 혼합 (실제 사용 패턴)
  mixed: {
    stages: [
      { duration: '30s', target: 50  },
      { duration: '2m',  target: 300 },
      { duration: '3m',  target: 300 },
      { duration: '30s', target: 0   },
    ],
    thresholds: {
      'http_req_duration': ['p(95)<2000'],
      'custom_error_rate': ['rate<0.01'],
    },
  },
};

export const options = {
  ...(SCENARIO_OPTIONS[SCENARIO] || SCENARIO_OPTIONS.ticketing),
};

// ============================================================
// setup() — 테스트 시작 전 1회 실행
// 1. 계정별 로그인 → { userId, accessToken } 맵 구성
// 2. 좌석 목록 사전 조회
// ============================================================
export function setup() {
  console.log(`\n🎵 콘서트 티켓팅 테스트 시작`);
  console.log(`   전략: ${STRATEGY} | 시나리오: ${SCENARIO} | 콘서트: ${CONCERT_ID}\n`);

  const tokenMap = {};

  for (const user of TEST_USERS) {
    const res = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({
        email: user.email,
        password: user.password,
        deviceId: user.deviceId,  // 멀티 디바이스 세션 충돌 방지
      }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    if (res.status === 200) {
      const body = JSON.parse(res.body);
      tokenMap[user.email] = {
        accessToken: body.accessToken,
        userId: body.userId,            // AuthResponse.userId (Long)
        email: body.email,
      };
      console.log(`✅ 로그인 성공: ${user.email} (userId=${body.userId})`);
    } else {
      console.error(`❌ 로그인 실패: ${user.email} → HTTP ${res.status} | ${res.body.slice(0, 100)}`);
    }

    sleep(0.5); // 순차 로그인 (InnoDB Lock Contention 방지)
  }

  // 좌석 목록 사전 조회 (section_id=1 기준)
  const seatsRes = http.get(
    `${BASE_URL}/api/concerts/${CONCERT_ID}/seats?sectionId=${SECTION_ID}`,
    { headers: { 'Content-Type': 'application/json' } }
  );

  let seatIds = [];
  if (seatsRes.status === 200) {
    try {
      const seats = JSON.parse(seatsRes.body);
      seatIds = seats.map(s => s.id).filter(Boolean);
      console.log(`✅ 좌석 조회 완료: ${seatIds.length}개 (section=${SECTION_ID})`);
    } catch (e) {
      console.error('❌ 좌석 파싱 실패: ' + seatsRes.body.slice(0, 100));
    }
  } else {
    console.error(`❌ 좌석 조회 실패: HTTP ${seatsRes.status}`);
  }

  return { tokenMap, seatIds };
}

// ============================================================
// default — VU마다 반복 실행되는 메인 함수
// ============================================================
export default function (data) {
  const { tokenMap, seatIds } = data;

  // VU 번호로 계정 분산 (1→test1, 2→test2, ..., 6→test1, ...)
  const userIndex = (__VU - 1) % TEST_USERS.length;
  const userKey   = TEST_USERS[userIndex].email;
  const userInfo  = tokenMap[userKey];

  if (!userInfo) {
    errorRate.add(1);
    sleep(1);
    return;
  }

  // 공통 헤더
  // - Authorization: Gateway의 JWT 필터가 검증 후 X-User-Id로 변환
  // - X-User-Id: 로컬 테스트 시 Gateway 없이 직접 reservation-server 호출할 경우 사용
  const authHeaders = {
    headers: {
      'Content-Type':  'application/json',
      'Authorization': `Bearer ${userInfo.accessToken}`,
      'X-User-Id':     String(userInfo.userId),  // Gateway bypass 시 사용
    },
  };

  // 시나리오 분기
  if (SCENARIO === 'queue') {
    runQueueScenario(userInfo, authHeaders, CONCERT_ID);
  } else if (SCENARIO === 'mixed') {
    runMixedScenario(userInfo, authHeaders, seatIds, CONCERT_ID);
  } else {
    runTicketingScenario(userInfo, authHeaders, seatIds, CONCERT_ID);
  }
}

// ============================================================
// 시나리오 1: 티켓팅 (선착순 좌석 예약)
// ============================================================
function runTicketingScenario(userInfo, headers, seatIds, concertId) {
  group('ticketing', () => {

    // 1. 공연 정보 조회 (읽기 부하)
    group('concert_info', () => {
      const r = http.get(`${BASE_URL}/api/concerts/${concertId}`, headers);
      check(r, { '공연 조회 200': (res) => res.status === 200 });
    });

    sleep(0.1);

    // 2. 좌석 목록 조회
    group('seat_list', () => {
      const r = http.get(
        `${BASE_URL}/api/concerts/${concertId}/seats?sectionId=${SECTION_ID}`,
        headers
      );
      check(r, { '좌석 목록 200': (res) => res.status === 200 });
    });

    sleep(0.1);

    // 3. 좌석 선택 & 예약 (핵심 — 동시성 경쟁 구간)
    group('reserve', () => {
      // 랜덤 좌석 선택 (같은 좌석에 몰리게 하여 경쟁 극대화)
      // seatIds가 비어있으면 1~100 범위로 직접 지정
      let seatId;
      if (seatIds && seatIds.length > 0) {
        // 앞 30개 좌석에 집중 → 충돌 강제 유발
        const pool = seatIds.slice(0, Math.min(30, seatIds.length));
        seatId = pool[Math.floor(Math.random() * pool.length)];
      } else {
        seatId = Math.floor(Math.random() * 30) + 1;
      }

      const payload = JSON.stringify({
        seatId:    seatId,
        concertId: parseInt(concertId),
      });

      const start = Date.now();
      const res = http.post(
        `${BASE_URL}/api/concerts/${concertId}/reserve/${STRATEGY}`,
        payload,
        headers
      );
      const elapsed = Date.now() - start;

      reserveDuration.add(elapsed);

      if (res.status === 200 || res.status === 201) {
        // 예약 성공
        reserveSuccess.add(1);
        errorRate.add(0);
        seatConflictRate.add(0);
        totalReserved.add(1);
        check(res, {
          '예약 성공 (200)':    (r) => r.status === 200,
          '응답에 reservationId': (r) => {
            try { return !!JSON.parse(r.body).id; } catch { return false; }
          },
          '전략 태그 확인':     (r) => {
            try {
              const b = JSON.parse(r.body);
              return b.strategy === STRATEGY.replace('-', '_').toUpperCase();
            } catch { return false; }
          },
        });

      } else if (res.status === 409) {
        // 좌석 충돌 (동시성 처리의 정상 결과 — 오류율에 미포함)
        reserveSuccess.add(0);
        errorRate.add(0);
        seatConflictRate.add(1);
        check(res, { '좌석 충돌 409 (정상)': (r) => r.status === 409 });

      } else if (res.status === 400) {
        // 잘못된 요청 (판매 기간 외 등)
        reserveSuccess.add(0);
        errorRate.add(0);
        seatConflictRate.add(0);
        check(res, { '400 Bad Request': (r) => r.status === 400 });

      } else {
        // 5xx 또는 예상치 못한 오류 — 실제 오류로 집계
        reserveSuccess.add(0);
        errorRate.add(1);
        seatConflictRate.add(0);
        console.error(`[VU${__VU}] 예상치 못한 오류: ${res.status} | ${res.body.slice(0, 150)}`);
      }
    });

    sleep(Math.random() * 0.3 + 0.1);
  });
}

// ============================================================
// 시나리오 2: 대기열 (Thundering Herd 입장 제어)
// ============================================================
function runQueueScenario(userInfo, headers, concertId) {
  group('queue', () => {

    // 대기열 진입
    group('queue_enter', () => {
      const start = Date.now();
      const res = http.post(
        `${BASE_URL}/api/concerts/${concertId}/queue/enter`,
        null,
        headers
      );
      queueDuration.add(Date.now() - start);

      if (res.status === 200) {
        errorRate.add(0);
        try {
          const body = JSON.parse(res.body);
          queueSize.add(body.totalWaiting || 0);
          check(res, {
            '대기열 진입 200':      (r) => r.status === 200,
            '순위 정보 포함':        (r) => body.rank !== undefined,
            '입장 토큰 여부 포함':   (r) => body.hasToken !== undefined,
          });
          if (body.hasToken) {
            console.log(`[VU${__VU}] userId=${userInfo.userId} 입장 토큰 보유 — 즉시 예약 가능`);
          }
        } catch (_) {}
      } else {
        errorRate.add(1);
      }
    });

    sleep(1);

    // 내 순위 폴링 (실제 클라이언트는 SSE 또는 polling으로 확인)
    group('queue_status', () => {
      const start = Date.now();
      const res = http.get(
        `${BASE_URL}/api/concerts/${concertId}/queue/status`,
        headers
      );
      queueDuration.add(Date.now() - start);

      check(res, {
        '순위 조회 200': (r) => r.status === 200,
      });

      if (res.status !== 200) {
        errorRate.add(1);
      } else {
        errorRate.add(0);
      }
    });

    sleep(Math.random() * 2 + 1); // 실제 사용자 행동 간격
  });
}

// ============================================================
// 시나리오 3: 혼합 (조회 70% + 대기열 20% + 예약 10%)
// ============================================================
function runMixedScenario(userInfo, headers, seatIds, concertId) {
  const rand = Math.random();

  if (rand < 0.5) {
    // 50% — 공연/좌석 조회 (읽기 부하)
    group('browse', () => {
      const r1 = http.get(`${BASE_URL}/api/concerts`, headers);
      check(r1, { '공연 목록 200': (r) => r.status === 200 });
      errorRate.add(r1.status >= 500 ? 1 : 0);

      sleep(0.2);

      const r2 = http.get(
        `${BASE_URL}/api/concerts/${concertId}/seats?sectionId=${SECTION_ID}`,
        headers
      );
      check(r2, { '좌석 목록 200': (r) => r.status === 200 });
      errorRate.add(r2.status >= 500 ? 1 : 0);
    });

  } else if (rand < 0.7) {
    // 20% — 대기열 진입
    runQueueScenario(userInfo, headers, concertId);

  } else {
    // 30% — 예약 시도
    runTicketingScenario(userInfo, headers, seatIds, concertId);
  }

  sleep(Math.random() * 1 + 0.5);
}

// ============================================================
// handleSummary — 테스트 완료 후 결과 출력
// ============================================================
export function handleSummary(data) {
  const m = data.metrics;

  const get = (name, field) => {
    const val = m[name]?.values?.[field];
    return val !== undefined ? val : 0;
  };

  const p50  = get('custom_reserve_duration_ms', 'p(50)').toFixed(1);
  const p95  = get('custom_reserve_duration_ms', 'p(95)').toFixed(1);
  const p99  = get('custom_reserve_duration_ms', 'p(99)').toFixed(1);
  const rps  = get('http_reqs', 'rate').toFixed(2);
  const err  = (get('custom_error_rate', 'rate') * 100).toFixed(2);
  const conf = (get('custom_seat_conflict_rate', 'rate') * 100).toFixed(2);
  const succ = (get('custom_reserve_success', 'rate') * 100).toFixed(2);
  const tot  = get('custom_total_reserved', 'count');
  const qp95 = get('custom_queue_duration_ms', 'p(95)').toFixed(1);
  const totalReqs = get('http_reqs', 'count');

  const strategyTag = STRATEGY === 'db-lock'    ? '🔒 DB 비관적 락'
                    : STRATEGY === 'redis-lock'  ? '⚡ Redis 분산 락'
                    : STRATEGY === 'kafka'       ? '🚀 Kafka 비동기'
                    : STRATEGY;

  const result = `
╔═══════════════════════════════════════════════════════════════╗
║          콘서트 티켓팅 부하 테스트 결과                          ║
╠═══════════════════════════════════════════════════════════════╣
║  전략         : ${strategyTag.padEnd(45)}║
║  시나리오     : ${SCENARIO.padEnd(45)}║
║  대상 콘서트  : ID=${String(CONCERT_ID).padEnd(43)}║
╠═══════════════════════════════════════════════════════════════╣
║  총 HTTP 요청 : ${String(totalReqs + ' 건').padEnd(45)}║
║  RPS (TPS)    : ${String(rps + ' req/s').padEnd(45)}║
║  총 예약 성공 : ${String(tot + ' 건').padEnd(45)}║
╠═══════════════════════════════════════════════════════════════╣
║  [예약 응답시간]                                                ║
║  p50           : ${String(p50 + ' ms').padEnd(44)}║
║  p95           : ${String(p95 + ' ms').padEnd(44)}║
║  p99           : ${String(p99 + ' ms').padEnd(44)}║
╠═══════════════════════════════════════════════════════════════╣
║  [대기열 응답시간 p95] : ${String(qp95 + ' ms').padEnd(36)}║
╠═══════════════════════════════════════════════════════════════╣
║  예약 성공률  : ${String(succ + ' %').padEnd(45)}║
║  좌석 충돌율  : ${String(conf + ' % (409 — 정상 동시성)').padEnd(45)}║
║  실제 오류율  : ${String(err + ' % (5xx 기준)').padEnd(45)}║
╚═══════════════════════════════════════════════════════════════╝

📌 전략 비교 기준 (예상치):
   db-lock   : TPS ~30~50,    p95 ~300~800ms, 충돌율 낮음
   redis-lock: TPS ~500~800,  p95 ~30~100ms,  충돌율 중간
   kafka     : TPS ~2000~5000,p95 ~10~30ms,   충돌율 높음(비동기)

💡 다음 단계:
   1. Grafana에서 concert-reservation-dashboard.json 임포트
   2. Prometheus → concert_reservation_duration_seconds 쿼리
   3. Tomcat thread max 값 조정 후 재측정 (config-repo/reservation-server-dev.yml)
`;

  console.log(result);

  const filename = `results/concert-${STRATEGY}-${SCENARIO}-result.txt`;
  return {
    'stdout': '',
    [filename]: result,
  };
}
