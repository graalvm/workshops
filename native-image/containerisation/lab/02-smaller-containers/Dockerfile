# Base Image
FROM container-registry.oracle.com/graalvm/native-image:25 AS builder

# Set the working directory to /home/app
WORKDIR /build

# Copy the source code into the image for building
COPY . /build

# Build
RUN ./mvnw --no-transfer-progress clean package -Pnative,distroless

# Deployment Containers
FROM gcr.io/distroless/base-debian12

EXPOSE 8080

COPY --from=builder /build/target/jibber-distroless .
ENTRYPOINT ["/jibber-distroless"]