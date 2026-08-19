# Fawnly SAST — backend (Java 17 + Git + Semgrep) ve frontend (nginx)
#
# Projeyi ayağa kaldırmak (önerilen):
#   docker compose up --build
#
# Compose yoksa:
#   docker network create fawnly-net
#   docker run -d --name fawnly-postgres --network fawnly-net \
#     -e POSTGRES_DB=fawnly -e POSTGRES_USER=fawnly -e POSTGRES_PASSWORD=changeme \
#     -p 5432:5432 postgres:16-alpine
#   docker build --target backend -t fawnly-backend .
#   docker run -d --name fawnly-backend --network fawnly-net -p 8080:8080 \
#     -e DB_URL=jdbc:postgresql://fawnly-postgres:5432/fawnly \
#     -e DB_USERNAME=fawnly -e DB_PASSWORD=changeme \
#     -e JWT_SECRET=change-this-to-a-256-bit-secret-key-minimum-32-chars!! \
#     -e CORS_ORIGINS=http://localhost:5173 \
#     -e GMAIL_USERNAME -e GMAIL_APP_PASSWORD \
#     fawnly-backend
#   docker build --target frontend -t fawnly-frontend .
#   docker run -d --name fawnly-frontend --network fawnly-net -p 5173:80 fawnly-frontend
#
# Arayüz:  http://localhost:5173
# API:     http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
#
# OTP e-postası için GMAIL_USERNAME ve GMAIL_APP_PASSWORD verin.

# ---------- Backend derleme ----------
FROM maven:3.9.6-eclipse-temurin-17 AS backend-build
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -q -DskipTests package

# ---------- Backend çalışma anı ----------
FROM eclipse-temurin:17-jre-jammy AS backend
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends git python3 python3-pip curl \
    && pip3 install --no-cache-dir semgrep \
    && rm -rf /var/lib/apt/lists/*

COPY --from=backend-build /build/target/fawnly-backend-1.0.0.jar /app/app.jar
COPY rules /app/rules

ENV SEMGREP_PATH=semgrep \
    SEMGREP_RULES_PATH=/app/rules/owasp_java.yaml \
    SCAN_TEMP_DIR=/tmp/sast-scans \
    SERVER_PORT=8080

RUN mkdir -p /tmp/sast-scans

EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=5s --start-period=60s --retries=10 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# ---------- Frontend derleme ----------
FROM node:20-alpine AS frontend-build
WORKDIR /web
COPY frontend/package.json ./
RUN npm install
COPY frontend/ ./
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

# ---------- Frontend çalışma anı ----------
FROM nginx:1.27-alpine AS frontend
COPY --from=frontend-build /web/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
