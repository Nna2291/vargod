// View layer — главная страница админ-панели: KPI и недавний аудит.

import { useQuery } from '@tanstack/react-query';
import { Activity, Database, FlaskConical, ShieldCheck, Users } from 'lucide-react';
import * as adminApi from '@/model/api/admin';
import * as modelsApi from '@/model/api/models';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/view/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/view/components/ui/table';

export function AdminDashboardPage() {
  const usersQ = useQuery({ queryKey: ['admin', 'users'], queryFn: adminApi.listUsers });
  const modelsQ = useQuery({ queryKey: ['models'], queryFn: modelsApi.fetchModels });
  const validatorsQ = useQuery({
    queryKey: ['admin', 'validators'],
    queryFn: () => adminApi.listValidators(),
  });
  const experimentsQ = useQuery({
    queryKey: ['admin', 'experiments'],
    queryFn: () => adminApi.listAllExperiments(200, 0),
  });
  const auditQ = useQuery({
    queryKey: ['admin', 'audit', 'recent'],
    queryFn: () => adminApi.listAudit({ limit: 10, offset: 0 }),
  });

  const kpis = [
    { label: 'Пользователей', value: usersQ.data?.length ?? '—', icon: Users },
    { label: 'Моделей', value: modelsQ.data?.length ?? '—', icon: Database },
    { label: 'Валидаторов', value: validatorsQ.data?.length ?? '—', icon: ShieldCheck },
    { label: 'Экспериментов', value: experimentsQ.data?.length ?? '—', icon: FlaskConical },
  ];

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl font-bold tracking-tight">Дашборд</h1>
        <p className="text-muted-foreground">Краткая сводка о системе.</p>
      </header>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {kpis.map(({ label, value, icon: Icon }) => (
          <Card key={label}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                {label}
              </CardTitle>
              <Icon className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">{value}</div>
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center gap-2">
          <Activity className="h-4 w-4 text-muted-foreground" />
          <CardTitle className="text-lg">Последние действия (аудит)</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {auditQ.isLoading ? (
            <p className="p-6 text-sm text-muted-foreground">Загрузка…</p>
          ) : auditQ.data && auditQ.data.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Время</TableHead>
                  <TableHead>Действие</TableHead>
                  <TableHead>Цель</TableHead>
                  <TableHead>Пользователь</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {auditQ.data.slice(0, 10).map((entry) => (
                  <TableRow key={entry.id}>
                    <TableCell className="font-mono text-xs">
                      {new Date(entry.createdAt).toLocaleString('ru-RU')}
                    </TableCell>
                    <TableCell className="font-medium">{entry.action}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {entry.targetType ?? '—'}
                    </TableCell>
                    <TableCell className="font-mono text-xs text-muted-foreground">
                      {entry.userId?.slice(0, 8) ?? '—'}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <p className="p-6 text-sm text-muted-foreground">Записей нет.</p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
