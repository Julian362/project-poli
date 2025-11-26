FROM eclipse-temurin:17-jdk
WORKDIR /app
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY pom.xml /app/pom.xml
COPY src /app/src
RUN mvn -q -f /app/pom.xml package -DskipTests
EXPOSE 8082
CMD ["bash", "-lc", "java -jar /app/target/analytics-spring-0.1.0.jar"]
