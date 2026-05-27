import path from 'node:path';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Конфигурация сборщика Vite.
// В режиме разработки dev-сервер проксирует запросы /api на REST API
// (Spring Boot, http://localhost:8080), что позволяет фронтенду обращаться
// к относительным путям /api/... как в продакшене (там проксирует nginx).
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
