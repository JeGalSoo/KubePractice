import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 테스트 옵션 설정
export const options = {
  stages: [
    { duration: '10s', target: 50 },  // 10초 동안 50명까지 증가
    { duration: '30s', target: 200 }, // 30초 동안 200명 동시 접속 유지 (부하 발생)
    { duration: '10s', target: 0 },   // 10초 동안 0명으로 감소
  ],
};

// 2. Setup 단계: 테스트 시작 전 딱 1번 실행되어 토큰을 발급받음
export function setup() {
  const loginRes = http.post('http://host.docker.internal:8080/api/auth/login', JSON.stringify({
    email: 'test@example.com', // 앱에 존재하는 테스트 유저
    password: 'password123'    // 설정된 비밀번호 (실제 비번에 맞게 수정 필요)
  }), {
    headers: { 'Content-Type': 'application/json' }
  });

  let token = null;
  if (loginRes.status === 200) {
    const body = JSON.parse(loginRes.body);
    token = body.accessToken;
  }
  return { token: token };
}

// 3. 메인 가상 유저(VU) 로직: 반복적으로 실행되는 실제 부하 발생 코드
export default function (data) {
  if (!data.token) {
    console.error("No token, skipping test. Check test credentials.");
    sleep(1);
    return;
  }

  const params = {
    headers: {
      'Authorization': `Bearer ${data.token}`,
      'Content-Type': 'application/json'
    },
  };

  // JWT가 포함된 상태로 API에 지속적인 요청 -> 백엔드는 토큰 검증하며 DB(혹은 Redis)에서 User조회
  const res = http.get('http://host.docker.internal:8080/api/boards', params);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(0.1);
}
