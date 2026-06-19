# Stage 1: Build the application using Maven and Java 21
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY certs/ /tmp/certs/
RUN apk add --no-cache ca-certificates && \
    for cert in /tmp/certs/*.pem; do \
      [ -f "$cert" ] && [ -s "$cert" ] && \
        cp "$cert" /usr/local/share/ca-certificates/$(basename "$cert" .pem).crt && \
        keytool -import -noprompt -trustcacerts \
          -alias "$(basename $cert .pem)" -file "$cert" \
          -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit || true; \
    done && update-ca-certificates
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Prepare the JRE and conditionally import the Zscaler cert
FROM eclipse-temurin:21-jre-jammy AS jre-with-cert
WORKDIR /app

ARG IMPORT_ZSCALER_CERT=false

# Wildcard prevents a build failure when the cert file is absent (production)
COPY zscaler.ce[r] /tmp/zscaler.cer

RUN if [ "$IMPORT_ZSCALER_CERT" = "true" ] && [ -f /tmp/zscaler.cer ]; then \
      echo ">>> Importing Zscaler certificate for local development..."; \
      keytool -importcert -file /tmp/zscaler.cer -alias zscaler-root -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt; \
    else \
      echo ">>> Skipping Zscaler certificate import for production build..."; \
    fi

# Stage 3: Final runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# curl is used by the HEALTHCHECK; install before switching to non-root user
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Run as a non-root user
RUN groupadd --system appgroup && useradd --system --gid appgroup --no-create-home appuser

COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar
COPY --from=jre-with-cert $JAVA_HOME/lib/security/cacerts $JAVA_HOME/lib/security/cacerts

USER appuser

# Any HTTP response (200, 302, 401, 403) means Spring Boot is up and accepting connections.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -s --max-time 5 http://localhost:8080/ -o /dev/null -w "%{http_code}" | grep -qE "[0-9]{3}"

EXPOSE 8080

# 'exec' replaces the shell with the Java process so it becomes PID 1 and
# receives SIGTERM directly from Docker, enabling graceful shutdown.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
