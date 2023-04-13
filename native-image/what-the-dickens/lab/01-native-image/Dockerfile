# Base Container Image
FROM ghcr.io/graalvm/native-image:ol8-java17-22 AS builder

# Install tar and gzip to extract the Maven binaries
RUN microdnf update \
 && microdnf install --nodocs \
    tar \
    gzip \
 && microdnf clean all \
 && rm -rf /var/cache/yum

# Set the working directory to /build
WORKDIR /build

# Copy the source code into the container for building
COPY . /build

# Build
RUN ./mvnw --no-transfer-progress clean package -Pnative -DskipTests=true

# The deployment container image
FROM docker.io/oraclelinux:8-slim

# This container will expose TCP port 8080, as this is the port on which your app will listen
EXPOSE 8080

# Copy the native executable into the container
COPY --from=builder /build/target/What_the_Dickens .

# Run What_the_Dickens when starting the container
ENTRYPOINT ["/What_the_Dickens"]