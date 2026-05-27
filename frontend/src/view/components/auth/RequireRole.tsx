// View layer — обёртка для маршрутов с проверкой роли.
// Гость → редирект на /login. Залогинен, но роль не подходит → 403-карточка.

import { Navigate, useLocation } from 'react-router-dom';
import type { Role } from '@/model/types';
import { useSessionStore } from '@/viewmodel/sessionStore';
import { Card, CardContent, CardHeader, CardTitle } from '@/view/components/ui/card';

interface Props {
  role: Role;
  children: React.ReactNode;
}

export function RequireRole({ role, children }: Props) {
  const user = useSessionStore((s) => s.user);
  const token = useSessionStore((s) => s.token);
  const location = useLocation();

  if (!token || !user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (user.role !== role) {
    return (
      <div className="container max-w-md py-12">
        <Card>
          <CardHeader>
            <CardTitle>Доступ запрещён</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm text-muted-foreground">
            <p>
              Требуется роль <strong>{role}</strong>, ваша текущая роль —{' '}
              <strong>{user.role}</strong>.
            </p>
            <p>Если считаете это ошибкой — обратитесь к администратору.</p>
          </CardContent>
        </Card>
      </div>
    );
  }
  return <>{children}</>;
}
