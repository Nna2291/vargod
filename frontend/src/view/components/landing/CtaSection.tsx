// View layer — финальный CTA-блок и сноска о том, что это open-source.

import { Link } from 'react-router-dom';
import { ArrowRight, Github } from 'lucide-react';
import { Button } from '@/view/components/ui/button';

export function CtaSection() {
  return (
    <section className="container py-20">
      <div className="relative overflow-hidden rounded-2xl border bg-gradient-to-br
                      from-primary/10 via-background to-primary/5 p-10 text-center md:p-16">
        <div
          className="absolute -bottom-32 -right-32 -z-0 h-96 w-96 rounded-full
                     bg-primary/20 blur-3xl"
          aria-hidden="true"
        />
        <div className="relative z-10 mx-auto max-w-2xl space-y-6">
          <h2 className="text-3xl font-bold tracking-tight md:text-4xl">
            Начните прямо сейчас
          </h2>
          <p className="text-balance text-lg text-muted-foreground">
            Регистрация занимает 10 секунд: email и пароль — больше ничего не нужно.
            Никаких подтверждений, никаких рассылок. Только модели и графики.
          </p>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <Button size="lg" asChild>
              <Link to="/register">
                Создать аккаунт
                <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            </Button>
            <Button size="lg" variant="outline" asChild>
              <Link to="/login">Войти</Link>
            </Button>
          </div>
          <div className="pt-2 text-sm text-muted-foreground">
            <a
              href="https://github.com/Nna2291/vargod"
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 underline-offset-4 hover:underline"
            >
              <Github className="h-3.5 w-3.5" />
              Открытый исходный код — MIT
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}
