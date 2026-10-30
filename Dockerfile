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
COPY --from=build /app/target/ayurveda-ai-bot-*.jar app.jar

# Dynamic cloud port (Render sets $PORT)
ENV PORT=8081
EXPOSE 8081

# Optimized for Render Free Tier (512MB RAM limit): SerialGC and 384MB heap limit prevents OOM kills
ENTRYPOINT ["sh", "-c", "java -XX:+UseSerialGC -Xmx384m -jar app.jar --server.port=${PORT}"]
