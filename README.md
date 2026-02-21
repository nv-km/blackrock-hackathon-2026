# Retirement Planning (Hackathon 2026)

Spring Boot service for retirement planning.

## Prerequisites
- Java 17 (toolchain is configured for 17)
- Docker (optional, for container build/run)

## Build

```powershell
./gradlew.bat clean build
```

```bash
./gradlew clean build
```

## Run locally

```powershell
./gradlew.bat bootRun
```

```bash
./gradlew bootRun
```

The service listens on port `5477` by default.

## Run tests

```powershell
./gradlew.bat test
```

```bash
./gradlew test
```

## Docker

Build the image from the repo root:

```powershell
docker build -t blk-hacking-ind-navinkumar-mudaliar .
```

Run the container:

```powershell
docker run --rm -p 5477:5477 --name blk-hacking-ind-navinkumar-mudaliar blk-hacking-ind-navinkumar-mudaliar
```
