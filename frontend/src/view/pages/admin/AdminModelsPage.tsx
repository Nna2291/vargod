// View layer — управление каталогом моделей (admin).

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Pencil, Plus, ShieldCheck, Trash2 } from 'lucide-react';
import * as adminApi from '@/model/api/admin';
import * as modelsApi from '@/model/api/models';
import { toApiError } from '@/model/api/client';
import type { ModelTemplate } from '@/model/types';
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
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/view/components/ui/form';
import { Input } from '@/view/components/ui/input';
import { Textarea } from '@/view/components/ui/textarea';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/view/components/ui/table';
import { useToast } from '@/view/components/ui/use-toast';

const schema = z.object({
  code: z.string().min(1, 'Обязательно'),
  name: z.string().min(1, 'Обязательно'),
  description: z.string(),
  solverType: z.string().min(1, 'Обязательно'),
});
type Values = z.infer<typeof schema>;

export function AdminModelsPage() {
  const qc = useQueryClient();
  const { toast } = useToast();
  const modelsQ = useQuery({ queryKey: ['models'], queryFn: modelsApi.fetchModels });

  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<ModelTemplate | null>(null);
  const [deleting, setDeleting] = useState<ModelTemplate | null>(null);

  const createForm = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { code: '', name: '', description: '', solverType: 'RK4' },
  });

  const editForm = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { code: '', name: '', description: '', solverType: 'RK4' },
  });

  const openEdit = (m: ModelTemplate) => {
    editForm.reset({
      code: m.code,
      name: m.name,
      description: m.description ?? '',
      solverType: m.solverType,
    });
    setEditing(m);
  };

  const createMut = useMutation({
    mutationFn: (values: Values) =>
      adminApi.createModel({
        code: values.code,
        name: values.name,
        description: values.description || null,
        solverType: values.solverType,
      }),
    onSuccess: () => {
      toast({ title: 'Модель создана' });
      qc.invalidateQueries({ queryKey: ['models'] });
      setCreateOpen(false);
      createForm.reset();
    },
    onError: (e) =>
      toast({
        variant: 'destructive',
        title: 'Ошибка',
        description: toApiError(e).message,
      }),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, body }: { id: string; body: Values }) =>
      adminApi.updateModel(id, {
        code: body.code,
        name: body.name,
        description: body.description || null,
        solverType: body.solverType,
      }),
    onSuccess: () => {
      toast({ title: 'Модель обновлена' });
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
    mutationFn: adminApi.deleteModel,
    onSuccess: () => {
      toast({ title: 'Модель удалена' });
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

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Модели</h1>
          <p className="text-muted-foreground">Каталог шаблонов ОДУ-моделей.</p>
        </div>
        <Dialog open={createOpen} onOpenChange={setCreateOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              Добавить модель
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Новая модель</DialogTitle>
              <DialogDescription>Создание нового шаблона ОДУ-модели.</DialogDescription>
            </DialogHeader>
            <ModelForm
              form={createForm}
              submitLabel="Создать"
              submitting={createMut.isPending}
              onSubmit={(v) => createMut.mutate(v)}
            />
          </DialogContent>
        </Dialog>
      </header>

      <Card>
        <CardContent className="p-0">
          {modelsQ.isLoading ? (
            <p className="p-6 text-sm text-muted-foreground">Загрузка…</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Код</TableHead>
                  <TableHead>Название</TableHead>
                  <TableHead>Решатель</TableHead>
                  <TableHead>Валидаторов</TableHead>
                  <TableHead className="w-[180px]">Действия</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {modelsQ.data?.map((m) => (
                  <TableRow key={m.id}>
                    <TableCell className="font-mono text-xs">{m.code}</TableCell>
                    <TableCell className="font-medium">{m.name}</TableCell>
                    <TableCell className="text-muted-foreground">{m.solverType}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {m.validators.length}
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-1">
                        <Button variant="ghost" size="icon" asChild aria-label="Валидаторы">
                          <Link to={`/admin/validators?modelTemplateId=${m.id}`}>
                            <ShieldCheck className="h-4 w-4" />
                          </Link>
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="Редактировать"
                          onClick={() => openEdit(m)}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="Удалить"
                          onClick={() => setDeleting(m)}
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

      <Dialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Редактировать модель</DialogTitle>
          </DialogHeader>
          {editing && (
            <ModelForm
              form={editForm}
              submitLabel="Сохранить"
              submitting={updateMut.isPending}
              onSubmit={(v) => updateMut.mutate({ id: editing.id, body: v })}
            />
          )}
        </DialogContent>
      </Dialog>

      <Dialog open={!!deleting} onOpenChange={(o) => !o && setDeleting(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Удалить модель?</DialogTitle>
            <DialogDescription>
              Будет удалена модель <strong>{deleting?.name}</strong>. Действие необратимо.
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

function ModelForm({
  form,
  onSubmit,
  submitting,
  submitLabel,
}: {
  form: ReturnType<typeof useForm<Values>>;
  onSubmit: (v: Values) => void;
  submitting: boolean;
  submitLabel: string;
}) {
  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField
          control={form.control}
          name="code"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Код (уникальный)</FormLabel>
              <FormControl>
                <Input placeholder="SEIRD" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="name"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Название</FormLabel>
              <FormControl>
                <Input placeholder="Модель SEIRD" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="solverType"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Тип решателя</FormLabel>
              <FormControl>
                <Input placeholder="RK4" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="description"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Описание</FormLabel>
              <FormControl>
                <Textarea rows={3} {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <DialogFooter>
          <Button type="submit" disabled={submitting}>
            {submitting ? 'Сохранение…' : submitLabel}
          </Button>
        </DialogFooter>
      </form>
    </Form>
  );
}
