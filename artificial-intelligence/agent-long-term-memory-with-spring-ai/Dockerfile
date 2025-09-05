FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .
RUN ./gradlew :artificial-intelligence:agent-long-term-memory-with-spring-ai:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/artificial-intelligence/agent-long-term-memory-with-spring-ai/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "--add-opens=java.base/java.nio.charset=ALL-UNNAMED", "-jar", "app.jar"]