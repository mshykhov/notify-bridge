# Build stage
FROM gradle:8.11-jdk21 AS build
WORKDIR /app

COPY .git ./.git
COPY build.gradle.kts settings.gradle.kts ./
COPY notifier-api ./notifier-api
RUN gradle dependencies --no-daemon

COPY src ./src
RUN gradle bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -g 1000 app && adduser -u 1000 -G app -D app
USER app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xmx256m -Xms128m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
