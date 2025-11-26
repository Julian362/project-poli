FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY pom.xml /app/pom.xml
RUN ./usr/local/bin/mvnw -v || true
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY src /app/src
RUN mvn -q -f /app/pom.xml package -DskipTests
EXPOSE 8081
ENV JAVA_OPTS=""
CMD ["bash", "-lc", "java $JAVA_OPTS -jar /app/target/chatbot-spring-0.1.0.jar"]
