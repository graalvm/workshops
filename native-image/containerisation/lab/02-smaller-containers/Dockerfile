FROM gcr.io/distroless/base

ARG APP_FILE
EXPOSE 8080

COPY ${APP_FILE} app
ENTRYPOINT ["/app"]