# Use a stable and verified Java 21 runtime image
FROM openjdk:26-slim-bullseye
# Set the working directory inside the container
WORKDIR /app

# Copy the pre-built JAR file from your 'target' folder into the image
# Renames it to 'app.jar' for a consistent entrypoint
COPY target/PDFMerger-Backend-0.0.1-SNAPSHOT.jar app.jar

# Tell Docker the container listens on port 8888
EXPOSE 8888

# The command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

