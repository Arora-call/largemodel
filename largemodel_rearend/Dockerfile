# ============================================
# CodeForge — 通用 Spring Boot 服务镜像
# 用法：docker build --build-arg MODULE=codeforge-code -t codeforge-code .
# ============================================
FROM eclipse-temurin:21-jre-alpine

ARG MODULE=codeforge-code
WORKDIR /app

# 复制已构建的 JAR
COPY ${MODULE}/target/${MODULE}-*.jar app.jar

# 健康检查端口
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health || exit 1

EXPOSE ${PORT:-8080}

ENTRYPOINT ["sh", "-c", "java -XX:+UseZGC -Xmx512m -jar app.jar"]
