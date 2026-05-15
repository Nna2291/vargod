FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle --no-daemon installDist -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/install/vargod ./
ENTRYPOINT ["./bin/vargod"]
