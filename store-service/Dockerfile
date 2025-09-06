# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

# Pre-fetch dependencies
COPY pom.xml ./
RUN mvn -q -e -DskipTests dependency:go-offline

# Build application
COPY src ./src
RUN mvn -q clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/store-service-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8081
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]


