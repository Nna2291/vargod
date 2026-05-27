// Model layer — каталог типов правил-валидаторов (для admin builder UI).

import { http } from './client';
import type { ValidatorTypeInfo } from '../types';

/** GET /api/admin/validator-types — список доступных типов правил с paramsSchema. */
export async function listValidatorTypes(): Promise<ValidatorTypeInfo[]> {
  const res = await http.get<ValidatorTypeInfo[]>('/admin/validator-types');
  return res.data;
}
