// Model layer — доступ к сравнениям экспериментов.

import { http } from './client';
import type { Comparison } from '../types';

/** GET /api/comparisons — список сравнений пользователя (требует JWT-токен). */
export async function fetchComparisons(): Promise<Comparison[]> {
  const res = await http.get<Comparison[]>('/comparisons');
  return res.data;
}

/** GET /api/comparisons/{id} — сравнение по идентификатору. */
export async function fetchComparison(id: string): Promise<Comparison> {
  const res = await http.get<Comparison>(`/comparisons/${id}`);
  return res.data;
}

/** POST /api/comparisons — создание сравнения из экспериментов (требует JWT-токен). */
export async function createComparison(experimentIds: string[]): Promise<Comparison> {
  const res = await http.post<Comparison>('/comparisons', { experimentIds });
  return res.data;
}
