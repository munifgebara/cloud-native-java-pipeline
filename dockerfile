FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

# Copia também o frontend antes do package
COPY frontend frontend

# Se existirem arquivos de config na raiz e o build Angular depender deles,
# copie também. Se algum não existir, depois ajustamos.
COPY src src

RUN ./mvnw -DskipTests clean package

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
