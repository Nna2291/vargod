// Model layer — доступ к endpoint'ам аутентификации (JWT).

import { http } from './client';
import type { AuthResponse, AuthUser } from '../types';

/** POST /api/auth/register — создание аккаунта + выдача JWT. */
export async function register(email: string, password: string): Promise<AuthResponse> {
  const res = await http.post<AuthResponse>('/auth/register', { email, password });
  return res.data;
}

/** POST /api/auth/login — вход по email/password, выдача JWT. */
export async function login(email: string, password: string): Promise<AuthResponse> {
  const res = await http.post<AuthResponse>('/auth/login', { email, password });
  return res.data;
}

/** GET /api/auth/me — проверка валидности токена и получение профиля. */
export async function me(): Promise<AuthUser> {
  const res = await http.get<AuthUser>('/auth/me');
  return res.data;
}
