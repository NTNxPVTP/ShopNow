# Stage 1: Build the server module from the multi-module root
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copy the root reactor pom and the server module
COPY pom.xml .
COPY server/pom.xml server/pom.xml
COPY server/src server/src
# Compile and package the server module (skipping tests for speed)
RUN mvn -pl server -am clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy only the built JAR from the first stage
COPY --from=build /app/server/target/*.jar app.jar
# Expose the port Spring Boot runs on
EXPOSE 8080
# Start the app
ENTRYPOINT ["java", "-jar", "app.jar"]
