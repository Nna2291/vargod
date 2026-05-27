// View layer — управление валидаторами (admin). Фильтр по модели.

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { Pencil, Plus, Trash2 } from 'lucide-react';
import * as adminApi from '@/model/api/admin';
import * as modelsApi from '@/model/api/models';
import { toApiError } from '@/model/api/client';
import type { Validator } from '@/model/types';
import { Badge } from '@/view/components/ui/badge';
import { Button } from '@/view/components/ui/button';
import { Card, CardContent } from '@/view/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/view/components/ui/dialog';
import { Label } from '@/view/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/view/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/view/components/ui/table';
import { useToast } from '@/view/components/ui/use-toast';
import { ValidatorBuilderForm } from '@/view/components/admin/ValidatorBuilderForm';

export function AdminValidatorsPage() {
  const qc = useQueryClient();
  const { toast } = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const modelTemplateId = searchParams.get('modelTemplateId') ?? '';

  const modelsQ = useQuery({ queryKey: ['models'], queryFn: modelsApi.fetchModels });
  const validatorsQ = useQuery({
    queryKey: ['admin', 'validators', modelTemplateId],
    queryFn: () => adminApi.listValidators(modelTemplateId || undefined),
    enabled: !!modelTemplateId,
  });

  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Validator | null>(null);
  const [deleting, setDeleting] = useState<Validator | null>(null);

  const createMut = useMutation({
    mutationFn: adminApi.createValidator,
    onSuccess: () => {
      toast({ title: 'Валидатор создан' });
      qc.invalidateQueries({ queryKey: ['admin', 'validators'] });
      qc.invalidateQueries({ queryKey: ['models'] });
      setCreateOpen(false);
    },
    onError: (e) =>
      toast({
        variant: 'destructive',
        title: 'Ошибка',
        description: toApiError(e).message,
      }),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, body }: { id: string; body: adminApi.UpdateValidatorRequest }) =>
      adminApi.updateValidator(id, body),
    onSuccess: () => {
      toast({ title: 'Валидатор обновлён' });
      qc.invalidateQueries({ queryKey: ['admin', 'validators'] });
      qc.invalidateQueries({ queryKey: ['models'] });
      setEditing(null);
    },
    onError: (e) =>
      toast({
        variant: 'destructive',
        title: 'Ошибка',
        description: toApiError(e).message,
      }),
  });

  const deleteMut = useMutation({
    mutationFn: adminApi.deleteValidator,
    onSuccess: () => {
      toast({ title: 'Валидатор удалён' });
      qc.invalidateQueries({ queryKey: ['admin', 'validators'] });
      qc.invalidateQueries({ queryKey: ['models'] });
      setDeleting(null);
    },
    onError: (e) =>
      toast({
        variant: 'destructive',
        title: 'Ошибка',
        description: toApiError(e).message,
      }),
  });

  const setModel = (id: string) => {
    const next = new URLSearchParams(searchParams);
    if (id) next.set('modelTemplateId', id);
    else next.delete('modelTemplateId');
    setSearchParams(next, { replace: true });
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Валидаторы</h1>
          <p className="text-muted-foreground">
            Правила-ограничения для параметров модели. Сначала выберите модель.
          </p>
        </div>
        <Dialog open={createOpen} onOpenChange={setCreateOpen}>
          <DialogTrigger asChild>
            <Button disabled={!modelTemplateId}>
              <Plus className="mr-2 h-4 w-4" />
              Добавить валидатор
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>Новый валидатор</DialogTitle>
              <DialogDescription>
                Опишите проверку: тип цели, поле и параметры правила.
              </DialogDescription>
            </DialogHeader>
            <ValidatorBuilderForm
              modelTemplateId={modelTemplateId}
              submitting={createMut.isPending}
              submitLabel="Создать"
              onSubmit={async (payload) => {
                await createMut.mutateAsync({ ...payload, modelTemplateId });
              }}
            />
          </DialogContent>
        </Dialog>
      </header>

      <Card>
        <CardContent className="space-y-4 p-6">
          <div className="space-y-2">
            <Label htmlFor="model-filter">Модель</Label>
            <Select value={modelTemplateId} onValueChange={setModel}>
              <SelectTrigger id="model-filter" className="max-w-md">
                <SelectValue placeholder="— выберите модель —" />
              </SelectTrigger>
              <SelectContent>
                {modelsQ.data?.map((m) => (
                  <SelectItem key={m.id} value={m.id}>
                    {m.code} — {m.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {modelTemplateId && (
        <Card>
          <CardContent className="p-0">
            {validatorsQ.isLoading ? (
              <p className="p-6 text-sm text-muted-foreground">Загрузка…</p>
            ) : !validatorsQ.data || validatorsQ.data.length === 0 ? (
              <p className="p-6 text-sm text-muted-foreground">
                У модели нет валидаторов. Создайте первый.
              </p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Название</TableHead>
                    <TableHead>Тип проверки</TableHead>
                    <TableHead>Цель</TableHead>
                    <TableHead>Поле</TableHead>
                    <TableHead>Правила</TableHead>
                    <TableHead className="w-[120px]">Действия</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {validatorsQ.data.map((v) => (
                    <TableRow key={v.id}>
                      <TableCell className="font-medium">{v.name}</TableCell>
                      <TableCell>
                        <Badge variant="outline">{v.checkType}</Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{v.targetKind}</TableCell>
                      <TableCell className="font-mono text-xs">{v.targetField}</TableCell>
                      <TableCell className="font-mono text-xs text-muted-foreground">
                        {JSON.stringify(v.rules)}
                      </TableCell>
                      <TableCell>
                        <div className="flex gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label="Редактировать"
                            onClick={() => setEditing(v)}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label="Удалить"
                            onClick={() => setDeleting(v)}
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
      )}

      <Dialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Редактировать валидатор</DialogTitle>
          </DialogHeader>
          {editing && (
            <ValidatorBuilderForm
              key={editing.id}
              modelTemplateId={modelTemplateId}
              initial={editing}
              submitting={updateMut.isPending}
              submitLabel="Сохранить"
              onSubmit={async (payload) => {
                await updateMut.mutateAsync({ id: editing.id, body: payload });
              }}
            />
          )}
        </DialogContent>
      </Dialog>

      <Dialog open={!!deleting} onOpenChange={(o) => !o && setDeleting(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Удалить валидатор?</DialogTitle>
            <DialogDescription>
              Будет удалён валидатор <strong>{deleting?.name}</strong>. Действие необратимо.
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
