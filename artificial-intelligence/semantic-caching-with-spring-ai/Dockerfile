# --- Build stage ---
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app
COPY . .

# Use parallel threads and configure Gradle for speed
RUN ./gradlew :artificial-intelligence:semantic-caching-with-spring-ai:bootJar \
    --no-daemon \
    --parallel \
    --build-cache \
    --configuration-cache \
    --max-workers=$(nproc)

# --- Runtime stage ---
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /app/artificial-intelligence/semantic-caching-with-spring-ai/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]