# ============================================================
#  Образ технологического UI (CLI) приложения Vargod.
#  Многоступенчатая сборка: компиляция мультимодульного проекта и
#  сборка дистрибутива выполняются внутри контейнера.
#  Контекст сборки — корень репозитория.
# ============================================================

# --- Стадия сборки: JDK 17 + Gradle Wrapper ---
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
# installDist компилирует все компоненты в отдельные JAR-файлы и связывает их
RUN ./gradlew --no-daemon :app-cli:installDist -x test

# --- Рантайм-образ: только JRE + собранные компоненты ---
FROM eclipse-temurin:17-jre
WORKDIR /app
# Каталог lib/ содержит собранные компоненты (domain, data-access,
# business-logic, app-cli) отдельными файлами; vargod.properties — внешний конфиг.
COPY --from=build /src/app-cli/build/install/app-cli/ ./
ENTRYPOINT ["./bin/app-cli"]
