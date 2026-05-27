// Model layer — доступ к вычислительным экспериментам.

import { http } from './client';
import type { CreateExperimentRequest, Experiment } from '../types';

/** Область выборки экспериментов: публичные или принадлежащие пользователю. */
export type ExperimentScope = 'public' | 'mine';

/**
 * GET /api/experiments?scope=... — список экспериментов.
 * Для scope=mine клиент автоматически добавит заголовок Authorization: Bearer <token>.
 */
export async function fetchExperiments(scope: ExperimentScope): Promise<Experiment[]> {
  const res = await http.get<Experiment[]>('/experiments', { params: { scope } });
  return res.data;
}

/** GET /api/experiments/{id} — эксперимент по идентификатору. */
export async function fetchExperiment(id: string): Promise<Experiment> {
  const res = await http.get<Experiment>(`/experiments/${id}`);
  return res.data;
}

/** POST /api/experiments — создание эксперимента (требует JWT-токен). */
export async function createExperiment(body: CreateExperimentRequest): Promise<Experiment> {
  const res = await http.post<Experiment>('/experiments', body);
  return res.data;
}

/** POST /api/experiments/{id}/run — синхронный запуск симуляции. */
export async function runExperiment(id: string): Promise<Experiment> {
  const res = await http.post<Experiment>(`/experiments/${id}/run`);
  return res.data;
}

/** POST /api/experiments/{id}/publish — публикация эксперимента (требует JWT-токен). */
export async function publishExperiment(id: string): Promise<Experiment> {
  const res = await http.post<Experiment>(`/experiments/${id}/publish`);
  return res.data;
}

/** POST /api/experiments/{id}/unpublish — снять публичность (только владелец). */
export async function unpublishExperiment(id: string): Promise<Experiment> {
  const res = await http.post<Experiment>(`/experiments/${id}/unpublish`);
  return res.data;
}
