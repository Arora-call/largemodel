#!/bin/bash
# ============================================
# CodeForge Docker 全容器化构建与启动脚本
#
# 前置：仅需 Docker（JDK/Node/Maven 全部在容器内运行）
# 用法：bash docker-build.sh
# ============================================
set -e

echo "===== CodeForge Docker 全容器化构建 ====="

# 0. 仅检查 Docker
command -v docker >/dev/null 2>&1 || { echo >&2 "❌ 未找到 Docker，请先安装 Docker / Docker Desktop"; exit 1; }
echo "✓ Docker $(docker -v | grep -oP '\d+\.\d+\.\d+')"

# 1. Maven 编译后端（容器内执行，挂载源码 + Maven 缓存卷）
echo ""
echo "[1/3] Maven 编译后端（docker://maven:3.9-eclipse-temurin-21）..."
cd largemodel_rearend
docker run --rm \
  -v "$PWD":/build \
  -v codeforge_maven_cache:/root/.m2 \
  -w /build \
  maven:3.9-eclipse-temurin-21 \
  mvn clean package -DskipTests -q
cd ..
echo "✓ 后端编译完成"

# 2. npm 构建前端（容器内执行）
echo ""
echo "[2/3] npm 构建前端（docker://node:22-alpine）..."
cd largemodel_frontend
docker run --rm \
  -v "$PWD":/app \
  -w /app \
  node:22-alpine \
  sh -c "npm install --silent && npm run build"
cd ..
echo "✓ 前端构建完成"

# 3. Docker Compose 编排启动
echo ""
echo "[3/3] Docker Compose 启动所有服务..."
docker compose up -d --build

echo ""
echo "===== CodeForge 启动完成 ====="
echo "  前端:    http://localhost"
echo "  网关:    http://localhost:8080"
echo "  Nacos:   http://localhost:8888/nacos  (nacos / nacos)"
echo "  MySQL:   localhost:3306"
echo "  Redis:   localhost:6379"
echo ""
echo "  查看日志: docker compose logs -f"
echo "  停止服务: docker compose down"
