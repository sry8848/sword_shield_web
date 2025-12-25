# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# 先拷贝 pom，最大化利用 Docker layer cache（仅 pom 变化才会重新下载依赖）
COPY pom.xml ./
COPY sword-shield-common/pom.xml sword-shield-common/pom.xml
COPY sword-shield-api/pom.xml sword-shield-api/pom.xml
COPY sword-shield-gateway/pom.xml sword-shield-gateway/pom.xml
COPY sword-shield-user/pom.xml sword-shield-user/pom.xml
COPY sword-shield-activity/pom.xml sword-shield-activity/pom.xml
COPY sword-shield-content/pom.xml sword-shield-content/pom.xml
COPY sword-shield-interaction/pom.xml sword-shield-interaction/pom.xml
COPY sword-shield-file/pom.xml sword-shield-file/pom.xml
COPY sword-shield-admin/pom.xml sword-shield-admin/pom.xml
RUN mvn -B -DskipTests -pl sword-shield-content -am dependency:go-offline

# 再拷贝源码并编译
COPY . .
RUN mvn -B -DskipTests -pl sword-shield-content -am package && \
    mvn -B -DskipTests -f sword-shield-content/pom.xml spring-boot:repackage

FROM eclipse-temurin:17-jre
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    JAVA_AGENT_OPTS=""
WORKDIR /app
COPY --from=builder /workspace/sword-shield-content/target/sword-shield-content-*.jar app.jar
COPY otel/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
EXPOSE 8103 20880
ENTRYPOINT ["sh","-c","java ${JAVA_OPTS} ${JAVA_AGENT_OPTS} -jar /app/app.jar"]
