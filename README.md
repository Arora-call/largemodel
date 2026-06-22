# CodeForge（代码锻造）— 大模型代码应用生成平台

> 作者：yx | 更新时间：2026-06-22（测试修复 + 配置规范化，19 个集成测试全部通过）

---

## 目录

- [一、项目概述](#一项目概述)
- [二、技术栈](#二技术栈)
- [三、架构总览](#三架构总览)
- [四、功能清单](#四功能清单)
- [五、微服务模块](#五微服务模块)
- [六、API 接口清单](#六api-接口清单)
- [七、前端架构](#七前端架构)
- [八、数据库设计](#八数据库设计)
- [九、AI 集成](#九ai-集成)
- [十、部署运行](#十部署运行)
- [十一、设计规范](#十一设计规范)
- [十二、文件索引](#十二文件索引)
- [十三、后续规划](#十三后续规划)

---

## 一、项目概述

**CodeForge** 是面向高校实训与企业开发者的 **AI 驱动代码生成与管理平台**，支持从自然语言描述到完整可运行项目的全流程自动化。

### 核心价值

| 角色 | 能力 |
|------|------|
| **学生/开发者** | AI 对话生成代码、创建完整工程项目、代码审查、实时预览编辑 |
| **教师/管理者** | 统一实训环境、用户管理、模型配置、调用统计 |
| **平台运营** | 展示微服务 + AI + DevOps 全栈技术能力 |

### 架构特点

- **微服务架构**：7 个独立模块（Gateway + 5 业务服务 + Common）
- **API 网关**：Spring Cloud Gateway 统一入口，路由 + CORS + 限流
- **AI 驱动**：LangChain4j + 多模型动态切换，SSE 流式输出
- **安全防护**：AES-256-GCM 加密存储 API Key，接口限流防滥用
- **容器化部署**：Docker Compose 一键启动 10 个服务

### 2026-06-22 更新

- 🧪 **测试修复**：19 个集成测试修复并通过，测试从 codeforge-code 迁至 codeforge-auth（与实际 Controller 同模块）
- 🔧 **配置修复**：修复 `LangChain4jConfig` 测试环境下因 `OPENAI_API_KEY` 缺失导致上下文加载失败的问题
- 📐 **YAML 规范化**：`application.yml` 中 Redis 配置从 `langchain4j` 下移至正确的 `spring.data.redis` 位置
- 🧹 **代码清理**：删除 `codeforge-code` 中重复的 `UserMapper.java`（统一使用 `codeforge-common` 中的版本）
- 📦 **扫描范围**：`AuthApplication.scanBasePackages` 扩展为 `org.example`，确保 `GlobalExceptionHandler` 等组件被加载

### 2026-06-21 更新

- 🏗️ **微服务拆分**：Auth (8081) + Admin (8085) + Code (8082) + Gateway (8080) 独立运行
- 🔐 **API Key 管理**：AES-256 加密存储，多模型动态切换，在线测试连接，密钥持久化
- 🛡️ **接口限流**：AI 5 QPS / 登录 20 QPS，超限返回 429
- 📄 **对话导出**：一键导出 Markdown，支持单对话删除
- 📊 **监控大盘**：ECharts 实时图表，调用量/Token/耗时/模型占比，首 Token 响应时间
- 📋 **系统日志**：AOP 自动记录管理员操作，支持筛选搜索
- 🤖 **模型选择**：AI 代码生成 + 项目生成 双页面模型切换，选择自动持久化
- 🔧 **UX 修复**：语言识别优化、预览错误去重、模型默认标识、对话切换消息保持

---

## 二、技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.5.4 |
| 微服务 | Spring Cloud Gateway | 2025.0.0 |
| JDK | Java | 21 (LTS) |
| ORM | JPA + MyBatis-Plus | 3.5.9 (并行过渡) |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x (Lettuce) |
| 安全 | Spring Security + JWT (jjwt) | 0.12.6 |
| AI 框架 | LangChain4j | 1.0.0-beta2 |
| AI 模型 | DeepSeek / GLM / Qwen / GPT | — |
| API 文档 | Springdoc OpenAPI (Swagger) | 2.7.0 |
| 前端框架 | Vue 3 (Composition API) | 3.5.x |
| UI 库 | Element Plus | 2.14.x |
| 代码编辑器 | Monaco Editor | 0.52.x |
| 沙箱预览 | sandpack-vue3 (Sandpack + CDN) | 3.1.x |
| 图表 | ECharts | 5.6.x |
| Markdown | markdown-it | 14.1.x |
| 状态管理 | Pinia | 3.x |
| 构建工具 | Vite | 8.x |
| 容器化 | Docker + Compose | — |

---

## 三、架构总览

```
                    ┌──────────────────────────┐
                    │   Vite Dev Server :3000   │
                    │   前端 SPA                 │
                    └──────────┬───────────────┘
                               │ proxy /api → :8080
                    ┌──────────▼───────────────┐
                    │   Gateway (:8080)         │
                    │   Spring Cloud Gateway    │
                    │   路由 + 限流 + CORS       │
                    └──────────┬───────────────┘
                               │
     ┌──────────┬──────────────┼──────────────┬──────────┐
     │          │              │              │          │
  ┌──▼───┐ ┌───▼────┐  ┌─────▼──────┐ ┌────▼───┐ ┌───▼───┐
  │Auth   │ │Code    │  │Knowledge   │ │Agent   │ │Admin  │
  │:8081  │ │:8082   │  │:8083       │ │:8084   │ │:8085  │
  │认证   │ │AI+项目 │  │文档+检索    │ │工作流  │ │管理   │
  └──┬───┘ └───┬────┘  └─────┬──────┘ └───┬────┘ └───┬───┘
     │         │              │             │          │
     └─────────┴──────────────┴─────────────┴──────────┘
                               │
                    ┌──────────▼───────────┐
                    │   MySQL :3306         │
                    │   Redis :6379         │
                    └──────────────────────┘
```

---

## 四、功能清单

| 模块 | 状态 | 功能点 |
|------|------|--------|
| **用户体系** | ✅ | 注册/登录、JWT 认证、角色权限、找回密码、头像上传、账号切换 |
| **AI 代码生成** | ✅ | SSE 流式输出、多轮对话、代码块解析、中断续写、手动保存 |
| **API Key 管理** | ✅ | AES-256 加密存储、多模型动态切换、在线测试连接 |
| **工程项目** | ✅ | 多文件生成、文件树、CDN 沙箱预览、ZIP 下载、面板拖拽 |
| **AI 代码审查** | ✅ | SSE 流式审查、4 维度评分（安全/性能/规范/最佳实践） |
| **对话导出** | ✅ | 一键导出 Markdown、单对话删除 |
| **接口限流** | ✅ | AI 5 QPS、登录 20 QPS，超限返回 429 |
| **应用管理** | ✅ | CRUD、重命名、分页搜索、语言筛选、封面图、下载 |
| **知识库** | ✅ | 文档上传、全文搜索、集合管理、向量化预留 |
| **Agent 工作流** | ✅ | 5 Agent 链式编排、SSE 分阶段执行、任务追踪 |
| **仪表盘** | ✅ | 项目数 + 对话数 + 角色 + 用户数（管理员） |
| **管理后台** | ✅ | 用户管理、模型配置（含测试连接）、系统日志 |
| **对话管理** | ✅ | MySQL 持久化、类型隔离 (NATIVE/ENGINEERING) |
| **监控大盘** | ✅ | ECharts 实时图表、调用量/Token/耗时/模型占比、TTFB 响应时间 |

---

## 五、微服务模块

```
largemodel_rearend/
├── pom.xml                          # 父 POM (dependencyManagement)
├── codeforge-common/                # 共享层 — Entity/DTO/Enum/Exception/Util/Config/Repository
├── codeforge-gateway/    :8080      # API 网关 — 路由 + 限流 + CORS
├── codeforge-auth/       :8081      # 认证服务 — 注册/登录/JWT/个人信息
├── codeforge-code/       :8082      # 代码服务 — AI生成/项目/审查/对话/应用/Dashboard
├── codeforge-knowledge/  :8083      # 知识库 — 文档CRUD/全文搜索 (可选启动)
├── codeforge-agent/      :8084      # Agent — 工作流编排/5 Agent 链 (需 API Key)
└── codeforge-admin/      :8085      # 管理 — 用户管理/模型配置
```

### 服务依赖

| 模块 | 依赖 |
|------|------|
| codeforge-common | JPA + MyBatis-Plus + Validation + Security + AOP + JWT + LangChain4j |
| codeforge-gateway | Spring Cloud Gateway |
| codeforge-auth | Common + Web + Security + MySQL + JPA + H2 (test) |
| codeforge-code | Common + Web + Security + MySQL + Redis |
| codeforge-knowledge | Common + Web + Security + MySQL |
| codeforge-agent | Common + Web + Security + MySQL |
| codeforge-admin | Common + Web + Security + MySQL + JPA |

### Gateway 路由表

| 前缀 | → 服务 | 端口 |
|------|--------|------|
| `/api/auth/**`, `/api/user/**` | auth-service | 8081 |
| `/api/ai/**`, `/api/projects/**`, `/api/applications/**`, `/api/conversations/**`, `/api/dashboard/**` | code-service | 8082 |
| `/api/knowledge/**` | knowledge-service | 8083 |
| `/api/agents/**` | agent-service | 8084 |
| `/api/admin/**` | admin-service | 8085 |

### 限流规则

| 端点 | QPS | 说明 |
|------|-----|------|
| `/api/ai/generate/stream` | 5 | 防止 API 配额耗尽 |
| `/api/ai/modify/stream` | 10 | |
| `/api/ai/review` | 10 | |
| `/api/projects/generate` | 5 | |
| `/api/auth/login` | 20 | 防暴力破解 |

---

## 六、API 接口清单

### 6.1 认证 `/api/auth/*`（公开）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册 |
| POST | /api/auth/login | 登录 |
| GET | /api/auth/me | 当前用户 |
| POST | /api/auth/forgot-password | 找回密码 |

### 6.2 用户 `/api/user/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/user/info | 个人信息 |
| PUT | /api/user/info | 修改信息 |
| PUT | /api/user/password | 修改密码 |
| DELETE | /api/user/account | 注销账户 |
| POST | /api/user/avatar | 上传头像 |

### 6.3 AI `/api/ai/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/ai/generate/stream | SSE 流式代码生成 (支持 modelId 切换模型) |
| POST | /api/ai/modify/stream | SSE 流式代码修改 |
| POST | /api/ai/review | SSE 流式代码审查 |

### 6.4 项目 `/api/projects/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/projects/generate | SSE 流式项目生成 |
| GET | /api/projects/{id}/tree | 文件树 |
| GET | /api/projects/{id}/file?path= | 读取文件 |
| GET | /api/projects/{id}/download | 下载 ZIP |
| POST | /api/projects/{id}/save | 保存到应用库 |

### 6.5 应用 `/api/applications/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/applications | 保存/更新（传 id 则更新） |
| GET | /api/applications | 分页 + 关键词 + 语言筛选 |
| GET | /api/applications/{id} | 详情 |
| DELETE | /api/applications/{id} | 删除 |
| GET | /api/applications/{id}/download | 下载代码 (blob + JWT) |

### 6.6 知识库 `/api/knowledge/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/knowledge/documents | 上传文档 |
| GET | /api/knowledge/documents | 文档列表 |
| GET | /api/knowledge/documents/{id} | 详情 |
| DELETE | /api/knowledge/documents/{id} | 删除 |
| POST | /api/knowledge/search | 语义搜索 |
| GET | /api/knowledge/stats | 统计 |

### 6.7 Agent `/api/agents/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/agents/workflow | 创建工作流 |
| GET | /api/agents/workflow | 列表 |
| GET | /api/agents/workflow/{id} | 详情 |
| DELETE | /api/agents/workflow/{id} | 删除 |
| POST | /api/agents/workflow/{id}/execute | SSE 流式执行 5 Agent 链 |

### 6.8 管理 `/api/admin/*`（需 ADMIN）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/admin/users | 用户列表 |
| GET | /api/admin/users/{id} | 详情 |
| PUT | /api/admin/users/{id}/status | 启用/禁用 |
| PUT | /api/admin/users/{id}/role | 修改角色 |
| DELETE | /api/admin/users/{id} | 删除 |
| PUT | /api/admin/users/{id}/password | 重置密码 |
| GET | /api/admin/models | 模型列表 |
| POST | /api/admin/models | 添加模型（API Key 加密） |
| PUT | /api/admin/models/{id} | 更新模型 |
| DELETE | /api/admin/models/{id} | 删除模型 |
| POST | /api/admin/models/{id}/test | 测试连接 |
| GET | /api/admin/models/enabled | 已启用列表（前端用） |

### 6.9 其他

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/health | 健康检查 (公开) |
| GET | /api/conversations | 对话列表 (`?type=NATIVE\|ENGINEERING`) |
| GET | /api/conversations/{id}/messages | 对话消息 |
| GET | /api/conversations/{id}/export | 导出 Markdown |
| DELETE | /api/conversations/{id} | 删除单对话 |
| GET | /api/dashboard/stats | 仪表盘统计（含对话次数） |

---

## 七、前端架构

```
largemodel_frontend/
├── index.html                         # CodeForge SPA 入口
├── Dockerfile                         # 多阶段构建 (Node → Nginx)
├── nginx.conf                         # SPA 路由 + API 代理 + SSE
├── src/
│   ├── api/
│   │   ├── request.js                 # Axios 实例 + Token 拦截器
│   │   ├── auth.js / user.js          # 认证 + 用户
│   │   ├── admin.js                   # 管理 + 模型配置
│   │   ├── ai.js                      # AI 生成 + 修改 + 审查
│   │   ├── app.js                     # 应用 + 对话 + 仪表盘 + 导出
│   │   ├── project.js                 # 工程项目
│   │   ├── knowledge.js               # 知识库
│   │   ├── agents.js                  # Agent 工作流
│   │   └── monitor.js                 # 监控
│   ├── components/
│   │   ├── CodeViewer.vue             # Monaco Editor 封装
│   │   ├── SandboxPreview.vue         # sandpack-vue3 沙箱预览
│   │   └── project/FileTree.vue       # 文件树
│   ├── layouts/
│   │   ├── AuthLayout.vue             # 登录/注册布局
│   │   └── DefaultLayout.vue          # 主布局 (侧边栏+顶栏)
│   ├── views/
│   │   ├── ai/Generate.vue            # AI 代码生成（含模型选择器）
│   │   ├── project/Create.vue         # 项目创建
│   │   ├── app/AppList.vue            # 我的应用（含重命名）
│   │   ├── dashboard/Index.vue        # 工作台
│   │   ├── knowledge/Index.vue        # 知识库
│   │   ├── agents/Index.vue           # Agent 工作流
│   │   ├── monitor/Index.vue          # 监控大盘
│   │   ├── admin/Users.vue            # 用户管理
│   │   ├── admin/Models.vue           # 模型配置（完整 CRUD）
│   │   ├── admin/Logs.vue             # 系统日志
│   │   ├── user/Profile.vue           # 个人中心
│   │   └── auth/{Login,Register,ForgotPassword}.vue
│   └── stores/auth.js                 # Pinia 状态管理
```

### 路由表

| 路径 | 页面 | 权限 |
|------|------|------|
| `/auth/login` | 登录 | guest |
| `/auth/register` | 注册 | guest |
| `/dashboard` | 工作台 | auth |
| `/ai/generate` | AI 代码生成 | auth |
| `/project/create` | 创建项目 | auth |
| `/app/list` | 我的应用 | auth |
| `/knowledge` | 知识库 | auth |
| `/agents` | Agent 工作流 | auth |
| `/monitor` | 监控大盘 | auth |
| `/user/profile` | 个人中心 | auth |
| `/admin/users` | 用户管理 | admin |
| `/admin/models` | 模型配置 | admin |
| `/admin/logs` | 系统日志 | admin |

---

## 八、数据库设计

### 8.1 数据表

| 表名 | 说明 |
|------|------|
| `users` | 用户 (BCrypt 密码 / 角色 / 状态 / 逻辑删除) |
| `applications` | 应用 (类型 / 语言 / 源码 / 配置 JSON) |
| `conversations` | 对话会话 (type: NATIVE/ENGINEERING) |
| `messages` | 消息 (USER/AI 角色 / 内容 / token 数) |
| `knowledge_documents` | 知识库文档 (全文索引 / 向量状态) |
| `agent_workflows` | Agent 工作流 (Agent 链 / 状态 / 结果) |
| `model_configs` | AI 模型配置 (AES-256 加密 API Key) |
| `api_call_logs` | API 调用日志（监控大盘统计） |
| `operation_logs` | 操作日志（管理员审计） |

### 8.2 安全性

- **API Key**：`model_configs.api_key_encrypted` 使用 AES-256-GCM 加密，密钥从 JWT secret 派生
- **密码**：BCrypt 加密存储
- **逻辑删除**：`users.deleted` + `applications.status` + `conversations.status`

---

## 九、AI 集成

### 9.1 模型管理

通过 `DynamicModelProvider` 实现运行时多模型切换：

```
请求携带 modelId
      ↓
DynamicModelProvider.getStreaming(modelId)
      ↓
查找 model_configs → AES 解密 API Key → 创建 StreamingChatModel
      ↓
缓存实例（按 modelId），配置变更后 Evict
```

支持的模型：DeepSeek / OpenAI / 智谱 GLM / 任何 OpenAI 兼容 API

### 9.2 AI 功能

| 功能 | 端点 | Prompt 模板 |
|------|------|------------|
| 单文件生成 | `/api/ai/generate/stream` (NATIVE) | 语言专用 System Prompt |
| 项目生成 | `/api/projects/generate` (ENGINEERING) | 多文件标记格式 |
| 代码修改 | `/api/ai/modify/stream` | 元素拾取 + 上下文 |
| 代码审查 | `/api/ai/review` | 4 维度评分报告 |
| Agent 执行 | `/api/agents/workflow/{id}/execute` | 5 角色链式编排 |

---

## 十、部署运行

### 10.1 本地开发

```bash
# 前置：MySQL 8 + Redis 7 已启动

# 1. 重建数据库
#    MySQL 中执行: SOURCE .../codeforge-code/src/main/resources/db/init.sql;

# 2. 编译
set JAVA_HOME=D:\Java\jdk-21
cd largemodel_rearend
mvn compile -q

# 3. 启动服务（5 个终端）
mvn spring-boot:run -pl codeforge-gateway     # :8080
mvn spring-boot:run -pl codeforge-auth        # :8081
mvn spring-boot:run -pl codeforge-code        # :8082
mvn spring-boot:run -pl codeforge-agent       # :8084
mvn spring-boot:run -pl codeforge-admin       # :8085
# knowledge :8083 可选

# 4. 前端
cd largemodel_frontend
npm install && npm run dev
# → http://localhost:3000
```

### 10.2 Docker 部署

```bash
cd D:\Idea-program-file\largemodel
# 编辑 .env 设置 OPENAI_API_KEY
docker compose up -d --build
# → http://localhost
```

> 详细部署步骤（Ubuntu/VMware 环境、分步构建、故障排查等）见 **[Deployment.md](Deployment.md)**。

### 10.3 测试

```bash
cd largemodel_rearend
set JAVA_HOME=D:\Java\jdk-21
mvn test -pl codeforge-auth -am
# → 19 tests: AuthControllerTest (13) + UserControllerTest (6)
```

测试使用 **H2 内存数据库**（MySQL 兼容模式），无需启动真实 MySQL/Redis。测试覆盖：

| 测试类 | 用例数 | 覆盖场景 |
|--------|--------|---------|
| `AuthControllerTest` | 13 | 注册（成功/重复/缺字段/短用户名）、登录（成功/密码错/用户不存在）、当前用户（有Token/无Token/无效Token）、找回密码（成功/邮箱错/用户不存在） |
| `UserControllerTest` | 6 | 获取用户信息（已认证/无Token）、更新信息（全字段/部分更新）、修改密码（成功/原密码错误） |

---

## 十一、设计规范

### 11.1 品牌

| 项目 | 值 |
|------|-----|
| 名称 | **CodeForge**（代码锻造） |
| 风格 | 深色科技风（Dark Tech） |

### 11.2 Design Tokens

```css
--accent: #7c8aff;           /* 靛蓝紫品牌色 */
--bg-primary: #0d1117;       /* 页面底 */
--bg-card: #141821;          /* 卡片 */
--bg-code: #1a1e2a;          /* 代码区 */
--text-primary: #c9d1d9;     /* 正文 */
--text-secondary: #8b949e;   /* 次要 */
--text-dim: #6b7280;         /* 辅助 */
```

---

## 十二、文件索引

### 12.1 快速导航

| 功能域 | 前端入口 | 后端模块 | 数据表 |
|--------|---------|---------|--------|
| 用户认证 | `Login.vue` | auth :8081 | `users` |
| 个人信息 | `Profile.vue` | auth :8081 | `users` |
| 后台管理 | `Users.vue` `Models.vue` | admin :8085 | `users` `model_configs` |
| AI 代码生成 | `Generate.vue` | code :8082 | `conversations` `messages` |
| AI 代码审查 | `Generate.vue` | code :8082 | — |
| 工程项目 | `Create.vue` | code :8082 | `conversations` + 磁盘 |
| 应用管理 | `AppList.vue` | code :8082 | `applications` |
| 知识库 | `knowledge/Index.vue` | knowledge :8083 | `knowledge_documents` |
| Agent 工作流 | `agents/Index.vue` | agent :8084 | `agent_workflows` |
| 监控 | `monitor/Index.vue` | — | — |

### 12.2 后端关键文件

| 文件 | 位置 | 职责 |
|------|------|------|
| `SecurityConfig.java` | common | Spring Security 无状态 + 白名单 |
| `JwtUtil.java` + `JwtAuthFilter.java` | common | JWT 签发 + 认证 (MyBatis-Plus) |
| `LangChain4jConfig.java` | common | LLM Bean (条件加载) |
| `DynamicModelProvider.java` | common | 动态模型工厂 (DB/fallback) |
| `RateLimitFilter.java` | gateway | 滑动窗口限流 |
| `AesUtil.java` | common | AES-256-GCM 加密 |
| `PromptTemplateService.java` | code | 5 套 System Prompt |
| `AiCodeGenService.java` | code | SSE 流式核心 |

---

## 十三、后续规划

### 🔴 P0 — 高优先级

| 功能 | 状态 | 说明 |
|------|------|------|
| 微服务真正拆分 | ✅ | Auth/Admin 独立，Gateway 5 路由 |
| Sentinel 流控 | ✅ | Gateway 全局过滤器 |
| API Key 管理 | ✅ | AES-256 加密 + 动态切换 + 测试连接 |
| 对话导出 | ✅ | 导出 Markdown + 单对话删除 |
| **Nacos 注册中心** | ⬜ | 服务注册发现 + 配置中心 |

### 🟡 P1 — 中优先级

| 功能 | 状态 | 说明 |
|------|------|------|
| 多模型支持 | ✅ | DeepSeek/GLM/Qwen/GPT 切换 |
| RAG 向量检索 | ⬜ | 接入 Milvus/Chroma |
| 代码审查自动化 | ⬜ | 保存审查历史 + 报告导出 |
| 单元测试生成 | ⬜ | AI 自动生成 JUnit/Vitest |
| 国际化 (i18n) | ⬜ | vue-i18n 中英文 |

### 🟢 P2 — 低优先级

| 功能 | 说明 |
|------|------|
| Prometheus + Grafana | 监控指标采集 + 可视化 |
| CI/CD 流水线 | GitHub Actions 自动构建 |
| 协作功能 | 项目分享 + 团队空间 |
| 移动端适配 | PWA + 响应式 |
| WebSocket 推送 | 对话列表实时更新 |
| 暗色/浅色主题切换 | CSS 变量扩展 |

### 🔵 技术债

| 项目 | 说明 |
|------|------|
| JPA → MyBatis-Plus 完全迁移 | 移除 JPA 依赖 |
| JWT 黑名单 | Redis 存储已注销 Token |
| 接口幂等性 | 关键写操作防重复提交 |
| 日志收集 | ELK/Loki 集中管理 |
| 压力测试 | JMeter 负载测试 |

---

> **相关文档**：`CodeForge-项目计划.md` — 完整项目计划与功能规划
