# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from source so rebuilds are fast
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV JAVA_OPTS=""

# FIX 1: Use a wildcard to ensure the packaged jar is caught correctly
COPY --from=build /app/target/hms-backend-*.jar app.jar

EXPOSE 8080

# FIX 2: Move the port binding property to the end of the command string
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]
