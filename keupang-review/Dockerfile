# 1. Base image
FROM eclipse-temurin:17-jdk-jammy

# 2. Work directory
WORKDIR /app

# 3. Copy JAR file
COPY build/libs/*.jar app.jar

# 5. Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
