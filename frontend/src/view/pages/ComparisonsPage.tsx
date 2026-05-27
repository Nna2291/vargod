// View layer — страница сравнений (маршрут "/comparisons").

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { isAdmin, isResearcher, useSessionStore } from '@/viewmodel/sessionStore';
import { useComparisons } from '@/viewmodel/useComparisons';
import { useExperiments } from '@/viewmodel/useExperiments';
import { ValidationAlert } from '@/view/components/ValidationAlert';
import { EmptyMessage, ErrorMessage, Loader } from '@/view/components/StateView';
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

export function ComparisonsPage() {
  const researcher = useSessionStore(isResearcher);
  const admin = useSessionStore(isAdmin);
  const canCompare = researcher || admin;
  const { data: comparisons, loading, error, submitting, apiError, create } =
    useComparisons();
  const { data: myExperiments } = useExperiments('mine');
  const completed = myExperiments.filter((e) => e.status === 'COMPLETED');

  const [selected, setSelected] = useState<string[]>([]);
  const [localError, setLocalError] = useState<string | null>(null);

  if (!canCompare) {
    return (
      <div className="container max-w-xl py-8">
        <ErrorMessage text="Сравнения доступны только зарегистрированным пользователям." />
      </div>
    );
  }

  const toggle = (id: string) =>
    setSelected((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));

  const handleCreate = async () => {
    setLocalError(null);
    if (selected.length < 2) {
      setLocalError('Выберите минимум два эксперимента для сравнения.');
      return;
    }
    const created = await create(selected);
    if (created) setSelected([]);
  };

  return (
    <div className="container space-y-6 py-8">
      <h1 className="text-3xl font-bold tracking-tight">Сравнения экспериментов</h1>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Новое сравнение</CardTitle>
          <CardDescription>
            Выберите два или более завершённых эксперимента — их результаты будут
            наложены на один график.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {completed.length === 0 ? (
            <EmptyMessage text="Нет завершённых экспериментов для сравнения." />
          ) : (
            <>
              <div className="max-h-60 space-y-1 overflow-auto rounded-md border p-3">
                {completed.map((exp) => (
                  <label
                    key={exp.id}
                    className="flex cursor-pointer items-center gap-2 rounded-sm px-2 py-1 text-sm hover:bg-accent"
                  >
                    <input
                      type="checkbox"
                      checked={selected.includes(exp.id)}
                      onChange={() => toggle(exp.id)}
                      className="h-4 w-4 rounded border-input"
                    />
                    {exp.name ?? 'Без названия'}
                  </label>
                ))}
              </div>
              {localError && (
                <p className="text-sm font-medium text-destructive">{localError}</p>
              )}
              {apiError && <ValidationAlert error={apiError} />}
              <Button
                onClick={() => void handleCreate()}
                disabled={submitting}
              >
                {submitting
                  ? 'Создание…'
                  : `Создать сравнение (${selected.length})`}
              </Button>
            </>
          )}
        </CardContent>
      </Card>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold">Мои сравнения</h2>
        {loading && <Loader />}
        {error && <ErrorMessage text={error} />}
        {!loading && !error && comparisons.length === 0 && (
          <EmptyMessage text="Сравнений пока нет." />
        )}
        {!loading && !error && comparisons.length > 0 && (
          <Card>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Название</TableHead>
                    <TableHead>Экспериментов</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {comparisons.map((c) => (
                    <TableRow key={c.id}>
                      <TableCell>
                        <Link
                          to={`/comparisons/${c.id}`}
                          className="font-medium text-primary hover:underline"
                        >
                          {c.name ?? `Сравнение ${c.id.slice(0, 8)}`}
                        </Link>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {c.experiments.length}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        )}
      </section>
    </div>
  );
}
