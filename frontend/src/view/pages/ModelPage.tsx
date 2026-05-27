// View layer — страница деталей модели (маршрут "/models/:id").

import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, FlaskConical } from 'lucide-react';
import { useModel } from '@/viewmodel/useModels';
import { constraintValidators, extractParameters } from '@/viewmodel/modelParameters';
import { isResearcher, isAdmin, useSessionStore } from '@/viewmodel/sessionStore';
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
import { ErrorMessage, Loader } from '@/view/components/StateView';

export function ModelPage() {
  const { id } = useParams<{ id: string }>();
  const { data: model, loading, error } = useModel(id);
  const researcher = useSessionStore(isResearcher);
  const admin = useSessionStore(isAdmin);
  const canRun = researcher || admin;

  if (loading) return <div className="container py-8"><Loader /></div>;
  if (error) return <div className="container py-8"><ErrorMessage text={error} /></div>;
  if (!model) return null;

  const params = extractParameters(model);
  const constraints = constraintValidators(model);

  return (
    <div className="container space-y-6 py-8">
      <Button variant="ghost" size="sm" asChild className="-ml-2">
        <Link to="/">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Каталог моделей
        </Link>
      </Button>

      <header className="space-y-3">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="outline" className="font-mono">
            {model.code}
          </Badge>
          <Badge variant="secondary">Решатель: {model.solverType}</Badge>
        </div>
        <h1 className="text-3xl font-bold tracking-tight">{model.name}</h1>
        <p className="text-muted-foreground">
          {model.description ?? 'Описание отсутствует.'}
        </p>
      </header>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Параметры модели</CardTitle>
          <CardDescription>Поля ввода для запуска симуляции.</CardDescription>
        </CardHeader>
        <CardContent>
          {params.length === 0 ? (
            <p className="text-sm text-muted-foreground">У модели нет числовых параметров.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Параметр</TableHead>
                  <TableHead>Минимум</TableHead>
                  <TableHead>Максимум</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {params.map((p) => (
                  <TableRow key={p.name}>
                    <TableCell className="font-mono">{p.name}</TableCell>
                    <TableCell>{p.min ?? '−∞'}</TableCell>
                    <TableCell>{p.max ?? '+∞'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Правила-ограничения</CardTitle>
          <CardDescription>Дополнительные межполевые валидаторы.</CardDescription>
        </CardHeader>
        <CardContent>
          {constraints.length === 0 ? (
            <p className="text-sm text-muted-foreground">Дополнительных ограничений нет.</p>
          ) : (
            <div className="space-y-2">
              {constraints.map((v) => (
                <div
                  key={v.id}
                  className="flex flex-wrap items-start gap-3 rounded-md border p-3 text-sm"
                >
                  <Badge variant="default">{v.name}</Badge>
                  <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs">
                    {v.targetField}
                  </code>
                  <code className="break-all font-mono text-xs text-muted-foreground">
                    {JSON.stringify(v.rules)}
                  </code>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {canRun && (
        <div>
          <Button asChild size="lg">
            <Link to={`/experiments/new?model=${model.id}`}>
              <FlaskConical className="mr-2 h-4 w-4" />
              Поставить эксперимент
            </Link>
          </Button>
        </div>
      )}
    </div>
  );
}
