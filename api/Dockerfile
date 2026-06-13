# ============================================================
#  Образ REST API-сервера Vargod (компонент связи GUI и бизнес-логики).
#  Многоступенчатая сборка; контекст — корень репозитория.
# ============================================================

# --- Стадия сборки ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew --no-daemon :api:bootJar -x test

# --- Рантайм-образ ---
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/api/build/libs/api-1.0.0.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
