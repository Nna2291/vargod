// View layer — страница списка экспериментов (маршрут "/experiments").

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus } from 'lucide-react';
import type { ExperimentScope } from '@/model/api/experiments';
import { isAdmin, isResearcher, useSessionStore } from '@/viewmodel/sessionStore';
import { useExperiments } from '@/viewmodel/useExperiments';
import { useModels } from '@/viewmodel/useModels';
import { Button } from '@/view/components/ui/button';
import { Card, CardContent } from '@/view/components/ui/card';
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@/view/components/ui/tabs';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/view/components/ui/table';
import { StatusBadge } from '@/view/components/StatusBadge';
import { EmptyMessage, ErrorMessage, Loader } from '@/view/components/StateView';

export function ExperimentsPage() {
  const researcher = useSessionStore(isResearcher);
  const admin = useSessionStore(isAdmin);
  const canCreate = researcher || admin;
  const [scope, setScope] = useState<ExperimentScope>('public');
  const { data, loading, error } = useExperiments(scope);
  const { data: models } = useModels();

  const modelName = (modelTemplateId: string) =>
    models.find((m) => m.id === modelTemplateId)?.name ?? modelTemplateId;

  return (
    <div className="container space-y-6 py-8">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-3xl font-bold tracking-tight">Вычислительные эксперименты</h1>
        {canCreate && (
          <Button asChild>
            <Link to="/experiments/new">
              <Plus className="mr-2 h-4 w-4" />
              Новый эксперимент
            </Link>
          </Button>
        )}
      </header>

      <Tabs value={scope} onValueChange={(v) => setScope(v as ExperimentScope)}>
        <TabsList>
          <TabsTrigger value="public">Публичные</TabsTrigger>
          {canCreate && <TabsTrigger value="mine">Мои</TabsTrigger>}
        </TabsList>

        <TabsContent value={scope} className="mt-6">
          {loading && <Loader />}
          {error && <ErrorMessage text={error} />}
          {!loading && !error && data.length === 0 && (
            <EmptyMessage
              text={
                scope === 'mine'
                  ? 'У вас пока нет экспериментов.'
                  : 'Публичных экспериментов пока нет.'
              }
            />
          )}
          {!loading && !error && data.length > 0 && (
            <Card>
              <CardContent className="p-0">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Название</TableHead>
                      <TableHead>Статус</TableHead>
                      <TableHead>Модель</TableHead>
                      <TableHead>Доступ</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {data.map((exp) => (
                      <TableRow key={exp.id}>
                        <TableCell>
                          <Link
                            to={`/experiments/${exp.id}`}
                            className="font-medium text-primary hover:underline"
                          >
                            {exp.name ?? 'Без названия'}
                          </Link>
                        </TableCell>
                        <TableCell>
                          <StatusBadge status={exp.status} />
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          {modelName(exp.modelTemplateId)}
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          {exp.isPublic ? 'Публичный' : 'Личный'}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
