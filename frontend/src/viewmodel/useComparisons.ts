// ViewModel layer — view-models сравнений экспериментов.
// Хуки связывают View с Model-слоем (api/comparisons). Возвращают форму
// { data, loading, error, ...actions }. JSX в этом слое отсутствует.

import { useCallback, useEffect, useState } from 'react';
import type { ApiError, Comparison } from '../model/types';
import * as comparisonsApi from '../model/api/comparisons';
import { toApiError } from '../model/api/client';
import { useSessionStore } from './sessionStore';

/**
 * View-model списка сравнений и формы создания.
 * Список загружается только для залогиненного пользователя (JWT обязателен).
 */
export function useComparisons() {
  const user = useSessionStore((s) => s.user);
  const [data, setData] = useState<Comparison[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState<ApiError | null>(null);

  const reload = useCallback(async () => {
    if (!user) {
      setData([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setData(await comparisonsApi.fetchComparisons());
    } catch (e) {
      setError(toApiError(e).message);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    void reload();
  }, [reload]);

  /** Создаёт сравнение из выбранных экспериментов и обновляет список. */
  const create = useCallback(
    async (experimentIds: string[]): Promise<Comparison | null> => {
      setSubmitting(true);
      setApiError(null);
      try {
        const created = await comparisonsApi.createComparison(experimentIds);
        await reload();
        return created;
      } catch (e) {
        setApiError(toApiError(e));
        return null;
      } finally {
        setSubmitting(false);
      }
    },
    [reload],
  );

  return { data, loading, error, submitting, apiError, reload, create };
}

/** View-model страницы сравнения: загружает одно сравнение по id. */
export function useComparison(id: string | undefined) {
  const [data, setData] = useState<Comparison | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      setData(await comparisonsApi.fetchComparison(id));
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
