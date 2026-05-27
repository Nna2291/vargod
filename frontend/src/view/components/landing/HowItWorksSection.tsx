// View layer — «как это работает» в три шага.

const STEPS = [
  {
    n: '01',
    title: 'Выберите модель',
    body: 'Откройте каталог: семь канонических моделей с описанием, ' +
      'параметрами и валидаторами. Под каждой — публичные эксперименты сообщества.',
  },
  {
    n: '02',
    title: 'Задайте параметры',
    body: 'Форма подскажет валидные диапазоны и предложит дефолты. Введите ' +
      'начальные условия, интервал интегрирования и шаг — всё это валидируется ' +
      'до запуска солвера.',
  },
  {
    n: '03',
    title: 'Запустите и получите график',
    body: 'Симуляция считается синхронно за миллисекунды. Графики — интерактивные, ' +
      'переменные модели — на одной оси времени. Сравнивайте, публикуйте, делитесь.',
  },
] as const;

export function HowItWorksSection() {
  return (
    <section className="border-b">
      <div className="container py-20">
        <div className="mx-auto mb-12 max-w-2xl space-y-3 text-center">
          <h2 className="text-3xl font-bold tracking-tight md:text-4xl">
            Три шага от модели до графика
          </h2>
          <p className="text-muted-foreground">
            От открытия страницы до готового результата — обычно меньше минуты.
          </p>
        </div>

        <div className="grid gap-8 md:grid-cols-3">
          {STEPS.map((s, i) => (
            <div key={s.n} className="relative">
              <div className="mb-4 flex items-center gap-4">
                <span className="text-5xl font-bold tracking-tight text-primary/30">
                  {s.n}
                </span>
                {i < STEPS.length - 1 && (
                  <div className="hidden h-px flex-1 bg-gradient-to-r
                                  from-border to-transparent md:block" />
                )}
              </div>
              <h3 className="mb-2 text-xl font-semibold">{s.title}</h3>
              <p className="text-muted-foreground">{s.body}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
