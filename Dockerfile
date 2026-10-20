# Stage 1: Build the Maven application
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Java Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create directory for persistent local database if needed
RUN mkdir -p /app/data

# Copy compiled jar
COPY --from=build /app/target/*.jar app.jar

# Dynamic cloud port (Render, Railway, Fly.io, etc. set $PORT)
ENV PORT=8081
EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
