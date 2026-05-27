// View layer — главная страница ("/").
// Гостю показываем полноценный лендинг (hero + features + showcase + CTA).
// Авторизованному — компактный каталог моделей.

import { useSessionStore } from '@/viewmodel/sessionStore';
import { useModels } from '@/viewmodel/useModels';
import { ModelCard } from '@/view/components/ModelCard';
import { EmptyMessage, ErrorMessage, Loader } from '@/view/components/StateView';
import { HeroSection } from '@/view/components/landing/HeroSection';
import { FeaturesSection } from '@/view/components/landing/FeaturesSection';
import { HowItWorksSection } from '@/view/components/landing/HowItWorksSection';
import { ModelsShowcase } from '@/view/components/landing/ModelsShowcase';
import { AudienceSection } from '@/view/components/landing/AudienceSection';
import { CtaSection } from '@/view/components/landing/CtaSection';

export function CatalogPage() {
  const { data, loading, error } = useModels();
  const isAuthenticated = useSessionStore((s) => s.token !== null);

  if (loading) return <div className="container py-12"><Loader /></div>;
  if (error) return <div className="container py-12"><ErrorMessage text={error} /></div>;

  // ---------- АВТОРИЗОВАННЫЙ: компактный каталог ----------
  if (isAuthenticated) {
    return (
      <div className="container py-8">
        <header className="mb-8 space-y-2">
          <h1 className="text-3xl font-bold tracking-tight">Каталог моделей</h1>
          <p className="text-muted-foreground">
            Выберите модель, чтобы изучить её параметры и поставить эксперимент.
          </p>
        </header>

        {data.length === 0 && <EmptyMessage text="Каталог моделей пуст." />}
        {data.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {data.map((m) => (
              <ModelCard key={m.id} model={m} />
            ))}
          </div>
        )}
      </div>
    );
  }

  // ---------- ГОСТЬ: полный лендинг ----------
  return (
    <>
      <HeroSection />
      <FeaturesSection />
      <HowItWorksSection />
      {data.length > 0 && <ModelsShowcase models={data} />}
      <AudienceSection />
      <CtaSection />
    </>
  );
}
