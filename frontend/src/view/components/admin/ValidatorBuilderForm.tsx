// View layer — конструктор валидатора. Запрашивает список типов правил
// (validator-types) и динамически рендерит поля по paramsSchema.

import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { listValidatorTypes } from '@/model/api/validatorTypes';
import * as adminApi from '@/model/api/admin';
import { toApiError } from '@/model/api/client';
import type {
  RuleParamsField,
  Validator,
  ValidatorTargetKind,
  ValidatorTypeInfo,
} from '@/model/types';
import { Button } from '@/view/components/ui/button';
import { Input } from '@/view/components/ui/input';
import { Label } from '@/view/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/view/components/ui/select';
import { Badge } from '@/view/components/ui/badge';
import { ValidationAlert } from '@/view/components/ValidationAlert';

const RANGE_TYPE = 'RANGE';

interface Props {
  modelTemplateId: string;
  /** При редактировании — существующий валидатор. */
  initial?: Validator;
  /** Внешний submit (создание/обновление). */
  onSubmit: (payload: {
    name: string;
    checkType: 'RANGE' | 'CONSTRAINT';
    targetKind: ValidatorTargetKind;
    targetField: string;
    rules: Record<string, unknown>;
  }) => Promise<void> | void;
  submitting?: boolean;
  submitLabel?: string;
}

/** Подбирает строковое значение rules по имени поля или возвращает дефолт. */
function getValue(rules: Record<string, unknown> | undefined, name: string): string {
  if (!rules) return '';
  const v = rules[name];
  if (v === undefined || v === null) return '';
  if (Array.isArray(v)) return v.join(', ');
  if (typeof v === 'boolean') return v ? 'true' : 'false';
  return String(v);
}

