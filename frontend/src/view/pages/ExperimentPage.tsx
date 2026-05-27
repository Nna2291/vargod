// View layer — страница деталей эксперимента (маршрут "/experiments/:id").

import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, EyeOff, Play, Share2 } from 'lucide-react';
import { useSessionStore } from '@/viewmodel/sessionStore';
import { useExperiment } from '@/viewmodel/useExperiments';
import { useModel } from '@/viewmodel/useModels';
import { StatusBadge } from '@/view/components/StatusBadge';
import { ResultChart } from '@/view/components/ResultChart';
import { ErrorMessage, Loader } from '@/view/components/StateView';
import { Badge } from '@/view/components/ui/badge';
import { Button } from '@/view/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
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

export function ExperimentPage() {
  const { id } = useParams<{ id: string }>();
  const userId = useSessionStore((s) => s.user?.id ?? null);
  const { data: exp, loading, error, busy, run, publish, unpublish } = useExperiment(id);
  const { data: model } = useModel(exp?.modelTemplateId);

  if (loading) return <div className="container py-8"><Loader /></div>;
  if (error) return <div className="container py-8"><ErrorMessage text={error} /></div>;
  if (!exp) return null;

  const isOwner = userId !== null && userId === exp.userId;
  const ps = exp.parameterSet;

  return (
    <div className="container space-y-6 py-8">
      <Button variant="ghost" size="sm" asChild className="-ml-2">
        <Link to="/experiments">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Эксперименты
        </Link>
      </Button>

      <header className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-3xl font-bold tracking-tight">
            {exp.name ?? 'Эксперимент без названия'}
          </h1>
          <StatusBadge status={exp.status} />
        </div>
        <div className="flex flex-wrap gap-2">
          <Badge variant="outline">Модель: {model?.name ?? exp.modelTemplateId}</Badge>
          <Badge variant="outline">
            Доступ: {exp.isPublic ? 'публичный' : 'личный'}
          </Badge>
        </div>
      </header>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Параметры запуска</CardTitle>
        </CardHeader>
        <CardContent>
          {ps ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Параметр</TableHead>
                  <TableHead>Значение</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {Object.entries(ps.values).map(([k, v]) => (
                  <TableRow key={k}>
                    <TableCell className="font-mono">{k}</TableCell>
                    <TableCell>{v}</TableCell>
                  </TableRow>
                ))}
                <TableRow>
                  <TableCell>Интервал интегрирования</TableCell>
                  <TableCell>
                    [{ps.tStart}; {ps.tEnd}], шаг {ps.step}
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          ) : (
            <p className="text-sm text-muted-foreground">Набор параметров не задан.</p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Действия</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-3">
            {exp.status === 'PENDING' && (
              <Button onClick={() => void run()} disabled={busy}>
                <Play className="mr-2 h-4 w-4" />
                {busy ? 'Запуск…' : 'Запустить симуляцию'}
              </Button>
            )}
            {isOwner && !exp.isPublic && (
              <Button variant="outline" onClick={() => void publish()} disabled={busy}>
                <Share2 className="mr-2 h-4 w-4" />
                {busy ? 'Публикация…' : 'Опубликовать'}
              </Button>
            )}
            {isOwner && exp.isPublic && (
              <Button variant="outline" onClick={() => void unpublish()} disabled={busy}>
                <EyeOff className="mr-2 h-4 w-4" />
                {busy ? 'Скрытие…' : 'Снять публичность'}
              </Button>
            )}
            {exp.status !== 'PENDING' && !isOwner && (
              <span className="text-sm text-muted-foreground">Доступных действий нет.</span>
            )}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Результат симуляции</CardTitle>
          <CardDescription>
            {exp.status === 'COMPLETED'
              ? 'Численное решение по интервалу интегрирования.'
              : 'Результат появится после запуска.'}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {exp.status === 'COMPLETED' && exp.result ? (
            <ResultChart series={[{ label: exp.name ?? 'Результат', result: exp.result }]} />
          ) : (
            <p className="text-sm text-muted-foreground">
              {exp.status === 'COMPLETED'
                ? 'Результат отсутствует.'
                : 'Результат появится после запуска симуляции.'}
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
