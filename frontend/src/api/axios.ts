import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// 토큰 헤더 추가 인터셉터 (로그인 연동 전 임시 구성)
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  } else {
    // 백엔드의 @AuthenticationPrincipal 처리를 위해 임시 Basic Auth나 특정 토큰 구조를 넣어줄 수도 있습니다.
    // 현재는 토큰이 없으면 그냥 보냅니다.
  }
  return config;
});

export default api;
