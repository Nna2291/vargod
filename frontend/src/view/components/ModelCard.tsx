// View layer — карточка математической модели в каталоге.

import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import type { ModelTemplate } from '@/model/types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/view/components/ui/card';
import { Badge } from '@/view/components/ui/badge';

export function ModelCard({ model }: { model: ModelTemplate }) {
  return (
    <Link
      to={`/models/${model.id}`}
      className="group block transition-transform hover:-translate-y-0.5"
    >
      <Card className="h-full transition-colors group-hover:border-primary/60">
        <CardHeader className="space-y-2">
          <div className="flex items-center justify-between">
            <Badge variant="outline" className="font-mono text-xs">
              {model.code}
            </Badge>
            <ArrowRight className="h-4 w-4 text-muted-foreground transition-transform group-hover:translate-x-1 group-hover:text-primary" />
          </div>
          <CardTitle className="text-lg">{model.name}</CardTitle>
        </CardHeader>
        <CardContent>
          <CardDescription className="line-clamp-3">
            {model.description ?? 'Описание отсутствует.'}
          </CardDescription>
        </CardContent>
      </Card>
    </Link>
  );
}
