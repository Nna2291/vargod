// View layer — каркас админ-панели: side-nav + контент через <Outlet/>.

import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import {
  ClipboardList,
  Database,
  FlaskConical,
  LayoutDashboard,
  Menu,
  ShieldCheck,
  Users,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/view/components/ui/button';
import { Sheet, SheetContent, SheetTrigger } from '@/view/components/ui/sheet';

const NAV_ITEMS = [
  { to: '/admin', label: 'Дашборд', icon: LayoutDashboard, end: true },
  { to: '/admin/users', label: 'Пользователи', icon: Users, end: false },
  { to: '/admin/models', label: 'Модели', icon: Database, end: false },
  { to: '/admin/validators', label: 'Валидаторы', icon: ShieldCheck, end: false },
  { to: '/admin/experiments', label: 'Эксперименты', icon: FlaskConical, end: false },
  { to: '/admin/audit', label: 'Аудит', icon: ClipboardList, end: false },
];

export function AdminLayout() {
  const [open, setOpen] = useState(false);

  return (
    <div className="container py-6">
      <div className="grid gap-6 lg:grid-cols-[220px_1fr]">
        {/* Desktop side-nav */}
        <aside className="hidden lg:block">
          <SideNav />
        </aside>

        {/* Mobile: sheet-trigger */}
        <div className="lg:hidden">
          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger asChild>
              <Button variant="outline" size="sm">
                <Menu className="mr-2 h-4 w-4" />
                Меню админ-панели
              </Button>
            </SheetTrigger>
            <SheetContent side="left" className="w-72">
              <div className="mt-6">
                <SideNav onNavigate={() => setOpen(false)} />
              </div>
            </SheetContent>
          </Sheet>
        </div>

        <main className="min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

function SideNav({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav className="space-y-1">
      <h2 className="px-3 pb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
        Админ-панель
      </h2>
      {NAV_ITEMS.map((item) => {
        const Icon = item.icon;
        return (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            onClick={onNavigate}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-accent text-accent-foreground'
                  : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
              )
            }
          >
            <Icon className="h-4 w-4" />
            {item.label}
          </NavLink>
        );
      })}
    </nav>
  );
}
