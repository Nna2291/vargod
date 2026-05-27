// View layer — страница создания эксперимента (маршрут "/experiments/new").

import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { isAdmin, isResearcher, useSessionStore } from '@/viewmodel/sessionStore';
import { useModels } from '@/viewmodel/useModels';
import { useCreateExperiment } from '@/viewmodel/useExperiments';
import { ParameterForm, type ParameterFormValues } from '@/view/components/ParameterForm';
import { ValidationAlert } from '@/view/components/ValidationAlert';
import { ErrorMessage, Loader } from '@/view/components/StateView';
import { Card, CardContent, CardHeader, CardTitle } from '@/view/components/ui/card';
import { Label } from '@/view/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/view/components/ui/select';

export function NewExperimentPage() {
  const navigate = useNavigate();
  const researcher = useSessionStore(isResearcher);
  const admin = useSessionStore(isAdmin);
  const allowed = researcher || admin;
  const [searchParams] = useSearchParams();

  const { data: models, loading, error } = useModels();
  const { submit, submitting, apiError } = useCreateExperiment();

  const [modelId, setModelId] = useState<string>(searchParams.get('model') ?? '');
  const selectedModel = models.find((m) => m.id === modelId) ?? null;

  if (!allowed) {
    return (
      <div className="container max-w-xl py-8">
        <ErrorMessage text="Создание эксперимента доступно только зарегистрированным пользователям." />
      </div>
    );
  }
  if (loading) return <div className="container py-8"><Loader /></div>;
  if (error) return <div className="container py-8"><ErrorMessage text={error} /></div>;

  const handleSubmit = async (values: ParameterFormValues) => {
    const created = await submit({
      name: values.name === '' ? null : values.name,
      modelTemplateId: modelId,
      parameters: values.parameters,
      tStart: values.tStart,
      tEnd: values.tEnd,
      step: values.step,
    });
    if (created) {
      navigate(`/experiments/${created.id}`);
    }
  };

  return (
    <div className="container max-w-3xl space-y-6 py-8">
      <h1 className="text-3xl font-bold tracking-tight">Новый вычислительный эксперимент</h1>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Параметры запуска</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="model-select">Математическая модель</Label>
            <Select value={modelId} onValueChange={setModelId}>
              <SelectTrigger id="model-select">
                <SelectValue placeholder="— выберите модель —" />
              </SelectTrigger>
              <SelectContent>
                {models.map((m) => (
                  <SelectItem key={m.id} value={m.id}>
                    {m.code} — {m.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {apiError && <ValidationAlert error={apiError} />}

          {selectedModel ? (
            <ParameterForm
              key={selectedModel.id}
              model={selectedModel}
              submitting={submitting}
              onSubmit={handleSubmit}
            />
          ) : (
            <p className="text-sm text-muted-foreground">
              Выберите модель, чтобы задать параметры запуска.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