export function ValidatorBuilderForm({
  modelTemplateId,
  initial,
  onSubmit,
  submitting,
  submitLabel = 'Сохранить',
}: Props) {
  const typesQ = useQuery({
    queryKey: ['admin', 'validator-types'],
    queryFn: listValidatorTypes,
  });

  const [name, setName] = useState(initial?.name ?? '');
  const [targetKind, setTargetKind] = useState<ValidatorTargetKind>(
    initial?.targetKind ?? 'PARAMETER',
  );
  const [targetField, setTargetField] = useState(initial?.targetField ?? '');
  // Текущий тип правила. Для RANGE — checkType = RANGE, иначе CONSTRAINT.
  const [ruleType, setRuleType] = useState<string>(
    initial?.checkType === 'RANGE'
      ? RANGE_TYPE
      : (initial?.rules?.type as string | undefined) ?? '',
  );
  const [paramValues, setParamValues] = useState<Record<string, string>>(() => {
    if (!initial) return {};
    const init: Record<string, string> = {};
    Object.entries(initial.rules).forEach(([k, v]) => {
      if (k === 'type') return;
      if (Array.isArray(v)) init[k] = v.join(', ');
      else if (v !== null && v !== undefined) init[k] = String(v);
    });
    return init;
  });
  const [submitError, setSubmitError] = useState<ReturnType<typeof toApiError> | null>(null);

  // При выборе нового типа правила сбрасываем значения параметров.
  const selectedType: ValidatorTypeInfo | undefined = useMemo(
    () => typesQ.data?.find((t) => t.type === ruleType),
    [typesQ.data, ruleType],
  );

  useEffect(() => {
    if (!initial) {
      setParamValues({});
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ruleType]);

  const setParam = (k: string, v: string) =>
    setParamValues((prev) => ({ ...prev, [k]: v }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);

    if (!name.trim() || !targetField.trim() || !ruleType) return;

    // Сериализация: учитываем тип каждого поля.
    const rules: Record<string, unknown> = {};
    if (selectedType) {
      for (const f of selectedType.paramsSchema.fields) {
        const raw = paramValues[f.name];
        if ((raw === undefined || raw === '') && !f.required) continue;
        rules[f.name] = serialize(f, raw ?? '');
      }
    }
    const checkType: 'RANGE' | 'CONSTRAINT' = ruleType === RANGE_TYPE ? 'RANGE' : 'CONSTRAINT';
    if (checkType === 'CONSTRAINT') rules.type = ruleType;

    try {
      await onSubmit({
        name: name.trim(),
        checkType,
        targetKind,
        targetField: targetField.trim(),
        rules,
      });
    } catch (err) {
      setSubmitError(toApiError(err));
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <input type="hidden" value={modelTemplateId} readOnly />

      <div className="space-y-2">
        <Label htmlFor="v-name">Название</Label>
        <Input
          id="v-name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="например, ограничение beta"
          required
        />
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="v-target-kind">Тип цели</Label>
          <Select value={targetKind} onValueChange={(v) => setTargetKind(v as ValidatorTargetKind)}>
            <SelectTrigger id="v-target-kind">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="PARAMETER">PARAMETER (параметр модели)</SelectItem>
              <SelectItem value="DATASET_COLUMN">
                DATASET_COLUMN (столбец данных)
              </SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-2">
          <Label htmlFor="v-target-field">Имя целевого поля</Label>
          <Input
            id="v-target-field"
            value={targetField}
            onChange={(e) => setTargetField(e.target.value)}
            placeholder="например, beta"
            required
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="v-rule-type">Тип правила</Label>
        {typesQ.isLoading ? (
          <p className="text-sm text-muted-foreground">Загрузка типов…</p>
        ) : (
          <Select value={ruleType} onValueChange={setRuleType}>
            <SelectTrigger id="v-rule-type">
              <SelectValue placeholder="— выберите тип правила —" />
            </SelectTrigger>
            <SelectContent>
              {typesQ.data?.map((t) => (
                <SelectItem key={t.type} value={t.type}>
                  {t.type}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      </div>

      {selectedType && (
        <div className="space-y-3 rounded-md border bg-muted/30 p-4">
          <h4 className="text-sm font-semibold">Параметры правила «{selectedType.type}»</h4>
          {selectedType.paramsSchema.fields.length === 0 ? (
            <p className="text-sm text-muted-foreground">У правила нет параметров.</p>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2">
              {selectedType.paramsSchema.fields.map((f) => (
                <ParamField
                  key={f.name}
                  field={f}
                  value={paramValues[f.name] ?? getValue(initial?.rules, f.name)}
                  onChange={(v) => setParam(f.name, v)}
                />
              ))}
            </div>
          )}
        </div>
      )}

      {submitError && <ValidationAlert error={submitError} />}

      <div className="flex justify-end">
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Сохранение…' : submitLabel}
        </Button>
      </div>
    </form>
  );
}

function ParamField({
  field,
  value,
  onChange,
}: {
  field: RuleParamsField;
  value: string;
  onChange: (v: string) => void;
}) {
  if (field.type === 'BOOLEAN') {
    return (
      <div className="space-y-2">
        <Label htmlFor={`pf-${field.name}`} className="flex items-center gap-2">
          <input
            id={`pf-${field.name}`}
            type="checkbox"
            checked={value === 'true'}
            onChange={(e) => onChange(e.target.checked ? 'true' : 'false')}
            className="h-4 w-4 rounded border-input"
          />
          {field.name}
          {field.required && <Badge variant="outline">обязат.</Badge>}
        </Label>
        {field.description && (
          <p className="text-xs text-muted-foreground">{field.description}</p>
        )}
      </div>
    );
  }

  if (field.type === 'STRING' && field.allowedValues && field.allowedValues.length > 0) {
    return (
      <div className="space-y-2">
        <Label htmlFor={`pf-${field.name}`}>
          {field.name} {field.required && <span className="text-destructive">*</span>}
        </Label>
        <Select value={value} onValueChange={onChange}>
          <SelectTrigger id={`pf-${field.name}`}>
            <SelectValue placeholder="— выбрать —" />
          </SelectTrigger>
          <SelectContent>
            {field.allowedValues.map((v) => (
              <SelectItem key={v} value={v}>
                {v}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {field.description && (
          <p className="text-xs text-muted-foreground">{field.description}</p>
        )}
      </div>
    );
  }

  const inputType =
    field.type === 'NUMBER' || field.type === 'INTEGER' ? 'number' : 'text';
  const step = field.type === 'INTEGER' ? '1' : 'any';

  return (
    <div className="space-y-2">
      <Label htmlFor={`pf-${field.name}`}>
        {field.name} {field.required && <span className="text-destructive">*</span>}
      </Label>
      <Input
        id={`pf-${field.name}`}
        type={inputType}
        step={inputType === 'number' ? step : undefined}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={field.type === 'LIST_OF_STRING' ? 'значения через запятую' : undefined}
      />
      {field.description && (
        <p className="text-xs text-muted-foreground">{field.description}</p>
      )}
    </div>
  );
}

function serialize(field: RuleParamsField, raw: string): unknown {
  switch (field.type) {
    case 'NUMBER':
      return Number(raw);
    case 'INTEGER':
      return Math.trunc(Number(raw));
    case 'BOOLEAN':
      return raw === 'true';
    case 'LIST_OF_STRING':
      return raw
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
    case 'STRING':
    default:
      return raw;
  }
}

// Хелперы для использования с admin API
export const validatorBuilderHelpers = {
  createOrUpdate: async (
    id: string | undefined,
    payload: {
      name: string;
      checkType: 'RANGE' | 'CONSTRAINT';
      targetKind: ValidatorTargetKind;
      targetField: string;
      rules: Record<string, unknown>;
    },
    modelTemplateId: string,
  ) => {
    if (id) {
      return adminApi.updateValidator(id, payload);
    }
    return adminApi.createValidator({ ...payload, modelTemplateId });
  },
};
