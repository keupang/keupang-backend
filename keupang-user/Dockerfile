# 1. Base image
FROM openjdk:17-jdk-slim

# 2. Work directory
WORKDIR /app

# 3. Copy JAR file
COPY build/libs/*.jar app.jar

# 4. Expose port
EXPOSE 8081

# 5. Run application
ENTRYPOINT ["java", "-jar", "app.jar"]