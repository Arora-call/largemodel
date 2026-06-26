# ============================================
# CodeForge — 通用 Spring Boot 服务镜像
# 用法:
#   构建: docker build --build-arg MODULE=codeforge-code -t codeforge-code .
#   运行: docker run -e JAVA_OPTS="-Xmx512m" -p 8082:8082 codeforge-code
# ============================================
FROM eclipse-temurin:21-jre-alpine

ARG MODULE=codeforge-code
WORKDIR /app

# 复制已构建的 JAR
COPY ${MODULE}/target/${MODULE}-*.jar app.jar

# 创建临时目录（代码输出 & 部署）
RUN mkdir -p /app/tmp/code_output /app/tmp/code_deploy

# Spring Boot 端口（各模块不同，运行时指定）
EXPOSE 8080 8081 8082 8083 8084 8085

# JVM 参数可通过环境变量覆盖
ENV JAVA_OPTS="-XX:+UseZGC -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
