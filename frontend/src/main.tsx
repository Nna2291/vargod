// View layer — точка входа SPA.
// Wires Model layer (HTTP-клиент с JWT-провайдером) к ViewModel-стору сессии,
// инициализирует TanStack Query и тему, монтирует приложение.

import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App } from './App';
import { setAuthTokenProvider } from './model/api/client';
import { useSessionStore } from './viewmodel/sessionStore';
import { ThemeProvider } from './view/components/theme/ThemeProvider';
import { Toaster } from './view/components/ui/toaster';
import './view/styles/globals.css';

// Model-слой получает токен, не завися от ViewModel-слоя напрямую:
// направление зависимостей остаётся View → ViewModel → Model.
setAuthTokenProvider(() => useSessionStore.getState().token);

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <ThemeProvider defaultTheme="system">
      <QueryClientProvider client={queryClient}>
        <App />
        <Toaster />
      </QueryClientProvider>
    </ThemeProvider>
  </React.StrictMode>,
);
