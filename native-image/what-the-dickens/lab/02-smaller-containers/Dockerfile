# Base Container Image
FROM container-registry.oracle.com/graalvm/native-image:24 AS builder

# Set the working directory to /build
WORKDIR /build

# Copy the source code into the container for building
COPY . /build

# Build
RUN ./mvnw --no-transfer-progress -Pnative,mostly-static -DskipTests=true clean package

# Deployment Container
# This time we use the distroless image - which is around 20 MB in size. Even smaller versions are available.
FROM gcr.io/distroless/base

# This container will expose TCP port 8080, as this is the port on which your app will listen
EXPOSE 8080

# Copy the native executable into the container
COPY --from=builder /build/target/what_the_dickens .

# Run what_the_dickens when starting the container
ENTRYPOINT ["/what_the_dickens"]