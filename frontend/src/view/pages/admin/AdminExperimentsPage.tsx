// View layer — список всех экспериментов с возможностью force-delete (admin).

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ChevronLeft, ChevronRight, ExternalLink, Trash2 } from 'lucide-react';
import * as adminApi from '@/model/api/admin';
import { toApiError } from '@/model/api/client';
import type { Experiment } from '@/model/types';
import { Button } from '@/view/components/ui/button';
import { Card, CardContent } from '@/view/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/view/components/ui/dialog';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/view/components/ui/table';
import { useToast } from '@/view/components/ui/use-toast';
import { StatusBadge } from '@/view/components/StatusBadge';

const PAGE = 25;

export function AdminExperimentsPage() {
  const qc = useQueryClient();
  const { toast } = useToast();
  const [offset, setOffset] = useState(0);
  const [deleting, setDeleting] = useState<Experiment | null>(null);

  const q = useQuery({
    queryKey: ['admin', 'experiments', offset],
    queryFn: () => adminApi.listAllExperiments(PAGE, offset),
  });

  const deleteMut = useMutation({
    mutationFn: adminApi.adminDeleteExperiment,
    onSuccess: () => {
      toast({ title: 'Эксперимент удалён' });
      qc.invalidateQueries({ queryKey: ['admin', 'experiments'] });
      setDeleting(null);
    },
    onError: (e) =>
      toast({
        variant: 'destructive',
        title: 'Ошибка',
        description: toApiError(e).message,
      }),
  });

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl font-bold tracking-tight">Эксперименты</h1>
        <p className="text-muted-foreground">Все эксперименты системы.</p>
      </header>

      <Card>
        <CardContent className="p-0">
          {q.isLoading ? (
            <p className="p-6 text-sm text-muted-foreground">Загрузка…</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Название</TableHead>
                  <TableHead>Статус</TableHead>
                  <TableHead>Доступ</TableHead>
                  <TableHead>Пользователь</TableHead>
                  <TableHead>Создан</TableHead>
                  <TableHead className="w-[100px]">Действия</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {q.data?.map((exp) => (
                  <TableRow key={exp.id}>
                    <TableCell className="font-medium">{exp.name ?? 'Без названия'}</TableCell>
                    <TableCell>
                      <StatusBadge status={exp.status} />
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {exp.isPublic ? 'Публичный' : 'Личный'}
                    </TableCell>
                    <TableCell className="font-mono text-xs text-muted-foreground">
                      {exp.userId.slice(0, 8)}
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {new Date(exp.createdAt).toLocaleString('ru-RU')}
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-1">
                        <Button variant="ghost" size="icon" asChild aria-label="Открыть">
                          <Link to={`/experiments/${exp.id}`}>
                            <ExternalLink className="h-4 w-4" />
                          </Link>
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="Удалить"
                          onClick={() => setDeleting(exp)}
                        >
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <div className="flex items-center justify-end gap-2">
        <span className="text-sm text-muted-foreground">
          Страница {Math.floor(offset / PAGE) + 1}
        </span>
        <Button
          variant="outline"
          size="sm"
          disabled={offset === 0}
          onClick={() => setOffset((o) => Math.max(0, o - PAGE))}
        >
          <ChevronLeft className="mr-1 h-4 w-4" /> Назад
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={(q.data?.length ?? 0) < PAGE}
          onClick={() => setOffset((o) => o + PAGE)}
        >
          Вперёд <ChevronRight className="ml-1 h-4 w-4" />
        </Button>
      </div>

      <Dialog open={!!deleting} onOpenChange={(o) => !o && setDeleting(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Удалить эксперимент?</DialogTitle>
            <DialogDescription>
              Будет принудительно удалён эксперимент{' '}
              <strong>{deleting?.name ?? deleting?.id.slice(0, 8)}</strong>.
              Действие необратимо.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleting(null)}>
              Отмена
            </Button>
            <Button
              variant="destructive"
              disabled={deleteMut.isPending}
              onClick={() => deleting && deleteMut.mutate(deleting.id)}
            >
              {deleteMut.isPending ? 'Удаление…' : 'Удалить'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
