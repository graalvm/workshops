# Base Image
FROM container-registry.oracle.com/graalvm/native-image:24 AS builder

# Set the working directory to /home/app
WORKDIR /build

# Copy the source code into the image for building
COPY . /build

# Build
RUN ./mvnw --no-transfer-progress clean package -Pnative

# The deployment Image
FROM gcr.io/distroless/java-base-debian12

EXPOSE 8080

# Copy the native executable into the containers
COPY --from=builder /build/target/jibber .
ENTRYPOINT ["/jibber"]