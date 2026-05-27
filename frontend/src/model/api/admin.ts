// Model layer — доступ к админ-API (/api/admin/*). Все вызовы требуют JWT с ролью ADMIN.

import { http } from './client';
import type {
  AuditEntry,
  AuthUser,
  Experiment,
  ModelTemplate,
  Role,
  Validator,
  ValidatorCheckType,
  ValidatorTargetKind,
} from '../types';

// ---------------------------------------------------------------------------
// Users
// ---------------------------------------------------------------------------

export async function listUsers(): Promise<AuthUser[]> {
  const res = await http.get<AuthUser[]>('/admin/users');
  return res.data;
}

export async function createUser(body: {
  email: string;
  password: string;
  role: Role;
}): Promise<AuthUser> {
  const res = await http.post<AuthUser>('/admin/users', body);
  return res.data;
}

export async function updateUserRole(id: string, role: Role): Promise<AuthUser> {
  const res = await http.put<AuthUser>(`/admin/users/${id}/role`, { role });
  return res.data;
}

export async function deleteUser(id: string): Promise<void> {
  await http.delete(`/admin/users/${id}`);
}

// ---------------------------------------------------------------------------
// Models
// ---------------------------------------------------------------------------

export interface CreateModelRequest {
  code: string;
  name: string;
  description: string | null;
  solverType: string;
}

export async function createModel(body: CreateModelRequest): Promise<ModelTemplate> {
  const res = await http.post<ModelTemplate>('/admin/models', body);
  return res.data;
}

export async function updateModel(id: string, body: CreateModelRequest): Promise<ModelTemplate> {
  const res = await http.put<ModelTemplate>(`/admin/models/${id}`, body);
  return res.data;
}

export async function deleteModel(id: string): Promise<void> {
  await http.delete(`/admin/models/${id}`);
}

// ---------------------------------------------------------------------------
// Validators
// ---------------------------------------------------------------------------

export interface CreateValidatorRequest {
  name: string;
  checkType: ValidatorCheckType;
  targetKind: ValidatorTargetKind;
  targetField: string;
  rules: Record<string, unknown>;
  modelTemplateId: string;
}

export interface UpdateValidatorRequest {
  name: string;
  checkType: ValidatorCheckType;
  targetKind: ValidatorTargetKind;
  targetField: string;
  rules: Record<string, unknown>;
}

export async function listValidators(modelTemplateId?: string): Promise<Validator[]> {
  const res = await http.get<Validator[]>('/admin/validators', {
    params: modelTemplateId ? { modelTemplateId } : undefined,
  });
  return res.data;
}

export async function fetchValidator(id: string): Promise<Validator> {
  const res = await http.get<Validator>(`/admin/validators/${id}`);
  return res.data;
}

export async function createValidator(body: CreateValidatorRequest): Promise<Validator> {
  const res = await http.post<Validator>('/admin/validators', body);
  return res.data;
}

export async function updateValidator(
  id: string,
  body: UpdateValidatorRequest,
): Promise<Validator> {
  const res = await http.put<Validator>(`/admin/validators/${id}`, body);
  return res.data;
}

export async function deleteValidator(id: string): Promise<void> {
  await http.delete(`/admin/validators/${id}`);
}

// ---------------------------------------------------------------------------
// Experiments (admin force-delete)
// ---------------------------------------------------------------------------

export async function listAllExperiments(
  limit = 50,
  offset = 0,
): Promise<Experiment[]> {
  const res = await http.get<Experiment[]>('/admin/experiments', {
    params: { limit, offset },
  });
  return res.data;
}

export async function adminDeleteExperiment(id: string): Promise<void> {
  await http.delete(`/admin/experiments/${id}`);
}

// ---------------------------------------------------------------------------
// Audit
// ---------------------------------------------------------------------------

export interface AuditQuery {
  action?: string;
  userId?: string;
  from?: string;
  to?: string;
  limit?: number;
  offset?: number;
}

export async function listAudit(query: AuditQuery = {}): Promise<AuditEntry[]> {
  const res = await http.get<AuditEntry[]>('/admin/audit', { params: query });
  return res.data;
}
