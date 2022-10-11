FROM ghcr.io/graalvm/native-image:ol8-java17-22 AS builder

# Install tar and gzip to extract the Maven binaries
RUN microdnf update \
 && microdnf install --nodocs \
    tar \
    gzip \
 && microdnf clean all \
 && rm -rf /var/cache/yum

# Set the working directory to /home/app
WORKDIR /build

# Copy the source code into the image for building
COPY . /build

# Build
RUN ./mvnw --no-transfer-progress clean package -Pdistroless

# Deployment Containers
FROM gcr.io/distroless/base

EXPOSE 8080

COPY --from=builder /build/target/jibber-distroless .
ENTRYPOINT ["/jibber-distroless"]