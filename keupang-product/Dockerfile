FROM gradle:8-jdk17 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN ./gradlew clean build -x test --no-daemon

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]