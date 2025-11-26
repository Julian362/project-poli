FROM eclipse-temurin:17-jdk
WORKDIR /app
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY pom.xml /app/pom.xml
COPY src /app/src
# Ensure Spring Boot creates an executable jar with the main manifest
RUN mvn -q -f /app/pom.xml -DskipTests package spring-boot:repackage
EXPOSE 8082
ENV JAVA_OPTS=""
CMD ["bash", "-lc", "java $JAVA_OPTS -jar /app/target/analytics-spring-0.1.0.jar"]
