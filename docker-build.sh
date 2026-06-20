#!/bin/bash
# ============================================
# CodeForge Docker 构建与启动脚本
# ============================================
set -e

echo "===== CodeForge Docker 构建 ====="

# 1. 编译所有后端模块
echo "[1/3] 编译后端服务..."
cd largemodel_rearend
JAVA_HOME="${JAVA_HOME:-D:/Java/jdk-21}"
mvn clean package -DskipTests -q
cd ..

# 2. 构建前端
echo "[2/3] 构建前端..."
cd largemodel_frontend
npm install --silent
npm run build
cd ..

# 3. 启动 Docker 编排
echo "[3/3] 启动服务..."
docker compose up -d --build

echo ""
echo "===== CodeForge 启动完成 ====="
echo "前端:    http://localhost"
echo "API:     http://localhost:8080"
echo "MySQL:   localhost:3306"
echo "Redis:   localhost:6379"
echo ""
echo "查看日志: docker compose logs -f"
echo "停止服务: docker compose down"
