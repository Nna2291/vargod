// View layer — бейдж статуса эксперимента.

import { Badge } from '@/view/components/ui/badge';
import type { ExperimentStatus } from '@/model/types';

const LABELS: Record<ExperimentStatus, string> = {
  PENDING: 'Ожидает',
  RUNNING: 'Выполняется',
  COMPLETED: 'Завершён',
  FAILED: 'Ошибка',
};

const VARIANTS: Record<
  ExperimentStatus,
  React.ComponentProps<typeof Badge>['variant']
> = {
  PENDING: 'outline',
  RUNNING: 'secondary',
  COMPLETED: 'default',
  FAILED: 'destructive',
};

export function StatusBadge({ status }: { status: ExperimentStatus }) {
  return <Badge variant={VARIANTS[status]}>{LABELS[status]}</Badge>;
}
