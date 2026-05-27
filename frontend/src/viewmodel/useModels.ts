// ViewModel layer — view-models каталога моделей.
// Хуки связывают View с Model-слоем (api/models). Возвращают форму
// { data, loading, error, ...actions }. JSX в этом слое отсутствует.

import { useCallback, useEffect, useState } from 'react';
import type { ModelTemplate } from '../model/types';
import * as modelsApi from '../model/api/models';
import { toApiError } from '../model/api/client';

/** View-model каталога моделей: загружает список шаблонов. */
export function useModels() {
  const [data, setData] = useState<ModelTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await modelsApi.fetchModels());
    } catch (e) {
      setError(toApiError(e).message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { data, loading, error, reload };
}

/** View-model страницы модели: загружает один шаблон по id. */
export function useModel(id: string | undefined) {
  const [data, setData] = useState<ModelTemplate | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      setData(await modelsApi.fetchModel(id));
    } catch (e) {
      setError(toApiError(e).message);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { data, loading, error, reload };
}
