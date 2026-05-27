// View layer — три колонки с ключевыми выгодами продукта.

import { Activity, Gauge, ShieldCheck, Share2, Sparkles, Workflow } from 'lucide-react';
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/view/components/ui/card';

const FEATURES = [
  {
    icon: Activity,
    title: '7 моделей биологии из коробки',
    body: 'Эпидемиология (SIR, SEIRD), популяционная экология (Lotka–Volterra, ' +
      'Rosenzweig–MacArthur, логистический рост) и нейрофизиология ' +
      '(FitzHugh–Nagumo, Hodgkin–Huxley). Каталог расширяется через админ-панель.',
  },
  {
    icon: Gauge,
    title: 'Интегрирование за миллисекунды',
    body: 'Под капотом — Apache Commons Math с адаптивным шагом Dormand–Prince 5(4). ' +
      'Жёсткие системы (Hodgkin–Huxley) получают рекомендованные ' +
      'tolerance-настройки автоматически.',
  },
  {
    icon: ShieldCheck,
    title: 'Параметры под надзором',
    body: 'Каждая модель приходит с набором валидаторов. Платформа не даст ' +
      'запустить SIR с отрицательной β или популяцией больше N — ошибка ' +
      'найдётся до того, как солвер потратит на неё CPU.',
  },
  {
    icon: Workflow,
    title: 'Сравнение опытов',
    body: 'Постройте несколько прогонов одной модели с разными параметрами и ' +
      'наложите их на один график. Удобно для калибровки и поиска критических значений.',
  },
  {
    icon: Share2,
    title: 'Делитесь, когда готовы',
    body: 'Эксперименты приватны по умолчанию. Опубликовали — попали в ленту ' +
      'для гостей. Передумали — снимите публичность в один клик.',
  },
  {
    icon: Sparkles,
    title: 'Конструктор валидаторов',
    body: 'Админ через UI собирает новые правила (RANGE, ' +
      'SUM_EQUALS_FIELD, ORDERED) без перекомпиляции бэкенда. Формы строятся ' +
      'динамически по схеме с сервера.',
  },
] as const;

export function FeaturesSection() {
  return (
    <section className="border-b bg-muted/30">
      <div className="container py-20">
        <div className="mx-auto mb-12 max-w-2xl space-y-3 text-center">
          <h2 className="text-3xl font-bold tracking-tight md:text-4xl">
            Всё, что нужно для вычислительного эксперимента
          </h2>
          <p className="text-muted-foreground">
            Без локального окружения, без Python, без зависимости от чьих-то Jupyter-блокнотов.
          </p>
        </div>

        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((f) => (
            <Card key={f.title} className="border-muted-foreground/10">
              <CardHeader>
                <div className="mb-3 inline-flex h-10 w-10 items-center justify-center
                                rounded-lg bg-primary/10 text-primary">
                  <f.icon className="h-5 w-5" />
                </div>
                <CardTitle className="text-lg">{f.title}</CardTitle>
                <CardDescription className="leading-relaxed">{f.body}</CardDescription>
              </CardHeader>
            </Card>
          ))}
        </div>
      </div>
    </section>
  );
}
