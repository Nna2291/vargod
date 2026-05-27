// View layer — страница регистрации (маршрут "/register").

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useSessionStore } from '@/viewmodel/sessionStore';
import { Button } from '@/view/components/ui/button';
import { Input } from '@/view/components/ui/input';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/view/components/ui/card';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/view/components/ui/form';

const schema = z
  .object({
    email: z.string().email('Введите корректный email'),
    password: z.string().min(8, 'Не короче 8 символов'),
    confirm: z.string(),
  })
  .refine((v) => v.password === v.confirm, {
    path: ['confirm'],
    message: 'Пароли не совпадают',
  });
type FormValues = z.infer<typeof schema>;

interface LocationState {
  from?: string;
}

export function RegisterPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const register = useSessionStore((s) => s.register);
  const loading = useSessionStore((s) => s.loading);
  const error = useSessionStore((s) => s.error);
  const token = useSessionStore((s) => s.token);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '', confirm: '' },
  });

  const redirectTo = (location.state as LocationState | null)?.from ?? '/';

  useEffect(() => {
    if (token) navigate(redirectTo, { replace: true });
  }, [token, navigate, redirectTo]);

  const onSubmit = async (values: FormValues) => {
    const ok = await register(values.email, values.password);
    if (ok) navigate(redirectTo, { replace: true });
  };

  return (
    <div className="container flex min-h-[calc(100vh-4rem)] max-w-md items-center py-12">
      <Card className="w-full">
        <CardHeader>
          <CardTitle>Регистрация</CardTitle>
          <CardDescription>Создайте аккаунт исследователя.</CardDescription>
        </CardHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)}>
            <CardContent className="space-y-4">
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input type="email" autoComplete="email" placeholder="you@example.com" {...field} />
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
                      <Input type="password" autoComplete="new-password" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="confirm"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Повторите пароль</FormLabel>
                    <FormControl>
                      <Input type="password" autoComplete="new-password" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              {error && (
                <p className="text-sm font-medium text-destructive" role="alert">
                  {error}
                </p>
              )}
            </CardContent>
            <CardFooter className="flex flex-col gap-3">
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? 'Создание…' : 'Зарегистрироваться'}
              </Button>
              <p className="text-sm text-muted-foreground">
                Уже есть аккаунт?{' '}
                <Link to="/login" className="text-primary underline-offset-4 hover:underline">
                  Войти
                </Link>
              </p>
            </CardFooter>
          </form>
        </Form>
      </Card>
    </div>
  );
}
