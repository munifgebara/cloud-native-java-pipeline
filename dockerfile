FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

COPY frontend frontend
COPY src src

ARG GIT_COMMIT=local

RUN ./mvnw -DskipTests clean package  -Dgit.commit=${GIT_COMMIT}

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN addgroup -S stella && adduser -S stella -G stella

COPY --from=build --chown=stella:stella /app/target/*.jar app.jar

USER stella

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
