FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /app/logs /app/reports
COPY --from=builder /app/target/art-gallery-1.0.0.jar app.jar
EXPOSE 8100
ENTRYPOINT ["java", "-jar", "app.jar"]