// View layer — витрина моделей с маркетинговым описанием.
// В отличие от ModelCard в каталоге, здесь — продающие подписи, эмодзи-категории
// и «крючки» вместо сухого описания.

import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import type { ModelTemplate } from '@/model/types';
import { Badge } from '@/view/components/ui/badge';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/view/components/ui/card';
import { Button } from '@/view/components/ui/button';

/** Маркетинговые описания, привязанные к коду модели. Если код незнакомый —
 *  карточка падает на model.description (нейтральное описание из БД). */
const COPY: Record<string, { category: string; hook: string; teaser: string }> = {
  SIR: {
    category: '🦠 Эпидемиология',
    hook: 'Пандемия в трёх уравнениях',
    teaser:
      'Восприимчивые → инфицированные → выздоровевшие. Простая, элегантная, ' +
      'предсказывает пик заражений и базовое R₀.',
  },
  SEIRD: {
    category: '🦠 Эпидемиология',
    hook: 'Реалистичная эпидемия с латентным периодом',
    teaser:
      'SIR + экспонированные и умершие. Параметризуется под COVID, ' +
      'корь, грипп — пять компартментов вместо трёх дают совсем другой профиль кривой.',
  },
  LOTKA_VOLTERRA: {
    category: '🦊 Экология популяций',
    hook: 'Классика хищник–жертва',
    teaser:
      'Зайцы и рыси, паразит и хозяин. Замкнутые орбиты в фазовом пространстве — ' +
      'модель из учебника, которой 100 лет, а всё ещё работает.',
  },
  ROSENZWEIG_MACARTHUR: {
    category: '🦊 Экология популяций',
    hook: 'Хищник–жертва с насыщением',
    teaser:
      'Функциональная реакция II типа: хищник насыщается. Тут начинается ' +
      'самое интересное — парадокс обогащения и предельные циклы.',
  },
  LOGISTIC: {
    category: '🌱 Базовая динамика',
    hook: 'Самая простая нелинейная модель роста',
    teaser:
      'Популяция упирается в ёмкость среды. Идеально для первого знакомства ' +
      'с ODE: одно уравнение, два параметра, понятная S-кривая.',
  },
  FITZHUGH_NAGUMO: {
    category: '🧠 Нейрофизиология',
    hook: 'Нейрон в двух уравнениях',
    teaser:
      'Упрощение Hodgkin–Huxley до сути: возбудимость и осцилляции. ' +
      'Показывает пороги и релаксационные колебания без четырёх уравнений HH.',
  },
  HODGKIN_HUXLEY: {
    category: '🧠 Нейрофизиология',
    hook: 'Золотой стандарт нейронауки',
    teaser:
      'Полная модель потенциала действия: Na⁺ и K⁺ каналы с воротными переменными n/m/h. ' +
      'За эти уравнения дали Нобелевскую премию. Жёсткая система — solver знает.',
  },
};

interface Props {
  models: ModelTemplate[];
}

export function ModelsShowcase({ models }: Props) {
  return (
    <section id="catalog" className="border-b">
      <div className="container py-20">
        <div className="mx-auto mb-12 max-w-2xl space-y-3 text-center">
          <h2 className="text-3xl font-bold tracking-tight md:text-4xl">
            Семь моделей. Сотни сценариев.
          </h2>
          <p className="text-muted-foreground">
            Каждая — каноническая, проверенная в учебниках и статьях. Выберите ту,
            которая решает вашу задачу — или ту, которую хочется потрогать в первый раз.
          </p>
        </div>

        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {models.map((m) => {
            const copy = COPY[m.code];
            return (
              <Card
                key={m.id}
                className="group flex flex-col transition-colors hover:border-primary/50"
              >
                <CardHeader>
                  <div className="mb-2 flex items-center justify-between">
                    <Badge variant="secondary" className="text-xs font-normal">
                      {copy?.category ?? m.code}
                    </Badge>
                    <span className="font-mono text-xs text-muted-foreground">
                      {m.code}
                    </span>
                  </div>
                  <CardTitle className="text-xl">
                    {copy?.hook ?? m.name}
                  </CardTitle>
                  <CardDescription className="leading-relaxed">
                    {copy?.teaser ?? m.description}
                  </CardDescription>
                </CardHeader>
                <CardContent className="flex-1">
                  <div className="flex flex-wrap gap-1.5">
                    {m.validators.slice(0, 4).map((v) => (
                      <Badge key={v.id} variant="outline" className="text-xs font-mono">
                        {v.targetField}
                      </Badge>
                    ))}
                    {m.validators.length > 4 && (
                      <Badge variant="outline" className="text-xs">
                        +{m.validators.length - 4}
                      </Badge>
                    )}
                  </div>
                </CardContent>
                <CardFooter>
                  <Button variant="ghost" className="ml-auto" asChild>
                    <Link to={`/models/${m.id}`}>
                      Изучить
                      <ArrowRight className="ml-2 h-4 w-4 transition-transform
                                              group-hover:translate-x-0.5" />
                    </Link>
                  </Button>
                </CardFooter>
              </Card>
            );
          })}
        </div>
      </div>
    </section>
  );
}
