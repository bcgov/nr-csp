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

FROM eclipse-temurin:21-jdk-alpine
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
COPY --from=build /app/target/csp-backend-0.0.1-SNAPSHOT.jar app.jar
# Extract the fat JAR so Spring Boot creates a standard file: URLClassLoader.
# This lets JasperReports 7 (JRJdk13Compiler) discover real JAR paths for javac.
RUN jar xf app.jar && rm app.jar
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]