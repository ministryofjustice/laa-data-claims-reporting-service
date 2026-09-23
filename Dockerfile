# Specify java runtime base image
FROM amazoncorretto:27.0.0-alpine3.23@sha256:37edecbbf70dcf5d4f9dd919847d2783450d72a4ea35efa531649270b2eff8ca

# Set up working directory in the container
RUN mkdir -p /opt/laa-data-reporting-service/claims-reporting/
WORKDIR /opt/laa-data-reporting-service/claims-reporting/

# Copy the JAR file into the container
COPY /build/libs/laa-data-claims-reporting-service-1.0.0.jar app.jar

# Create a group and non-root user
RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup

# Set the default user
USER 1001

# Expose the port that the application will run on
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "app.jar"]