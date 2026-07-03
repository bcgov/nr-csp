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

# Imports every *.cer / *.pem / *.crt found in certs/ — silently skips missing or empty files.
# Keep the certs/ directory committed (even if empty) so the bind mount never fails.
RUN --mount=type=bind,source=certs,target=/certs \
    for cert in /certs/*.cer /certs/*.pem /certs/*.crt; do \
      [ -s "$cert" ] || continue; \
      alias=$(basename "${cert%.*}"); \
      echo ">>> Importing certificate: $(basename "$cert") (alias: $alias)"; \
      keytool -importcert \
        -file "$cert" \
        -alias "$alias" \
        -cacerts \
        -storepass changeit \
        -noprompt; \
    done

# Stage 3: Final runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd --system appgroup && \
    useradd --system --gid appgroup --no-create-home appuser

COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar
COPY --from=jre-with-cert $JAVA_HOME/lib/security/cacerts $JAVA_HOME/lib/security/cacerts
RUN printf 'jdk.tls.disabledAlgorithms=SSLv3, RC4, DES, MD5withRSA, \\\n    DH keySize < 512, EC keySize < 128, anon, NULL\n' \
    > /app/tls-legacy.security && \
    printf '#!/bin/sh\n\
# Merge Oracle cert (written by certextractor init container) into a copy of\n\
# the JVM default cacerts so both Oracle TCPS and public HTTPS are trusted.\n\
cp "$JAVA_HOME/lib/security/cacerts" /tmp/truststore.jks\n\
if [ -f /cert/jssecacerts ]; then\n\
  keytool -importkeystore \\\n\
    -srckeystore /cert/jssecacerts \\\n\
    -destkeystore /tmp/truststore.jks \\\n\
    -srcstorepass:env KEYSTORE_SECRET \\\n\
    -deststorepass changeit \\\n\
    -noprompt 2>/dev/null || true\n\
fi\n\
exec java $JAVA_OPTS \\\n\
  -Djavax.net.ssl.trustStore=/tmp/truststore.jks \\\n\
  -Djavax.net.ssl.trustStorePassword=changeit \\\n\
  -Djava.security.properties=/app/tls-legacy.security \\\n\
  -jar /app/app.jar\n' \
    > /app/entrypoint.sh && \
    chmod +x /app/entrypoint.sh

USER appuser

# Any HTTP response (200, 302, 401, 403) means Spring Boot is up and accepting connections.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -s --max-time 5 http://localhost:8080/ -o /dev/null -w "%{http_code}" | grep -qE "[0-9]{3}"

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]
