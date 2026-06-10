export interface Board {
  id: number;
  title: string;
  content: string;
  createdAt: string;
  updatedAt: string;
  user?: {
    username: string;
  };
}

export interface BoardRequestDto {
  title: string;
  content: string;
}

export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    offset: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: number;
  email: string;
  name: string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  phone: string;
  birthDate: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
}
