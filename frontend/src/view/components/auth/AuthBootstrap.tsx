// View layer — компонент-эффект: при монтировании запускает bootstrap()
// (валидация токена через /api/auth/me) и подписывается на событие 401,
// чтобы при истечении токена очистить сессию и перенаправить на /login.

import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { UNAUTHORIZED_EVENT } from '@/model/api/client';
import { useSessionStore } from '@/viewmodel/sessionStore';

export function AuthBootstrap() {
  const bootstrap = useSessionStore((s) => s.bootstrap);
  const logout = useSessionStore((s) => s.logout);
  const navigate = useNavigate();
  const location = useLocation();

  // Однократная проверка валидности токена при старте приложения.
  useEffect(() => {
    void bootstrap();
  }, [bootstrap]);

  // 401 от любого endpoint'а: выходим и перенаправляем на /login.
  useEffect(() => {
    const handler = () => {
      logout();
      // Не зацикливаемся: если уже на /login или /register, ничего не делаем.
      if (!location.pathname.startsWith('/login') && !location.pathname.startsWith('/register')) {
        navigate('/login', { replace: true, state: { from: location.pathname } });
      }
    };
    window.addEventListener(UNAUTHORIZED_EVENT, handler);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, handler);
  }, [logout, navigate, location.pathname]);

  return null;
}
