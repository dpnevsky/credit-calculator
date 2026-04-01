FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew .
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY gradle/libs.versions.toml gradle/libs.versions.toml

COPY libs/ libs/
COPY apps/scoring-service/ apps/scoring-service/

RUN chmod +x gradlew && ./gradlew :apps:scoring-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /workspace/apps/scoring-service/build/libs/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
