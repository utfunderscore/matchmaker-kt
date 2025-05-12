# Use a lightweight base image with Java installed
FROM eclipse-temurin:23-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the distribution zip into the container
COPY server.zip /app/server.zip

# Unzip the distribution
RUN apk add --no-cache unzip && \
    unzip server.zip && \
    rm server.zip

EXPOSE 7000

# Assuming the zip unpacks to a directory named 'server'
WORKDIR /app/server

# Make sure the startup script is executable
RUN chmod +x bin/server

# Set default command
CMD ["bin/server"]
