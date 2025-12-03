# Etapa 1: construir el JAR con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copiar archivos del proyecto
COPY pom.xml .
COPY src ./src

# Empaquetar (sin correr tests para más rapidez)
RUN mvn clean package -DskipTests

# Etapa 2: imagen final con solo el JAR
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copiar el JAR generado desde la etapa anterior
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto configurado en application.properties
EXPOSE 8082

# Comando para ejecutar la app
ENTRYPOINT ["java", "-jar", "app.jar"]
