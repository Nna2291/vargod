// ViewModel layer — производные данные модели для формы параметров.
// Чистая логика без JSX: вычисляет список параметров и подсказки диапазонов
// из валидаторов шаблона модели.

import type { ModelTemplate, Validator } from '../model/types';

/** Описание одного входного параметра модели для формы. */
export interface ParameterSpec {
  /** Имя параметра (targetField валидатора). */
  name: string;
  /** Нижняя граница для подсказки HTML-поля, если задана. */
  min?: number;
  /** Верхняя граница для подсказки HTML-поля, если задана. */
  max?: number;
}

/**
 * Извлекает список параметров модели.
 * Параметры — это валидаторы типа RANGE с targetKind=PARAMETER: каждый
 * targetField является одним числовым полем. Валидаторы CONSTRAINT (например,
 * S+I+R=N) — это межполевые правила, а не отдельные параметры.
 */
export function extractParameters(model: ModelTemplate): ParameterSpec[] {
  return model.validators
    .filter((v) => v.checkType === 'RANGE' && v.targetKind === 'PARAMETER')
    .map((v) => ({
      name: v.targetField,
      min: numericRule(v, 'min'),
      max: numericRule(v, 'max'),
    }));
}

/**
 * Возвращает валидаторы-ограничения (CONSTRAINT) модели —
 * межполевые правила для отображения на странице модели.
 */
export function constraintValidators(model: ModelTemplate): Validator[] {
  return model.validators.filter((v) => v.checkType === 'CONSTRAINT');
}

/** Безопасно читает числовое правило из rules валидатора. */
function numericRule(v: Validator, key: string): number | undefined {
  const value = v.rules[key];
  return typeof value === 'number' ? value : undefined;
}
