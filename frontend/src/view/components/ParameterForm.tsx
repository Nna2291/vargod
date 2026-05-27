// View layer — форма ввода параметров эксперимента (shadcn-style).

import { useMemo, useState } from 'react';
import type { ModelTemplate } from '@/model/types';
import { extractParameters } from '@/viewmodel/modelParameters';
import { Button } from '@/view/components/ui/button';
import { Input } from '@/view/components/ui/input';
import { Label } from '@/view/components/ui/label';

/** Собранные формой данные для запроса создания эксперимента. */
export interface ParameterFormValues {
  name: string;
  parameters: Record<string, number>;
  tStart: number;
  tEnd: number;
  step: number;
}

interface Props {
  model: ModelTemplate;
  submitting: boolean;
  onSubmit: (values: ParameterFormValues) => void;
}

export function ParameterForm({ model, submitting, onSubmit }: Props) {
  const params = useMemo(() => extractParameters(model), [model]);

  const [name, setName] = useState('');
  // Значения полей хранятся строками — числа разбираются при отправке.
  const [paramValues, setParamValues] = useState<Record<string, string>>({});
  const [tStart, setTStart] = useState('0');
  const [tEnd, setTEnd] = useState('10');
  const [step, setStep] = useState('0.1');
  const [localError, setLocalError] = useState<string | null>(null);

  const setParam = (key: string, value: string) =>
    setParamValues((prev) => ({ ...prev, [key]: value }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);

    // Минимальная клиентская проверка: все числовые поля обязаны быть числами.
    // Содержательную проверку диапазонов выполняет backend (ответ 422).
    const parameters: Record<string, number> = {};
    for (const p of params) {
      const num = Number(paramValues[p.name]);
      if (paramValues[p.name] === undefined || paramValues[p.name] === '' || Number.isNaN(num)) {
        setLocalError(`Параметр «${p.name}» должен быть числом.`);
        return;
      }
      parameters[p.name] = num;
    }
    const ts = Number(tStart);
    const te = Number(tEnd);
    const st = Number(step);
    if ([ts, te, st].some(Number.isNaN)) {
      setLocalError('Поля интервала интегрирования должны быть числами.');
      return;
    }

    onSubmit({ name: name.trim(), parameters, tStart: ts, tEnd: te, step: st });
  };

  return (
    <form className="space-y-6" onSubmit={handleSubmit}>
      <div className="space-y-2">
        <Label htmlFor="exp-name">Название эксперимента</Label>
        <Input
          id="exp-name"
          value={name}
          placeholder="Например, базовый сценарий"
          onChange={(e) => setName(e.target.value)}
        />
      </div>

      <section className="space-y-3">
        <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
          Параметры модели
        </h3>
        {params.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            У модели нет числовых параметров.
          </p>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {params.map((p) => (
              <div className="space-y-2" key={p.name}>
                <Label htmlFor={`param-${p.name}`}>{p.name}</Label>
                <Input
                  id={`param-${p.name}`}
                  type="number"
                  step="any"
                  min={p.min}
                  max={p.max}
                  value={paramValues[p.name] ?? ''}
                  onChange={(e) => setParam(p.name, e.target.value)}
                />
                {(p.min !== undefined || p.max !== undefined) && (
                  <p className="text-xs text-muted-foreground">
                    Диапазон: {p.min ?? '−∞'} … {p.max ?? '+∞'}
                  </p>
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="space-y-3">
        <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
          Интервал интегрирования
        </h3>
        <div className="grid gap-4 sm:grid-cols-3">
          <div className="space-y-2">
            <Label htmlFor="t-start">Начало (tStart)</Label>
            <Input
              id="t-start"
              type="number"
              step="any"
              value={tStart}
              onChange={(e) => setTStart(e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="t-end">Конец (tEnd)</Label>
            <Input
              id="t-end"
              type="number"
              step="any"
              value={tEnd}
              onChange={(e) => setTEnd(e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="t-step">Шаг (step)</Label>
            <Input
              id="t-step"
              type="number"
              step="any"
              value={step}
              onChange={(e) => setStep(e.target.value)}
            />
          </div>
        </div>
      </section>

      {localError && (
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
          {localError}
        </div>
      )}

      <div className="flex justify-end">
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Создание…' : 'Создать эксперимент'}
        </Button>
      </div>
    </form>
  );
}
