// View layer — верхняя панель навигации с управлением сессией и темой.

import { Link, NavLink, useNavigate } from 'react-router-dom';
import { LogOut, Shield, User } from 'lucide-react';
import { cn } from '@/lib/utils';
import { isAdmin, isResearcher, useSessionStore } from '@/viewmodel/sessionStore';
import { Button } from '@/view/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/view/components/ui/dropdown-menu';
import { ThemeToggle } from './theme/ThemeToggle';

export function NavBar() {
  const navigate = useNavigate();
  const user = useSessionStore((s) => s.user);
  const researcher = useSessionStore(isResearcher);
  const admin = useSessionStore(isAdmin);
  const logout = useSessionStore((s) => s.logout);

  return (
    <header className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-16 items-center gap-4">
        <Link to="/" className="text-lg font-bold text-primary">
          Vargod
        </Link>

        <nav className="flex flex-1 items-center gap-1 text-sm">
          <NavLink to="/" end className={navLinkClass}>
            Каталог
          </NavLink>
          <NavLink to="/experiments" className={navLinkClass}>
            Эксперименты
          </NavLink>
          {(researcher || admin) && (
            <NavLink to="/comparisons" className={navLinkClass}>
              Сравнения
            </NavLink>
          )}
          {admin && (
            <NavLink to="/admin" className={navLinkClass}>
              Админ-панель
            </NavLink>
          )}
        </nav>

        <ThemeToggle />

        {user ? (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm" className="gap-2">
                <User className="h-4 w-4" />
                <span className="hidden md:inline">{user.email}</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuLabel className="font-normal">
                <div className="flex flex-col">
                  <span className="text-sm font-medium">{user.email}</span>
                  <span className="text-xs text-muted-foreground">{user.role}</span>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              {admin && (
                <DropdownMenuItem onClick={() => navigate('/admin')}>
                  <Shield className="mr-2 h-4 w-4" />
                  Админ-панель
                </DropdownMenuItem>
              )}
              <DropdownMenuItem onClick={logout}>
                <LogOut className="mr-2 h-4 w-4" />
                Выйти
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" asChild>
              <Link to="/login">Войти</Link>
            </Button>
            <Button size="sm" asChild>
              <Link to="/register">Регистрация</Link>
            </Button>
          </div>
        )}
      </div>
    </header>
  );
}

function navLinkClass({ isActive }: { isActive: boolean }): string {
  return cn(
    'rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground',
    isActive ? 'text-foreground' : 'text-muted-foreground',
  );
}
