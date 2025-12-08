# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY . .
RUN mvn -B -pl sword-shield-gateway -am install -DskipTests && \
    cd sword-shield-gateway && mvn -B package spring-boot:repackage -DskipTests

FROM eclipse-temurin:17-jre
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    JAVA_AGENT_OPTS=""
WORKDIR /app
COPY --from=builder /workspace/sword-shield-gateway/target/sword-shield-gateway-*.jar app.jar
COPY otel/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java ${JAVA_OPTS} ${JAVA_AGENT_OPTS} -jar /app/app.jar"]
