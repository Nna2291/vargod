// View layer — «для кого». Три аудитории — три тона голоса.

import { GraduationCap, Microscope, Users } from 'lucide-react';
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/view/components/ui/card';

const AUDIENCES = [
  {
    icon: Microscope,
    title: 'Для исследователей',
    body:
      'Сохраняйте эксперименты, делайте их публичными по готовности, ' +
      'сравнивайте бок-о-бок прогоны с разными параметрами. Snapshot ' +
      'валидаторов на момент создания — даже если админ потом поменяет правила, ' +
      'ваш опубликованный результат не «переедет».',
  },
  {
    icon: GraduationCap,
    title: 'Для студентов и преподавателей',
    body:
      'Уравнения из учебника — оживают в браузере. Покажите за лекцию, ' +
      'что происходит с пиком эпидемии при изменении R₀, как формируются ' +
      'колебания у Лотки–Вольтерры или почему Hodgkin–Huxley даёт потенциал ' +
      'действия именно такой формы.',
  },
  {
    icon: Users,
    title: 'Для всех остальных',
    body:
      'Любопытно — потыкайте. Зарегистрироваться не нужно: гость видит весь ' +
      'каталог моделей и все опубликованные эксперименты. Хотите попробовать ' +
      'сами — регистрация за 10 секунд, без подтверждений и капч.',
  },
] as const;

export function AudienceSection() {
  return (
    <section className="border-b bg-muted/30">
      <div className="container py-20">
        <div className="mx-auto mb-12 max-w-2xl space-y-3 text-center">
          <h2 className="text-3xl font-bold tracking-tight md:text-4xl">
            Для кого этот инструмент
          </h2>
          <p className="text-muted-foreground">
            Vargod писался как академическая платформа. Поэтому он одинаково
            годится и для серьёзной работы, и для «а как оно вообще?».
          </p>
        </div>

        <div className="grid gap-6 md:grid-cols-3">
          {AUDIENCES.map((a) => (
            <Card key={a.title} className="border-muted-foreground/10">
              <CardHeader>
                <div className="mb-3 inline-flex h-10 w-10 items-center justify-center
                                rounded-lg bg-primary/10 text-primary">
                  <a.icon className="h-5 w-5" />
                </div>
                <CardTitle className="text-lg">{a.title}</CardTitle>
                <CardDescription className="leading-relaxed">{a.body}</CardDescription>
              </CardHeader>
            </Card>
          ))}
        </div>
      </div>
    </section>
  );
}
