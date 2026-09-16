FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY --from=build /app/target/dental-clinic-1.0.0.jar app.jar

RUN mkdir -p /app/uploads && chmod 755 /app/uploads

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]