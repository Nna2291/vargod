// Model layer — типы данных предметной области.
// TypeScript-зеркало доменных классов backend (модуль domain в Kotlin).
// UUID и Instant сериализуются в JSON как строки.

/** Роль пользователя в системе. */
export type Role = 'ADMIN' | 'RESEARCHER';

/** Тип проверки валидатора. */
export type ValidatorCheckType = 'RANGE' | 'CONSTRAINT';

/** К чему относится проверка валидатора. */
export type ValidatorTargetKind = 'PARAMETER' | 'DATASET_COLUMN';

/** Статус жизненного цикла вычислительного эксперимента. */
export type ExperimentStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

/** Правило проверки значения параметра или столбца данных. */
export interface Validator {
  id: string;
  name: string;
  checkType: ValidatorCheckType;
  targetKind: ValidatorTargetKind;
  targetField: string;
  rules: Record<string, unknown>;
  modelTemplateId?: string;
}

/** Эталонный шаблон математической модели (каталог моделей). */
export interface ModelTemplate {
  id: string;
  code: string;
  name: string;
  description: string | null;
  solverType: string;
  validators: Validator[];
}

/** Набор параметров запуска симуляции. */
export interface ParameterSet {
  id: string;
  values: Record<string, number>;
  tStart: number;
  tEnd: number;
  step: number;
}

/** Результат численной симуляции: ряды значений переменных по времени. */
export interface SimulationResult {
  id: string;
  timePoints: number[];
  variables: Record<string, number[]>;
  createdAt: string;
}

/** Вычислительный эксперимент. */
export interface Experiment {
  id: string;
  name: string | null;
  status: ExperimentStatus;
  isPublic: boolean;
  userId: string;
  modelTemplateId: string;
  parameterSet: ParameterSet | null;
  result: SimulationResult | null;
  createdAt: string;
}

/** Сравнение нескольких экспериментов. */
export interface Comparison {
  id: string;
  name: string | null;
  userId: string;
  experiments: Experiment[];
  createdAt: string;
}

/** Тело запроса на создание эксперимента. */
export interface CreateExperimentRequest {
  name: string | null;
  modelTemplateId: string;
  parameters: Record<string, number>;
  tStart: number;
  tEnd: number;
  step: number;
}

/** Тело запроса на создание сравнения. */
export interface CreateComparisonRequest {
  experimentIds: string[];
}

/** Пользователь системы (как он возвращается из /api/auth/me и /api/admin/users). */
export interface AuthUser {
  id: string;
  email: string;
  role: Role;
  createdAt?: string;
}

/** Ответ /api/auth/{login,register}: JWT + данные пользователя. */
export interface AuthResponse {
  token: string;
  user: AuthUser;
}

/** Запись журнала действий пользователей (admin audit). */
export interface AuditEntry {
  id: string;
  userId: string | null;
  action: string;
  targetType: string | null;
  targetId: string | null;
  payload: Record<string, unknown> | null;
  createdAt: string;
}

/** Описание одного поля параметров правила-валидатора. */
export interface RuleParamsField {
  name: string;
  type: 'NUMBER' | 'INTEGER' | 'STRING' | 'BOOLEAN' | 'LIST_OF_STRING';
  required: boolean;
  description?: string;
  allowedValues?: string[];
}

/** Схема параметров одного типа правила. */
export interface RuleParamsSchema {
  fields: RuleParamsField[];
}

/** Метаданные о доступном типе правила-валидатора (для admin-UI). */
export interface ValidatorTypeInfo {
  type: string;
  paramsSchema: RuleParamsSchema;
}

/**
 * Нормализованная ошибка API. Соответствует JSON-ответу backend
 * { error, message, details } при статусах 4xx/5xx.
 * Поле status хранит HTTP-код для ветвления в UI (например, 422 → показ details).
 */
export interface ApiError {
  error: string;
  message: string;
  details: string[];
  status: number;
}
