# Stage 1: Build
FROM gradle:8.14-jdk21 AS builder

WORKDIR /app

# Copy gradle files first for better caching
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Download dependencies (cached unless build files change)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application
RUN gradle bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S coffer && \
    adduser -u 1001 -S coffer -G coffer

# Create data directory for image storage
RUN mkdir -p /app/data/images && chown -R coffer:coffer /app/data

# Copy the built jar
COPY --from=builder /app/build/libs/*.jar app.jar

# Set ownership
RUN chown coffer:coffer app.jar

USER coffer

# Environment variables with defaults
ENV DB_HOST=coffer-db \
    DB_PORT=5432 \
    DB_NAME=coffer \
    DB_USERNAME=coffer \
    DB_PASSWORD=coffer \
    STORAGE_BASE_PATH=/app/data/images \
    JAVA_OPTS=""

EXPOSE 8080

# Health check using swagger endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/swagger-ui.html || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
