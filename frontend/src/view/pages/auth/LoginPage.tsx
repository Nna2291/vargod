// View layer — страница входа (маршрут "/login").

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

const schema = z.object({
  email: z.string().email('Введите корректный email'),
  password: z.string().min(8, 'Не короче 8 символов'),
});
type FormValues = z.infer<typeof schema>;

interface LocationState {
  from?: string;
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useSessionStore((s) => s.login);
  const loading = useSessionStore((s) => s.loading);
  const error = useSessionStore((s) => s.error);
  const token = useSessionStore((s) => s.token);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  });

  const redirectTo = (location.state as LocationState | null)?.from ?? '/';

  // Если уже залогинены — сразу уходим.
  useEffect(() => {
    if (token) navigate(redirectTo, { replace: true });
  }, [token, navigate, redirectTo]);

  const onSubmit = async (values: FormValues) => {
    const ok = await login(values.email, values.password);
    if (ok) navigate(redirectTo, { replace: true });
  };

  return (
    <div className="container flex min-h-[calc(100vh-4rem)] max-w-md items-center py-12">
      <Card className="w-full">
        <CardHeader>
          <CardTitle>Вход</CardTitle>
          <CardDescription>Войдите по email и паролю.</CardDescription>
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
                      <Input type="password" autoComplete="current-password" {...field} />
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
                {loading ? 'Вход…' : 'Войти'}
              </Button>
              <p className="text-sm text-muted-foreground">
                Нет аккаунта?{' '}
                <Link to="/register" className="text-primary underline-offset-4 hover:underline">
                  Зарегистрироваться
                </Link>
              </p>
            </CardFooter>
          </form>
        </Form>
      </Card>
    </div>
  );
}
