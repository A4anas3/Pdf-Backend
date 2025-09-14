# STAGE 1: Build the application using the full JDK 17
FROM openjdk:26-jdk-slim AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven wrapper and project definition files
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies to leverage Docker's layer caching
RUN ./mvnw dependency:go-offline

# Copy the rest of the application's source code
COPY src ./src

# Package the application into a JAR file
RUN ./mvnw package -DskipTests

# STAGE 2: Create the final, lightweight runtime image
FROM openjdk:26-jre-slim

# Set the working directory for the final image
WORKDIR /app

# For better security, create a dedicated non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copy the built JAR file from the 'builder' stage
COPY --from=builder /app/target/PDFMerger-Backend-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your application listens on
EXPOSE 8888

# The command to start your application
ENTRYPOINT ["java", "-jar", "app.jar"]

