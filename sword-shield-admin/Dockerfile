# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY . .
RUN mvn -B -pl sword-shield-admin -am install -DskipTests && \
    cd sword-shield-admin && mvn -B package spring-boot:repackage -DskipTests

FROM eclipse-temurin:17-jre
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    JAVA_AGENT_OPTS=""
WORKDIR /app
COPY --from=builder /workspace/sword-shield-admin/target/sword-shield-admin-*.jar app.jar
COPY otel/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
EXPOSE 8106 20880
ENTRYPOINT ["sh","-c","java ${JAVA_OPTS} ${JAVA_AGENT_OPTS} -jar /app/app.jar"]
