// ViewModel layer — хранилище состояния сессии (JWT + профиль).
// Zustand-store с сохранением в localStorage. Связывает View с Model-слоем
// (api/auth). Гость — token === null. JSX в этом слое отсутствует.

import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthUser, Role } from '../model/types';
import * as authApi from '../model/api/auth';
import { toApiError } from '../model/api/client';

interface SessionState {
  /** JWT-токен текущей сессии; null для гостя или после выхода. */
  token: string | null;
  /** Профиль текущего пользователя; null для гостя. */
  user: AuthUser | null;
  /** Признак выполнения сетевого запроса. */
  loading: boolean;
  /** Сообщение об ошибке последнего действия с сессией. */
  error: string | null;

  /** Вход по email/password. Возвращает true при успехе. */
  login: (email: string, password: string) => Promise<boolean>;
  /** Регистрация и автологин. Возвращает true при успехе. */
  register: (email: string, password: string) => Promise<boolean>;
  /** Выход. Очищает локальное состояние и сохранённую сессию. */
  logout: () => void;
  /**
   * Проверка валидности токена при старте приложения: вызывает
   * GET /api/auth/me; при ошибке — сбрасывает сессию (logout).
   */
  bootstrap: () => Promise<void>;
}

export const useSessionStore = create<SessionState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      loading: false,
      error: null,

      login: async (email, password) => {
        set({ loading: true, error: null });
        try {
          const resp = await authApi.login(email, password);
          set({ token: resp.token, user: resp.user, loading: false });
          return true;
        } catch (e) {
          set({ error: toApiError(e).message, loading: false });
          return false;
        }
      },

      register: async (email, password) => {
        set({ loading: true, error: null });
        try {
          const resp = await authApi.register(email, password);
          set({ token: resp.token, user: resp.user, loading: false });
          return true;
        } catch (e) {
          set({ error: toApiError(e).message, loading: false });
          return false;
        }
      },

      logout: () => set({ token: null, user: null, error: null }),

      bootstrap: async () => {
        if (!get().token) return;
        try {
          const user = await authApi.me();
          set({ user });
        } catch {
          set({ token: null, user: null });
        }
      },
    }),
    {
      name: 'vargod-session-v2',
      partialize: (state) => ({ token: state.token, user: state.user }),
    },
  ),
);

/** Признак залогиненного исследователя — селектор для View-слоя. */
export const isResearcher = (state: SessionState): boolean =>
  state.user !== null && state.user.role === 'RESEARCHER';

/** Признак залогиненного администратора. */
export const isAdmin = (state: SessionState): boolean =>
  state.user !== null && state.user.role === 'ADMIN';

/** Признак залогиненного пользователя (любая роль). */
export const isAuthenticated = (state: SessionState): boolean =>
  state.user !== null && state.token !== null;

/** Возвращает роль текущего пользователя или null для гостя. */
export const currentRole = (state: SessionState): Role | null =>
  state.user?.role ?? null;
