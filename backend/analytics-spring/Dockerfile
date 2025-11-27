FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package spring-boot:repackage

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/analytics-spring-0.1.0.jar app.jar
EXPOSE 8082
ENV JAVA_OPTS=""
ENTRYPOINT ["java","-jar","app.jar"]
