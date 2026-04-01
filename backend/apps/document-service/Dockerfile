FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew .
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY gradle/libs.versions.toml gradle/libs.versions.toml

COPY libs/ libs/
COPY apps/document-service/ apps/document-service/

RUN chmod +x gradlew && ./gradlew :apps:document-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /workspace/apps/document-service/build/libs/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
