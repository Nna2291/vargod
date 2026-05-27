// View layer — страница деталей сравнения (маршрут "/comparisons/:id").

import { Link, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { useComparison } from '@/viewmodel/useComparisons';
import { ResultChart, type ResultSeries } from '@/view/components/ResultChart';
import { StatusBadge } from '@/view/components/StatusBadge';
import { ErrorMessage, Loader } from '@/view/components/StateView';
import { Button } from '@/view/components/ui/button';
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

export function ComparisonPage() {
  const { id } = useParams<{ id: string }>();
  const { data: comparison, loading, error } = useComparison(id);

  if (loading) return <div className="container py-8"><Loader /></div>;
  if (error) return <div className="container py-8"><ErrorMessage text={error} /></div>;
  if (!comparison) return null;

  const series: ResultSeries[] = comparison.experiments
    .filter((e) => e.result !== null)
    .map((e) => ({
      label: e.name ?? `Эксперимент ${e.id.slice(0, 8)}`,
      result: e.result!,
    }));

  return (
    <div className="container space-y-6 py-8">
      <Button variant="ghost" size="sm" asChild className="-ml-2">
        <Link to="/comparisons">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Сравнения
        </Link>
      </Button>

      <h1 className="text-3xl font-bold tracking-tight">
        {comparison.name ?? `Сравнение ${comparison.id.slice(0, 8)}`}
      </h1>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Эксперименты сравнения</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Название</TableHead>
                <TableHead>Статус</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {comparison.experiments.map((e) => (
                <TableRow key={e.id}>
                  <TableCell>
                    <Link
                      to={`/experiments/${e.id}`}
                      className="font-medium text-primary hover:underline"
                    >
                      {e.name ?? 'Без названия'}
                    </Link>
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={e.status} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Совмещённый график результатов</CardTitle>
        </CardHeader>
        <CardContent>
          {series.length > 0 ? (
            <ResultChart series={series} />
          ) : (
            <p className="text-sm text-muted-foreground">
              Ни один из экспериментов сравнения не имеет результата симуляции.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
