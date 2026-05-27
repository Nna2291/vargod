// View layer — герой-блок лендинга.
// Огромный заголовок, sub-headline и две CTA: «изучить каталог» / «зарегистрироваться».
// Показывается гостю на главной странице.

import { Link } from 'react-router-dom';
import { ArrowRight, Sparkles } from 'lucide-react';
import { Button } from '@/view/components/ui/button';

export function HeroSection() {
  return (
    <section className="relative overflow-hidden border-b">
      {/* Декоративный градиент-фон: акцентный цвет с радиальной подсветкой. */}
      <div className="absolute inset-0 -z-10 bg-gradient-to-br from-primary/10 via-background to-background" />
      <div
        className="absolute -top-32 right-0 -z-10 h-[500px] w-[500px] rounded-full
                   bg-primary/20 blur-3xl"
        aria-hidden="true"
      />

      <div className="container py-20 md:py-28">
        <div className="mx-auto max-w-3xl space-y-6 text-center">
          <div className="inline-flex items-center gap-2 rounded-full border bg-background/80
                          px-4 py-1.5 text-xs font-medium backdrop-blur">
            <Sparkles className="h-3.5 w-3.5 text-primary" />
            Численная биология в браузере
          </div>

          <h1 className="text-balance text-4xl font-bold tracking-tight md:text-6xl">
            Поставьте эксперимент,
            <br />
            <span className="bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
              не написав ни строчки кода
            </span>
          </h1>

          <p className="text-balance text-lg text-muted-foreground md:text-xl">
            Семь канонических моделей биологии — от классической эпидемии SIR до
            полной модели потенциала действия Hodgkin–Huxley. Выберите модель,
            задайте параметры, нажмите кнопку. Получите интерактивный график за секунды.
          </p>

          <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
            <Button size="lg" asChild>
              <Link to="#catalog">
                Открыть каталог моделей
                <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            </Button>
            <Button size="lg" variant="outline" asChild>
              <Link to="/register">Зарегистрироваться</Link>
            </Button>
          </div>

          <p className="pt-4 text-sm text-muted-foreground">
            Гость видит каталог и публичные эксперименты без регистрации.
            Чтобы ставить свои опыты — нужен бесплатный аккаунт.
          </p>
        </div>
      </div>
    </section>
  );
}
