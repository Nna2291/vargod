// Model layer — HTTP-клиент REST API.
// Единственная точка обращения фронтенда к backend. Знает только о домене
// и протоколе; ничего не знает об UI. Слой ViewModel вызывает функции-обёртки
// из этого модуля и его соседей.

import axios, { AxiosError, type AxiosInstance } from 'axios';
import type { ApiError } from '../types';

/**
 * Поставщик JWT-токена текущего пользователя.
 * Внедряется при старте приложения (см. main.tsx), чтобы клиент Model-слоя
 * не зависел от Zustand-хранилища ViewModel-слоя — направление зависимостей
 * остаётся View → ViewModel → Model.
 */
let authTokenProvider: () => string | null = () => null;

/** Регистрирует функцию получения JWT-токена для заголовка Authorization. */
export function setAuthTokenProvider(provider: () => string | null): void {
  authTokenProvider = provider;
}

/** Событие, которое диспатчится при ответе 401 — UI должен перейти на /login. */
export const UNAUTHORIZED_EVENT = 'vargod:unauthorized';

/** Базовый экземпляр axios. Путь /api относительный — проксируется Vite/nginx. */
export const http: AxiosInstance = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Перехватчик запроса: автоматически добавляет Authorization: Bearer <token>,
// если токен присутствует (вошедший пользователь). Гость — без токена.
http.interceptors.request.use((config) => {
  const token = authTokenProvider();
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

// Перехватчик ответа: при 401 диспатчим окно-событие — слой View подхватит его
// (например, через AuthBootstrap), вызовет logout() и перенаправит на /login.
http.interceptors.response.use(
  (res) => res,
  (err: unknown) => {
    if (axios.isAxiosError(err) && err.response?.status === 401) {
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT));
    }
    return Promise.reject(err);
  },
);

/**
 * Преобразует ошибку axios в унифицированную ApiError.
 * Для ответа backend { error, message, details } сохраняет поля как есть;
 * для сетевых сбоев формирует понятное пользователю сообщение.
 */
export function toApiError(err: unknown): ApiError {
  if (axios.isAxiosError(err)) {
    const axiosErr = err as AxiosError<Partial<ApiError>>;
    const data = axiosErr.response?.data;
    if (data && typeof data === 'object') {
      return {
        error: data.error ?? 'ERROR',
        message: data.message ?? axiosErr.message,
        details: Array.isArray(data.details) ? data.details : [],
        status: axiosErr.response?.status ?? 0,
      };
    }
    return {
      error: 'NETWORK_ERROR',
      message: 'Не удалось связаться с сервером. Проверьте, запущен ли API.',
      details: [],
      status: axiosErr.response?.status ?? 0,
    };
  }
  return {
    error: 'UNKNOWN_ERROR',
    message: err instanceof Error ? err.message : 'Неизвестная ошибка',
    details: [],
    status: 0,
  };
}
