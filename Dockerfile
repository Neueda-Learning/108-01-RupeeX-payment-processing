# Multi-stage Dockerfile: build with Maven, run on JRE
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -Dserver.port=${SERVER_PORT:-8080} -jar /app/app.jar"]
