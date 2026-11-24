# Imagen de la API de ChambaReady (backend/). Se construye desde la RAÍZ del repo porque el backend
# reutiliza las reglas de android/core/domain y el catálogo de versiones de android/gradle.
#   docker build -t chambaready-api .
#   docker run -p 8080:8080 --env-file backend/.env chambaready-api

# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:17-jdk AS compilacion
WORKDIR /app
COPY android/gradle/libs.versions.toml android/gradle/libs.versions.toml
COPY android/core/domain android/core/domain
COPY backend backend
WORKDIR /app/backend
# Por si el archivo llegó con finales de línea de Windows.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew installDist --no-daemon -x test

# ---------- Etapa 2: ejecutar (solo el JRE y la app) ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=compilacion /app/backend/build/install/chambaready-api ./
# Heroku define PORT. La memoria máxima (-Xmx300m) ya viene en el script de inicio.
RUN useradd --system --no-create-home chambaready
USER chambaready
CMD ["bin/chambaready-api"]
