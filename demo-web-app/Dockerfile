FROM ghcr.io/hiconic-os/runtime-base:latest

COPY ./dist/application /app

EXPOSE 8080

ENTRYPOINT ["/app/bin/run-or-debug"]
