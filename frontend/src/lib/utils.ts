// Утилита cn — стандартный хелпер shadcn/ui: объединяет classNames и
// разрешает конфликты Tailwind-классов через tailwind-merge.

import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
