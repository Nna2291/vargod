// View layer — график результата симуляции (recharts), обёрнутый в Card.

import { useMemo } from 'react';
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { SimulationResult } from '@/model/types';
import { Card } from '@/view/components/ui/card';

/** Один ряд результата для наложения: подпись + сам результат. */
export interface ResultSeries {
  label: string;
  result: SimulationResult;
}

// Палитра линий — на основе CSS-переменных темы (primary + цвета chart).
const COLORS = [
  'hsl(var(--primary))',
  'hsl(217 91% 60%)',
  'hsl(142 71% 45%)',
  'hsl(0 84% 60%)',
  'hsl(38 92% 50%)',
  'hsl(280 73% 60%)',
];
const DASHES = ['0', '6 3', '2 3', '8 3 2 3'];

interface Props {
  series: ResultSeries[];
}

export function ResultChart({ series }: Props) {
  const chartData = useMemo(() => {
    const multi = series.length > 1;
    const lines: { key: string; name: string; color: string; dash: string }[] = [];
    const byTime = new Map<number, Record<string, number>>();

    series.forEach((s, sIdx) => {
      const variableNames = Object.keys(s.result.variables);
      variableNames.forEach((vName, vIdx) => {
        const key = multi ? `${s.label} · ${vName}` : vName;
        lines.push({
          key,
          name: key,
          color: COLORS[(sIdx * 3 + vIdx) % COLORS.length],
          dash: DASHES[sIdx % DASHES.length],
        });
        const arr = s.result.variables[vName];
        s.result.timePoints.forEach((t, i) => {
          const point = byTime.get(t) ?? { t };
          point[key] = arr[i];
          byTime.set(t, point);
        });
      });
    });

    return { lines, data: [...byTime.values()].sort((a, b) => a.t - b.t) };
  }, [series]);

  if (series.length === 0) {
    return <p className="text-sm text-muted-foreground">Нет данных для построения графика.</p>;
  }

  return (
    <Card className="p-4">
      <ResponsiveContainer width="100%" height={360}>
        <LineChart data={chartData.data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
          <XAxis
            dataKey="t"
            label={{
              value: 'Время',
              position: 'insideBottom',
              offset: -4,
              fill: 'hsl(var(--muted-foreground))',
            }}
            tick={{ fontSize: 12, fill: 'hsl(var(--muted-foreground))' }}
            stroke="hsl(var(--border))"
          />
          <YAxis
            tick={{ fontSize: 12, fill: 'hsl(var(--muted-foreground))' }}
            stroke="hsl(var(--border))"
          />
          <Tooltip
            contentStyle={{
              backgroundColor: 'hsl(var(--popover))',
              border: '1px solid hsl(var(--border))',
              borderRadius: 'var(--radius)',
              color: 'hsl(var(--popover-foreground))',
            }}
          />
          <Legend wrapperStyle={{ color: 'hsl(var(--muted-foreground))' }} />
          {chartData.lines.map((l) => (
            <Line
              key={l.key}
              type="monotone"
              dataKey={l.key}
              name={l.name}
              stroke={l.color}
              strokeDasharray={l.dash}
              strokeWidth={2}
              dot={false}
              isAnimationActive={false}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </Card>
  );
}
