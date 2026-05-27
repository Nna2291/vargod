// View layer — вспомогательные индикаторы загрузки, ошибки, пустого результата.

import { Loader2 } from 'lucide-react';

export function Loader({ text = 'Загрузка…' }: { text?: string }) {
  return (
    <div className="flex items-center gap-2 rounded-lg border bg-card p-4 text-sm text-muted-foreground">
      <Loader2 className="h-4 w-4 animate-spin" />
      {text}
    </div>
  );
}

export function ErrorMessage({ text }: { text: string }) {
  return (
    <div className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
      Ошибка: {text}
    </div>
  );
}

export function EmptyMessage({ text }: { text: string }) {
  return (
    <div className="rounded-lg border border-dashed bg-card p-6 text-center text-sm text-muted-foreground">
      {text}
    </div>
  );
}
