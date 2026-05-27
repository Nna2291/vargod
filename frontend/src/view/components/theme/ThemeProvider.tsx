// View layer — провайдер темы (light/dark/system).
// Хранит выбор в localStorage и переключает класс "dark" на <html>.

import * as React from 'react';

export type Theme = 'light' | 'dark' | 'system';

interface ThemeProviderContext {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  /** Эффективное значение темы — light или dark — после разрешения 'system'. */
  resolvedTheme: 'light' | 'dark';
}

const Context = React.createContext<ThemeProviderContext | undefined>(undefined);
const STORAGE_KEY = 'vargod-theme';

function resolve(theme: Theme): 'light' | 'dark' {
  if (theme === 'system') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  return theme;
}

function applyTheme(theme: 'light' | 'dark') {
  const root = document.documentElement;
  root.classList.remove('light', 'dark');
  root.classList.add(theme);
}

export function ThemeProvider({
  children,
  defaultTheme = 'system',
}: {
  children: React.ReactNode;
  defaultTheme?: Theme;
}) {
  const [theme, setThemeState] = React.useState<Theme>(() => {
    const stored = typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY) : null;
    return (stored as Theme) ?? defaultTheme;
  });
  const [resolvedTheme, setResolvedTheme] = React.useState<'light' | 'dark'>(() =>
    typeof window !== 'undefined' ? resolve(theme) : 'light',
  );

  React.useEffect(() => {
    const r = resolve(theme);
    setResolvedTheme(r);
    applyTheme(r);
  }, [theme]);

  // Реагируем на изменение системной темы, если выбран режим 'system'.
  React.useEffect(() => {
    if (theme !== 'system') return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const handler = () => {
      const r = mq.matches ? 'dark' : 'light';
      setResolvedTheme(r);
      applyTheme(r);
    };
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, [theme]);

  const setTheme = React.useCallback((next: Theme) => {
    localStorage.setItem(STORAGE_KEY, next);
    setThemeState(next);
  }, []);

  const value = React.useMemo<ThemeProviderContext>(
    () => ({ theme, setTheme, resolvedTheme }),
    [theme, setTheme, resolvedTheme],
  );

  return <Context.Provider value={value}>{children}</Context.Provider>;
}

export function useTheme(): ThemeProviderContext {
  const ctx = React.useContext(Context);
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider');
  return ctx;
}
