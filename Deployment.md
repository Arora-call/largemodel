# CodeForge 部署手册

> 更新时间：2026-06-23 | 适用环境：Ubuntu 24.04 LTS + Docker | 全容器化构建（无需安装 JDK/Node/Maven）

---

## 目录

- [一、架构概览](#一架构概览)
- [二、Ubuntu 环境准备](#二ubuntu-环境准备)
- [三、项目配置](#三项目配置)
- [四、构建与启动](#四构建与启动)
- [五、验证服务](#五验证服务)
- [六、常用管理命令](#六常用管理命令)
- [七、Docker Compose 服务清单](#七docker-compose-服务清单)

---

## 一、架构概览

```
┌──────────────────────────────────────────┐
│  Nginx (:80)                             │
│  前端静态资源 + API 反向代理 + SSE 直通   │
└─────────────┬────────────────────────────┘
              │ proxy_pass http://gateway:8080
┌─────────────▼────────────────────────────┐
│  Gateway (:8080)                         │
│  Spring Cloud Gateway — 路由 + CORS + 限流│
└─────────────┬────────────────────────────┘
              │
  ┌───────────┼───────────┬───────────┬──────────┐
  │           │           │           │          │
┌─▼──┐   ┌──▼──┐   ┌───▼───┐  ┌───▼───┐  ┌───▼──┐
│Auth│   │Code │   │ Knowl │  │ Agent │  │Admin │
│8081│   │8082 │   │ 8083  │  │ 8084  │  │ 8085 │
└─┬──┘   └──┬──┘   └───┬───┘  └───┬───┘  └──┬───┘
  │         │           │          │          │
  └─────────┴───────────┴──────────┴──────────┘
         │               │              │
    ┌────▼───┐      ┌───▼────┐    ┌───▼────┐
    │ Nacos  │      │ MySQL  │    │ Redis  │
    │ :8848  │      │ :3306  │    │ :6379  │
    └────────┘      └────────┘    └────────┘
```

**容器间通信**：所有服务通过 Docker Compose 自动创建的 `codeforge_default` 网络互连，使用服务名（如 `gateway`、`mysql`、`redis`）直接访问。

### 关键文件

| 文件 | 位置 | 作用 |
|------|------|------|
| `docker-compose.yml` | 项目根目录 | 11 个服务编排（Nacos + MySQL + Redis + 7 后端 + 1 前端） |
| `docker-build.sh` | 项目根目录 | 一键构建脚本 |
| `largemodel_rearend/Dockerfile` | 后端 | 通用 Spring Boot 镜像（eclipse-temurin:21-jre-alpine） |
| `largemodel_frontend/Dockerfile` | 前端 | 多阶段构建（Node → Nginx） |
| `largemodel_frontend/nginx.conf` | 前端 | Nginx 配置（SPA 路由 + API 代理 + SSE 直通，用 `gateway` 服务名） |
| `codeforge-gateway/.../application-docker.yml` | 后端 Gateway | Docker profile — 路由 URI 使用 Compose 服务名替代 localhost |
| `docker-compose.yml` | 项目根目录 | 服务编排 — Nacos + MySQL + Redis + 7 后端 + 前端（通过 env 覆盖 nacos server-addr） |
| `.env` | 项目根目录（自建） | 环境变量（API Key、端口、密码、Nacos 配置等） |

---

## 二、Ubuntu 环境准备

> 适用：VMware / VirtualBox 中运行的 Ubuntu 24.04 LTS 桌面版。
>
> **只需 Docker，JDK 21 / Node.js 22 / Maven 3.9 全部在容器内运行。**

### 2.1 安装 Docker

```bash
# 官方一键安装脚本
curl -fsSL https://get.docker.com | sudo bash

# 将当前用户加入 docker 组（免 sudo）
sudo usermod -aG docker $USER

# ⚠️ 注销并重新登录，或执行以下命令使组生效
newgrp docker

# 验证
docker run hello-world
```

### 2.2 （可选）安装 Docker Desktop

如果偏好图形化界面管理容器：

```bash
# 下载 .deb 包
wget https://desktop.docker.com/linux/main/amd64/docker-desktop-amd64.deb
sudo apt install -y ./docker-desktop-amd64.deb
```

### 2.3 获取项目代码

```bash
# 方式 A: Git clone（推荐）
git clone <your-repo-url> /home/$USER/codeforge

# 方式 B: VMware 共享文件夹
#   在 VMware 中设置共享文件夹后：
#   sudo mount -t fuse.vmhgfs-fuse .host:/shared-folder /mnt/share -o allow_other
#   cp -r /mnt/share/largemodel /home/$USER/codeforge

# 方式 C: SCP 传输
#   在宿主机执行：
#   scp -r D:\Idea-program-file\largemodel ubuntu@<VM-IP>:/home/ubuntu/codeforge

cd /home/$USER/codeforge
```

### 2.4 验证

```bash
docker -v   # 应显示 27.x 或更高
```

> JDK / Node / Maven **无需安装在宿主机上**。`docker-build.sh` 会通过 `docker run` 拉取 `maven:3.9-eclipse-temurin-21` 和 `node:22-alpine` 镜像在容器内完成编译。

---

## 三、项目配置

### 3.1 创建 .env 文件

在项目根目录创建 `.env`（与 `docker-compose.yml` 同级）：

```bash
cd /home/$USER/codeforge
cat > .env << 'EOF'
# ============================================
# 必填：AI API Key
# ============================================
# DeepSeek:  在 https://platform.deepseek.com 获取
# OpenAI:    在 https://platform.openai.com/api-keys 获取
OPENAI_API_KEY=sk-your-api-key-here

# ============================================
# 可选：数据库
# ============================================
MYSQL_ROOT_PASSWORD=123456
MYSQL_DATABASE=largemodel

# ============================================
# 可选：Nacos（注册中心 + 配置中心）
# ============================================
NACOS_PORT=8848
NACOS_GRPC_PORT=9848

# ============================================
# 可选：端口映射（默认值如下，冲突时修改）
# ============================================
MYSQL_PORT=3306
REDIS_PORT=6379
GATEWAY_PORT=8080
AUTH_PORT=8081
CODE_PORT=8082
KNOWLEDGE_PORT=8083
AGENT_PORT=8084
ADMIN_PORT=8085
FRONTEND_PORT=80
EOF

# 编辑 .env，替换 OPENAI_API_KEY
vim .env
```

### 3.2 关键配置说明

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `OPENAI_API_KEY` | ✅ | 无 | DeepSeek 或 OpenAI 的 API Key |
| `MYSQL_ROOT_PASSWORD` | | `123456` | 生产环境请修改 |
| `FRONTEND_PORT` | | `80` | 如需 80 以外端口，改为如 `8086` |

### 3.3 Linux Docker 兼容性（已修复）

前端的 `nginx.conf` 中 API 代理使用 Docker Compose 网络服务名 `gateway` 而非 `host.docker.internal`（后者仅 Docker Desktop 支持），已在代码中修复，无需手动改动。

---

## 四、构建与启动

> 全容器化构建：Maven 和 npm 均在 Docker 容器内执行，宿主机无需安装 JDK/Node/Maven。

### 4.1 方式一：一键构建（推荐）

```bash
cd /home/$USER/codeforge
bash docker-build.sh
安装 nacos-server-3.1.2
```

脚本自动完成三个阶段：

| 阶段 | 执行容器 | 说明 |
|------|---------|------|
| Maven 编译 | `maven:3.9-eclipse-temurin-21` | 源码挂载到容器，生成 JAR 到各模块 `target/` |
| npm 构建 | `node:22-alpine` | 源码挂载到容器，生成 `dist/` |
| Docker Compose | — | `docker compose up -d --build` 启动全部 11 个服务 |

首次运行会拉取 `maven` 和 `node` 镜像（约 500MB），后续运行仅增量编译。

### 4.2 方式二：分步构建

适合调试或单独更新某个模块：

```bash
cd /home/$USER/codeforge

# === Step 1: Maven 编译后端（容器内执行） ===
cd largemodel_rearend
docker run --rm \
  -v "$PWD":/build \
  -v codeforge_maven_cache:/root/.m2 \
  -w /build \
  maven:3.9-eclipse-temurin-21 \
  mvn clean package -DskipTests -q
cd ..

# === Step 2: npm 构建前端（容器内执行） ===
cd largemodel_frontend
docker run --rm \
  -v "$PWD":/app \
  -w /app \
  node:22-alpine \
  sh -c "npm install --silent && npm run build"
cd ..

# === Step 3: 启动所有服务 ===
docker compose up -d --build

# === Step 4: 观察启动日志 ===
docker compose logs -f
# 按 Ctrl+C 退出日志
```

### 4.3 只启动基础设施（开发调试用）

如果只需要在本地 IDE 调试后端，只启动基础设施：

```bash
docker compose up -d nacos mysql redis
# 然后在 IDE 中手动启动各 Spring Boot 服务
```

### 4.4 更新部署（代码修改后）

```bash
cd /home/$USER/codeforge

# 重新编译后端
cd largemodel_rearend
docker run --rm -v "$PWD":/build -v codeforge_maven_cache:/root/.m2 -w /build \
  maven:3.9-eclipse-temurin-21 mvn clean package -DskipTests -q
cd ..

# 重新编译前端
cd largemodel_frontend
docker run --rm -v "$PWD":/app -w /app \
  node:22-alpine sh -c "npm run build"
cd ..

# 重建并重启
docker compose up -d --build
```

---

## 五、验证服务

### 5.1 检查容器状态

```bash
docker compose ps
```

预期所有 11 个容器状态为 `Up`（Nacos/MySQL/Redis 额外显示 `healthy`）：

```
NAME                   STATUS
codeforge-nacos        Up (healthy)
codeforge-mysql        Up (healthy)
codeforge-redis        Up (healthy)
codeforge-gateway      Up
codeforge-auth         Up
codeforge-code         Up
codeforge-knowledge    Up
codeforge-agent        Up
codeforge-admin        Up
codeforge-frontend     Up
```

### 5.2 健康检查

```bash
# 网关健康检查
curl http://localhost:8080/api/health
# → {"code":200,"message":"OK"}

# 认证服务
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
# → {"code":200,"data":{"token":"eyJ..."}}

# 前端页面
curl -s http://localhost/ | head -5
# → <!DOCTYPE html>...
```

### 5.3 浏览器访问

在 VMware Ubuntu 中打开浏览器访问 `http://localhost`，或从宿主机访问 `http://<VM-IP>`（需 VMware 桥接网络模式）。

默认管理员账号：`admin` / `admin123`

---

## 六、常用管理命令

```bash
# ========== 日志 ==========
docker compose logs -f                     # 所有服务日志（实时）
docker compose logs -f code-service        # 单服务日志
docker compose logs --tail=100 code-service # 最近 100 行

# ========== 启停 ==========
docker compose stop                        # 停止（保留容器）
docker compose start                       # 启动已停止的容器
docker compose restart code-service        # 重启单个服务
docker compose down                        # 停止并删除容器
docker compose down -v                     # 停止并清除数据卷 ⚠️ 数据库重置

# ========== 重建 ==========
docker compose up -d --build               # 重新构建并启动
docker compose up -d --build code-service  # 只重建单个服务

# ========== 资源 ==========
docker stats                               # 所有容器资源使用
docker compose ps                          # 容器状态列表

# ========== 调试 ==========
docker exec -it codeforge-mysql mysql -uroot -p123456 largemodel   # 进入 MySQL
docker exec -it codeforge-redis redis-cli                           # 进入 Redis
docker exec -it codeforge-code sh                                   # 进入后端容器
docker exec -it codeforge-frontend sh                               # 进入前端容器
```

---

---

## 七、Docker Compose 服务清单

| 服务名 | 镜像 | 端口 | 内存 | 依赖 |
|--------|------|------|------|------|
| `nacos` | nacos/nacos-server:v3.1.2 | 8848, 9848 | — | — |
| `mysql` | mysql:8.0 | 3306 | — | — |
| `redis` | redis:7-alpine | 6379 | — | — |
| `gateway` | 自构建 `codeforge-gateway` | 8080 | 256m | nacos, mysql, redis |
| `auth-service` | 自构建 `codeforge-auth` | 8081 | 256m | nacos, mysql |
| `code-service` | 自构建 `codeforge-code` | 8082 | 512m | nacos, mysql, redis |
| `knowledge-service` | 自构建 `codeforge-knowledge` | 8083 | 256m | nacos, mysql |
| `agent-service` | 自构建 `codeforge-agent` | 8084 | 512m | nacos, mysql |
| `admin-service` | 自构建 `codeforge-admin` | 8085 | 256m | nacos, mysql |
| `frontend` | 自构建 `codeforge-frontend` | 80 | — | gateway |

### 数据卷

| 卷名 | 用途 | 清除命令 |
|------|------|---------|
| `nacos_data` | Nacos 配置持久化 | `docker compose down -v` |
| `mysql_data` | MySQL 持久化数据 | `docker compose down -v` |
| `uploads_data` | 上传文件（头像等） | `docker compose down -v` |
| `codeforge_maven_cache` | Maven 本地仓库缓存（加速重复编译） | `docker volume rm codeforge_maven_cache` |

---

> **相关文档**：[README.md](README.md) — 项目总览 | 技术栈 | 功能清单 | API 接口 | 数据库设计
