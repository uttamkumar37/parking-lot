# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY .mvn/        .mvn/
COPY mvnw         mvnw
COPY pom.xml      pom.xml
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline -q

COPY src/ src/
RUN ./mvnw -B package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

RUN groupadd --system parksmart && useradd --system --gid parksmart parksmart

WORKDIR /app
COPY --from=builder --chown=parksmart:parksmart /app/target/*.jar app.jar

USER parksmart
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
