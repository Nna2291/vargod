// View layer — управление пользователями (admin).

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { MoreHorizontal, Plus, Trash2, UserCog } from 'lucide-react';
import * as adminApi from '@/model/api/admin';
import type { AuthUser, Role } from '@/model/types';
import { toApiError } from '@/model/api/client';
import { Badge } from '@/view/components/ui/badge';
import { Button } from '@/view/components/ui/button';
import {
  Card,
  CardContent,
} from '@/view/components/ui/card';
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
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/view/components/ui/dropdown-menu';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/view/components/ui/form';
import { Input } from '@/view/components/ui/input';
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

const ROLES: Role[] = ['ADMIN', 'RESEARCHER'];

const createSchema = z.object({
  email: z.string().email('Введите корректный email'),
  password: z.string().min(8, 'Не короче 8 символов'),
  role: z.enum(['ADMIN', 'RESEARCHER']),
});
type CreateValues = z.infer<typeof createSchema>;

export function AdminUsersPage() {
  const qc = useQueryClient();
  const { toast } = useToast();
  const { data: users, isLoading, error } = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: adminApi.listUsers,
  });

  const [createOpen, setCreateOpen] = useState(false);
  const [deletingUser, setDeletingUser] = useState<AuthUser | null>(null);

  const form = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: { email: '', password: '', role: 'RESEARCHER' },
  });

  const createMut = useMutation({
    mutationFn: adminApi.createUser,
    onSuccess: () => {
      toast({ title: 'Пользователь создан' });
      qc.invalidateQueries({ queryKey: ['admin', 'users'] });
      setCreateOpen(false);
      form.reset();
    },
    onError: (e) =>
      toast({
        variant: 'destructive',
        title: 'Не удалось создать пользователя',
        description: toApiError(e).message,
      }),
  });

  const roleMut = useMutation({
    mutationFn: ({ id, role }: { id: string; role: Role }) =>
      adminApi.updateUserRole(id, role),
    onSuccess: () => {
      toast({ title: 'Роль обновлена' });
      qc.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: (e) =>
      toast({
        variant: 'destructive',
        title: 'Не удалось обновить роль',
        description: toApiError(e).message,
      }),
  });

  const deleteMut = useMutation({
    mutationFn: adminApi.deleteUser,
    onSuccess: () => {
      toast({ title: 'Пользователь удалён' });
      qc.invalidateQueries({ queryKey: ['admin', 'users'] });
      setDeletingUser(null);
    },
    onError: (e) =>
      toast({
        variant: 'destructive',
        title: 'Не удалось удалить пользователя',
        description: toApiError(e).message,
      }),
  });

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Пользователи</h1>
          <p className="text-muted-foreground">Управление аккаунтами системы.</p>
        </div>
        <Dialog open={createOpen} onOpenChange={setCreateOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              Добавить пользователя
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Новый пользователь</DialogTitle>
              <DialogDescription>Создайте аккаунт с указанной ролью.</DialogDescription>
            </DialogHeader>
            <Form {...form}>
              <form
                onSubmit={form.handleSubmit((values) => createMut.mutate(values))}
                className="space-y-4"
              >
                <FormField
                  control={form.control}
                  name="email"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Email</FormLabel>
                      <FormControl>
                        <Input type="email" placeholder="user@example.com" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="password"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Пароль</FormLabel>
                      <FormControl>
                        <Input type="password" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="role"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Роль</FormLabel>
                      <Select onValueChange={field.onChange} value={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {ROLES.map((r) => (
                            <SelectItem key={r} value={r}>
                              {r}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <DialogFooter>
                  <Button type="submit" disabled={createMut.isPending}>
                    {createMut.isPending ? 'Создание…' : 'Создать'}
                  </Button>
                </DialogFooter>
              </form>
            </Form>
          </DialogContent>
        </Dialog>
      </header>

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <p className="p-6 text-sm text-muted-foreground">Загрузка…</p>
          ) : error ? (
            <p className="p-6 text-sm text-destructive">
              Ошибка: {toApiError(error).message}
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Роль</TableHead>
                  <TableHead>Создан</TableHead>
                  <TableHead className="w-[80px]">Действия</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users?.map((u) => (
                  <TableRow key={u.id}>
                    <TableCell className="font-mono text-xs">{u.id.slice(0, 8)}</TableCell>
                    <TableCell className="font-medium">{u.email}</TableCell>
                    <TableCell>
                      <Badge variant={u.role === 'ADMIN' ? 'default' : 'secondary'}>
                        {u.role}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {u.createdAt
                        ? new Date(u.createdAt).toLocaleString('ru-RU')
                        : '—'}
                    </TableCell>
                    <TableCell>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon" aria-label="Действия">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuLabel>Изменить роль</DropdownMenuLabel>
                          {ROLES.filter((r) => r !== u.role).map((r) => (
                            <DropdownMenuItem
                              key={r}
                              onClick={() => roleMut.mutate({ id: u.id, role: r })}
                            >
                              <UserCog className="mr-2 h-4 w-4" />
                              Назначить {r}
                            </DropdownMenuItem>
                          ))}
                          <DropdownMenuSeparator />
                          <DropdownMenuItem
                            className="text-destructive focus:text-destructive"
                            onClick={() => setDeletingUser(u)}
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
                            Удалить
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog open={!!deletingUser} onOpenChange={(o) => !o && setDeletingUser(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Удалить пользователя?</DialogTitle>
            <DialogDescription>
              Будет удалён пользователь <strong>{deletingUser?.email}</strong>.
              Действие необратимо.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeletingUser(null)}>
              Отмена
            </Button>
            <Button
              variant="destructive"
              disabled={deleteMut.isPending}
              onClick={() => deletingUser && deleteMut.mutate(deletingUser.id)}
            >
              {deleteMut.isPending ? 'Удаление…' : 'Удалить'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
