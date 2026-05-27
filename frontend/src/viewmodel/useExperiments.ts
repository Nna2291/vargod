// ViewModel layer — view-models вычислительных экспериментов.
// Хуки связывают View с Model-слоем (api/experiments). Возвращают форму
// { data, loading, error, ...actions }. JSX в этом слое отсутствует.

import { useCallback, useEffect, useState } from 'react';
import type { ApiError, CreateExperimentRequest, Experiment } from '../model/types';
import * as experimentsApi from '../model/api/experiments';
import type { ExperimentScope } from '../model/api/experiments';
import { toApiError } from '../model/api/client';
import { useSessionStore } from './sessionStore';

/**
 * View-model списка экспериментов.
 * Для scope='mine' запрос выполняется только при наличии userId
 * (JWT обязателен) — иначе список остаётся пустым.
 */
export function useExperiments(scope: ExperimentScope) {
  const user = useSessionStore((s) => s.user);
  const [data, setData] = useState<Experiment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    if (scope === 'mine' && !user) {
      setData([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setData(await experimentsApi.fetchExperiments(scope));
    } catch (e) {
      setError(toApiError(e).message);
    } finally {
      setLoading(false);
    }
  }, [scope, user]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { data, loading, error, reload };
}

/**
 * View-model страницы эксперимента: загрузка, запуск симуляции, публикация.
 * Действия run/publish обновляют локальное состояние ответом сервера.
 */
export function useExperiment(id: string | undefined) {
  const [data, setData] = useState<Experiment | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const reload = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      setData(await experimentsApi.fetchExperiment(id));
    } catch (e) {
      setError(toApiError(e).message);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void reload();
  }, [reload]);

  /** Запускает синхронную симуляцию и сохраняет обновлённый эксперимент. */
  const run = useCallback(async () => {
    if (!id) return;
    setBusy(true);
    setError(null);
    try {
      setData(await experimentsApi.runExperiment(id));
    } catch (e) {
      setError(toApiError(e).message);
    } finally {
      setBusy(false);
    }
  }, [id]);

  /** Публикует эксперимент и сохраняет обновлённое состояние. */
  const publish = useCallback(async () => {
    if (!id) return;
    setBusy(true);
    setError(null);
    try {
      setData(await experimentsApi.publishExperiment(id));
    } catch (e) {
      setError(toApiError(e).message);
    } finally {
      setBusy(false);
    }
  }, [id]);

  /** Снимает публичность с эксперимента. */
  const unpublish = useCallback(async () => {
    if (!id) return;
    setBusy(true);
    setError(null);
    try {
      setData(await experimentsApi.unpublishExperiment(id));
    } catch (e) {
      setError(toApiError(e).message);
    } finally {
      setBusy(false);
    }
  }, [id]);

  return { data, loading, error, busy, reload, run, publish, unpublish };
}

/**
 * View-model формы создания эксперимента.
 * Хранит структурированную ошибку (apiError) — это позволяет View показать
 * details[] при статусе 422 (ошибки валидации параметров).
 */
export function useCreateExperiment() {
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState<ApiError | null>(null);

  /** Отправляет запрос; возвращает созданный эксперимент или null при ошибке. */
  const submit = useCallback(
    async (body: CreateExperimentRequest): Promise<Experiment | null> => {
      setSubmitting(true);
      setApiError(null);
      try {
        return await experimentsApi.createExperiment(body);
      } catch (e) {
        setApiError(toApiError(e));
        return null;
      } finally {
        setSubmitting(false);
      }
    },
    [],
  );

  return { submit, submitting, apiError };
}
