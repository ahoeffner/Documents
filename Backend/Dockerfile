# --- Stage 1: Build Vue.js frontend ---
FROM node:20-slim AS frontend-build
WORKDIR /frontend
COPY Frontend/package*.json ./
RUN npm ci --prefer-offline
COPY Frontend/ ./
RUN npm run build

# --- Stage 2: Compile Spring Boot app to native binary (ARM64) ---
FROM --platform=linux/arm64 ghcr.io/graalvm/native-image-community:17 AS native-build
RUN microdnf install -y maven
WORKDIR /app
COPY Backend/pom.xml .
# Place pre-built frontend where the Maven plugin expects it
COPY --from=frontend-build /frontend/dist /Frontend/dist
RUN mvn dependency:go-offline -q
COPY Backend/src/ src/
RUN mvn -Pnative native:compile -DskipTests -DskipFrontend=true -q

# --- Stage 3: Minimal ARM64 runtime with Tesseract ---
FROM --platform=linux/arm64 debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
        tesseract-ocr \
        tesseract-ocr-dan \
        tesseract-ocr-eng \
        tesseract-ocr-deu \
        tesseract-ocr-fra \
        tesseract-ocr-spa \
        libgomp1 \
        poppler-utils \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=native-build /app/target/documents documents
ENV PATH="/app:$PATH"
EXPOSE 8080
ENTRYPOINT ["/app/documents","-d"]
