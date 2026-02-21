# docker build -t blk-hacking-ind-navinkumar-mudaliar .
FROM eclipse-temurin:17-jdk-jammy AS builder
# Linux Ubuntu Jammy LTS chosen for stable build tooling and security updates
WORKDIR /workspace

COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle ./gradle
COPY retirement-saving ./retirement-saving

RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy AS runtime
# Linux Ubuntu Jammy LTS chosen for small runtime and long-term security updates
WORKDIR /app

ENV SERVER_PORT=5477
EXPOSE 5477

COPY --from=builder /workspace/build/libs/blackrock-hackathon-0.0.1-SNAPSHOT.jar /app/app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]