# Demo Web App

Simple example for a `Reflex` web-application.

Request URL
```
http://localhost:8080/api/main/ReverseText?text=Hello
```

## Build and Run Locally

Assemble (Mandatory precondition for both local run and Docker)
```bash
hc-build ant assemble
```

Run locally
```bash
sh dist/application/bin/run
sh dist/application/bin/run-debug
```

Build docker image:
```bash
hc-build ant build-docker
```

Run Docker image:
```bash
docker run --rm -it -p8080:8080 demo-web-app
docker run --rm -it -p8080:8080 -e DEBUG=true -e demo-web-app
docker run --rm -it -p8080:8080 -e DEBUG=true -e DEBUG_PORT=9000 demo-web-app
```
