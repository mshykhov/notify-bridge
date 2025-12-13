# Build stage
FROM gradle:8.11-jdk21 AS build
WORKDIR /app

ARG APP_VERSION=0.0.1-SNAPSHOT

COPY build.gradle.kts settings.gradle.kts ./
RUN gradle dependencies --no-daemon

COPY src ./src
RUN APP_VERSION=${APP_VERSION} gradle bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -g 1000 app && adduser -u 1000 -G app -D app
USER app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xmx256m -Xms128m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
