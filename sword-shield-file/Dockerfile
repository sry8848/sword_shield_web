# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY . .
RUN mvn -B -pl sword-shield-file -am package -DskipTests

FROM eclipse-temurin:17-jre
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    JAVA_AGENT_OPTS=""
WORKDIR /app
COPY --from=builder /workspace/sword-shield-file/target/sword-shield-file-*.jar app.jar
COPY otel/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
EXPOSE 8105
ENTRYPOINT ["sh","-c","java ${JAVA_OPTS} ${JAVA_AGENT_OPTS} -jar /app/app.jar"]
