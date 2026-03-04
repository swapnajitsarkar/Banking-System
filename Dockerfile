# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
# Download dependencies first (layer-cached separately for faster rebuilds)
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Non-root user for security best practice
RUN addgroup -S banking && adduser -S banking -G banking
WORKDIR /app

COPY --from=builder /app/target/digital-banking-system-1.0.0.jar app.jar

# Set ownership
RUN chown banking:banking app.jar
USER banking

EXPOSE 8080

# JVM tuning for containerized environments
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
