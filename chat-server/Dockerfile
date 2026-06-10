FROM gradle:8-jdk17 AS builder
WORKDIR /app
COPY build.gradle settings.gradle* ./
RUN gradle dependencies --no-daemon 2>/dev/null || true
COPY src ./src
RUN gradle bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]