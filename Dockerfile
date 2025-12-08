# --- Etapa 1: Build ---
FROM gradle:8.6-jdk21 AS build
WORKDIR /app

# Copiar todo el proyecto dentro del contenedor
COPY . .

# Construir el proyecto
RUN gradle clean build -x test

# --- Etapa 2: Runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiar el jar generado desde la etapa build
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]