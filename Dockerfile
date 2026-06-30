# =============================================================
#  Compliance Monitoring & Audit Platform — Dockerfile
#  Multi-stage build using Spring Boot layered JARs
#  File: Dockerfile
# =============================================================

# ─────────────────────────────────────────────────────────────
# Stage 1: Builder — extract Spring Boot layers
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Copy Maven wrapper and POM first — cached unless POM changes
COPY .mvn/           .mvn/
COPY mvnw            mvnw
COPY pom.xml         pom.xml

# Pre-download dependencies (cached layer unless pom.xml changes)
RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B --no-transfer-progress -q

# Copy source and build (skip tests — run separately in CI)
COPY src/ src/
RUN ./mvnw package -DskipTests -B --no-transfer-progress -q

# Extract Spring Boot layered JAR for optimal layer caching
WORKDIR /build/extracted
RUN java -Djarmode=layertools \
         -jar /build/target/compliance-audit-platform-*.jar \
         extract


# ─────────────────────────────────────────────────────────────
# Stage 2: Runtime — minimal JRE image
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# ── Security hardening ─────────────────────────────────────────
# Run as non-root user
RUN addgroup -S compliance && adduser -S compliance -G compliance

# ── System packages ────────────────────────────────────────────
# curl for healthcheck, tzdata for correct timezone
RUN apk add --no-cache curl tzdata

# ── Working directory ──────────────────────────────────────────
WORKDIR /app

# ── Report storage directory ───────────────────────────────────
RUN mkdir -p /data/compliance-reports && \
    chown compliance:compliance /data/compliance-reports

# ── Spring Boot layers (ordered for optimal cache reuse) ──────
# dependencies layer changes least frequently
COPY --from=builder --chown=compliance:compliance \
     /build/extracted/dependencies/ ./
# spring-boot-loader
COPY --from=builder --chown=compliance:compliance \
     /build/extracted/spring-boot-loader/ ./
# snapshot-dependencies (external SNAPSHOTs if any)
COPY --from=builder --chown=compliance:compliance \
     /build/extracted/snapshot-dependencies/ ./
# application classes change most frequently — last layer
COPY --from=builder --chown=compliance:compliance \
     /build/extracted/application/ ./

# ── Switch to non-root ─────────────────────────────────────────
USER compliance

# ── Ports ─────────────────────────────────────────────────────
EXPOSE 8080   
# API
EXPOSE 8081 
  # Actuator / management

# ── JVM tuning ────────────────────────────────────────────────
ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+UseStringDeduplication \
  -Djava.security.egd=file:/dev/./urandom \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=UTC"

# ── Healthcheck ────────────────────────────────────────────────
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health/liveness || exit 1

# ── Entrypoint ─────────────────────────────────────────────────
ENTRYPOINT ["sh", "-c", \
  "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

# ── Labels (OCI standard) ─────────────────────────────────────
LABEL org.opencontainers.image.title="Compliance Monitoring & Audit Platform" \
      org.opencontainers.image.description="Enterprise compliance monitoring, audit trail, and reporting" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.vendor="Company" \
      org.opencontainers.image.base.name="eclipse-temurin:17-jre-alpine"
