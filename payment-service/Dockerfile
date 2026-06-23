# Stage 1: Build the payment-service module
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the parent pom and all module poms to allow maven to resolve the reactor
COPY pom.xml .
COPY auth-service/pom.xml auth-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml

# Now copy the source code of the module we actually want to build
COPY payment-service/src payment-service/src

# Compile and package the module
RUN mvn -pl payment-service -am clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/payment-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
