# ============================================================
#  Образ веб-GUI Vargod (React SPA, архитектура MVVM).
#  Многоступенчатая сборка; контекст сборки — каталог ./frontend.
# ============================================================

# --- Стадия 1: сборка статики через Vite ---
FROM node:25-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

# --- Стадия 2: раздача статики через nginx ---
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
