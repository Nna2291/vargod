// Model layer — доступ к каталогу математических моделей.

import { http } from './client';
import type { ModelTemplate } from '../types';

/** GET /api/models — список всех шаблонов моделей. */
export async function fetchModels(): Promise<ModelTemplate[]> {
  const res = await http.get<ModelTemplate[]>('/models');
  return res.data;
}

/** GET /api/models/{id} — шаблон модели по идентификатору. */
export async function fetchModel(id: string): Promise<ModelTemplate> {
  const res = await http.get<ModelTemplate>(`/models/${id}`);
  return res.data;
}
