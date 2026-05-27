// View layer — отображение ошибки API, включая список ошибок валидации (422).

import { AlertTriangle } from 'lucide-react';
import type { ApiError } from '@/model/types';

/**
 * Показывает ошибку API. При статусе 422 (VALIDATION_FAILED) дополнительно
 * выводит details[] — конкретные нарушения проверок параметров.
 */
export function ValidationAlert({ error }: { error: ApiError }) {
  return (
    <div
      role="alert"
      className="flex gap-3 rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-destructive"
    >
      <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0" />
      <div className="space-y-2 text-sm">
        <p className="font-semibold">
          {error.status === 422 ? 'Ошибки валидации' : 'Ошибка'}
        </p>
        <p className="text-destructive/90">{error.message}</p>
        {error.details.length > 0 && (
          <ul className="list-disc space-y-1 pl-5 text-destructive/90">
            {error.details.map((d, i) => (
              <li key={i}>{d}</li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
