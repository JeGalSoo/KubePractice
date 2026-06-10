import api from './axios';
import type { Board, BoardRequestDto, Page } from '../types';

export const boardApi = {
  getBoards: async (page = 0, size = 10) => {
    const response = await api.get<Page<Board>>(`/boards?page=${page}&size=${size}`);
    return response.data;
  },

  getBoard: async (id: number) => {
    const response = await api.get<Board>(`/boards/${id}`);
    return response.data;
  },

  createBoard: async (data: BoardRequestDto) => {
    const response = await api.post<Board>('/boards', data);
    return response.data;
  },

  updateBoard: async (id: number, data: BoardRequestDto) => {
    const response = await api.put<Board>(`/boards/${id}`, data);
    return response.data;
  },

  deleteBoard: async (id: number) => {
    await api.delete(`/boards/${id}`);
  },
};
