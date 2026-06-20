# CodeForge（代码锻造）— 大模型代码应用生成平台

> 作者：yx | 更新时间：2026-06-20（微服务重构完成）

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

- **微服务架构**：8 个独立模块（Gateway + 5 业务服务 + Common + Frontend）
- **API 网关**：Spring Cloud Gateway 统一入口，路由 + CORS + 鉴权
- **AI 驱动**：LangChain4j + DeepSeek/GLM，SSE 流式输出
- **容器化部署**：Docker Compose 一键启动 10 个服务

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
| AI 模型 | DeepSeek V4 / GLM-4.5-air | — |
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
                    │   Gateway (:8080)          │
                    │   Spring Cloud Gateway     │
                    │   CORS + 路由分发           │
                    └──────────┬───────────────┘
                               │
         ┌─────────────────────┼──────────────────┐
         │                     │                  │
  ┌──────▼──────┐  ┌──────────▼───┐  ┌──────────▼───┐
  │ Code :8082  │  │Knowledge:8083│  │  Agent :8084  │
  │ 认证+AI生成  │  │ 文档上传+检索 │  │ 工作流编排     │
  │ 项目+应用    │  │              │  │ 5 Agent链     │
  └──────┬──────┘  └──────┬───────┘  └──────┬───────┘
         │                │                  │
         └────────────────┼──────────────────┘
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
| **AI 代码生成** | ✅ | SSE 流式输出、多轮对话、代码块解析、中断续写、单文件预览编辑 |
| **工程项目** | ✅ | 多文件生成、文件树、CDN 沙箱预览、ZIP 下载、面板拖拽 |
| **AI 代码审查** | ✅ | SSE 流式审查、安全/性能/规范/最佳实践 4 维度评分 |
| **应用管理** | ✅ | CRUD、分页搜索、语言筛选、封面图、下载 |
| **知识库** | ✅ | 文档上传、全文搜索、集合管理、向量化预留 |
| **Agent 工作流** | ✅ | 5 Agent 链式编排、SSE 分阶段执行、任务追踪 |
| **监控大盘** | 🚧 | 统计卡片 + 图表占位（后端统计 API 待完善） |
| **管理后台** | ✅ | 用户管理、模型配置、系统日志 |
| **对话管理** | ✅ | MySQL 持久化、类型隔离(NATIVE/ENGINEERING) |

---

## 五、微服务模块

```
largemodel_rearend/
├── pom.xml                          # 父 POM (dependencyManagement)
├── codeforge-common/                # 共享层 — Entity/DTO/Enum/Exception/Util/AOP
├── codeforge-gateway/    :8080      # API 网关 — 路由 + CORS ✅
├── codeforge-code/       :8082      # 代码服务 — AI生成+项目+审查+对话+应用+认证 ✅
├── codeforge-knowledge/  :8083      # 知识库 — 文档CRUD/全文搜索 ✅
├── codeforge-agent/      :8084      # Agent — 工作流编排/多Agent链式执行 ✅
├── codeforge-auth/       :8081      # 认证服务 — 骨架 (待拆分)
└── codeforge-admin/      :8085      # 管理 — 骨架 (待拆分)
```

### 服务运行状态

| 模块 | 端口 | 状态 | 说明 |
|------|------|------|------|
| codeforge-gateway | 8080 | ✅ 可启动 | Spring Cloud Gateway Netty |
| codeforge-code | 8082 | ✅ 可启动 | 包含全部业务逻辑 (认证+AI+项目+应用) |
| codeforge-knowledge | 8083 | ✅ 可启动 | 知识库 API |
| codeforge-agent | 8084 | ✅ 可启动 | Agent 工作流 (需 OPENAI_API_KEY) |
| codeforge-auth | 8081 | 🚧 骨架 | 代码在 code 模块，待拆分 |
| codeforge-admin | 8085 | 🚧 骨架 | 代码在 code 模块，待拆分 |

### Gateway 路由表

| 前缀 | → 服务 | 端口 |
|------|--------|------|
| `/api/auth/**`, `/api/user/**`, `/api/ai/**`, `/api/projects/**`, `/api/applications/**`, `/api/conversations/**`, `/api/dashboard/**`, `/api/admin/**` | code-service | 8082 |
| `/api/knowledge/**` | knowledge-service | 8083 |
| `/api/agents/**` | agent-service | 8084 |

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
| POST | /api/ai/generate/stream | SSE 流式代码生成 |
| POST | /api/ai/modify/stream | SSE 流式代码修改 |
| POST | /api/ai/review | **SSE 流式代码审查** ✨ |

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
| POST | /api/applications | 保存应用 |
| GET | /api/applications | 分页列表+筛选 |
| GET | /api/applications/{id} | 详情 |
| DELETE | /api/applications/{id} | 删除 |
| GET | /api/applications/{id}/download | 下载代码 |

### 6.6 知识库 `/api/knowledge/*`（需登录）✨

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/knowledge/documents | 上传文档 |
| GET | /api/knowledge/documents | 文档列表 |
| GET | /api/knowledge/documents/{id} | 文档详情 |
| DELETE | /api/knowledge/documents/{id} | 删除文档 |
| POST | /api/knowledge/search | 语义搜索 |
| GET | /api/knowledge/stats | 统计信息 |

### 6.7 Agent `/api/agents/*`（需登录）✨

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/agents/workflow | 创建工作流 |
| GET | /api/agents/workflow | 工作流列表 |
| GET | /api/agents/workflow/{id} | 工作流详情 |
| DELETE | /api/agents/workflow/{id} | 删除 |
| POST | /api/agents/workflow/{id}/execute | **SSE 流式执行 5 Agent 链** |

### 6.8 管理 `/api/admin/*`（需 ADMIN）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/admin/users | 用户列表 |
| GET | /api/admin/users/{id} | 用户详情 |
| PUT | /api/admin/users/{id}/status | 启用/禁用 |
| PUT | /api/admin/users/{id}/role | 修改角色 |
| DELETE | /api/admin/users/{id} | 删除用户 |
| PUT | /api/admin/users/{id}/password | 重置密码 |

### 6.9 其他

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/health | 健康检查 (公开) |
| GET | /api/conversations | 对话列表 (`?type=NATIVE\|ENGINEERING`) |
| GET | /api/dashboard/stats | 仪表盘统计 |

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
│   │   ├── admin.js                   # 管理
│   │   ├── ai.js                      # AI 生成 + 修改 + 审查
│   │   ├── app.js                     # 应用 + 对话 + 仪表盘
│   │   ├── project.js                 # 工程项目
│   │   ├── knowledge.js               # 知识库 ✨
│   │   ├── agents.js                  # Agent 工作流 ✨
│   │   └── monitor.js                 # 监控 ✨
│   ├── components/
│   │   ├── CodeViewer.vue             # Monaco Editor 封装
│   │   ├── SandboxPreview.vue         # sandpack-vue3 沙箱预览
│   │   └── project/FileTree.vue       # 文件树
│   ├── layouts/
│   │   ├── AuthLayout.vue             # 登录/注册布局
│   │   └── DefaultLayout.vue          # 主布局 (侧边栏+顶栏)
│   ├── views/
│   │   ├── ai/Generate.vue            # AI 代码生成
│   │   ├── project/Create.vue         # 项目创建
│   │   ├── app/AppList.vue            # 我的应用
│   │   ├── dashboard/Index.vue        # 工作台
│   │   ├── knowledge/Index.vue        # 知识库 ✨
│   │   ├── agents/Index.vue           # Agent 工作流 ✨
│   │   ├── monitor/Index.vue          # 监控大盘 ✨
│   │   ├── admin/Users.vue            # 用户管理
│   │   ├── admin/Models.vue           # 模型配置 ✨
│   │   ├── admin/Logs.vue             # 系统日志 ✨
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

| 表名 | 说明 | 引擎 |
|------|------|------|
| `users` | 用户 (BCrypt 密码 / 角色 / 状态 / 逻辑删除) | InnoDB |
| `applications` | 应用 (类型 / 语言 / 源码 / 配置 JSON) | InnoDB |
| `conversations` | 对话会话 (type: NATIVE/ENGINEERING) | InnoDB |
| `messages` | 消息 (USER/AI 角色 / 内容 / token 数) | InnoDB |
| `knowledge_documents` | 知识库文档 (全文索引 / 向量状态) ✨ | InnoDB |
| `agent_workflows` | Agent 工作流 (Agent 链 / 状态 / 结果) ✨ | InnoDB |

### 8.2 迁移脚本

| 文件 | 说明 |
|------|------|
| `db/init.sql` | 基础表（用户/应用/对话/消息） |
| `db/migration_V1__add_conversation_type.sql` | conversations 新增 type 列 |
| `codeforge-knowledge/.../migration_V2__add_knowledge_documents.sql` | 知识库文档表 |
| `codeforge-agent/.../migration_V3__add_agent_workflows.sql` | Agent 工作流表 |

### 8.3 存储

| 类型 | 位置 |
|------|------|
| MySQL | `largemodel` 数据库 (JPA ddl-auto: validate) |
| Redis | `localhost:6379` — Session 缓存 (1h TTL) |
| 磁盘 | `uploads/avatars/` + `uploads/projects/project_{id}/` |

---

## 九、AI 集成

### 9.1 LangChain4j 配置

- **模型**：DeepSeek V4 Flash（可通过 `langchain4j.openai.*` 配置切换）
- **超时**：Streaming 300s / Non-streaming 120s
- **线程**：Virtual Threads (Java 21)

### 9.2 AI 功能矩阵

| 功能 | 端点 | Prompt 模板 | 输出 |
|------|------|------------|------|
| 单文件生成 | `/api/ai/generate/stream` (NATIVE) | `PromptTemplateService.getNativeAppSystemPrompt()` | 单个代码块 |
| 项目生成 | `/api/projects/generate` (ENGINEERING) | `PromptTemplateService.getEngineeringProjectSystemPrompt()` | `[PROJECT]` + `[FILE]` 多文件 |
| 代码修改 | `/api/ai/modify/stream` | `PromptTemplateService.getProjectModifySystemPrompt()` | 修改后多文件 |
| 代码审查 | `/api/ai/review` | `PromptTemplateService.getCodeReviewSystemPrompt()` | 4 维度评分报告 |
| Agent 执行 | `/api/agents/workflow/{id}/execute` | 5 角色链 (analyzer→architect→coder→tester→reviewer) | SSE 分阶段输出 |

### 9.3 SSE 数据流

```
浏览器                                    后端
  │                                         │
  ├─ POST /api/ai/* ──────────────────────▶│
  │  { prompt/code, type, language, ... }   │
  │                                         ├─ PromptTemplateService 构建消息
  │                                         ├─ LangChain4j StreamingChatModel
  │                                         │
  │◀── event: token ◀──────────────────────┤ 逐 token 推送
  │    data: "public"                       │
  │◀── event: token ◀──────────────────────┤
  │       ...                               │
  │◀── event: done ◀──────────────────────┤ 生成/审查完成
  │    data: {"code":"...","score":85,...}   │
  │                                         ├─ 持久化到 MySQL
```

---

## 十、部署运行

### 10.1 本地开发

```bash
# 前置条件：MySQL 8.0 + Redis 7.x 已启动

# 1. 重建数据库（首次）
#    在 MySQL 中执行:
#    SOURCE D:/Idea-program-file/largemodel/largemodel_rearend/codeforge-code/src/main/resources/db/init.sql;

# 2. 设置 JDK 21
set JAVA_HOME=D:\Java\jdk-21

# 3. 编译全部模块
cd largemodel_rearend
mvn compile -q

# 4. 启动 Gateway (:8080)、Code (:8082)、Knowledge (:8083)、Agent (:8084)
#    分别在不同终端中启动：
mvn spring-boot:run -pl codeforge-gateway
mvn spring-boot:run -pl codeforge-code
mvn spring-boot:run -pl codeforge-knowledge
mvn spring-boot:run -pl codeforge-agent

# 5. 前端
cd largemodel_frontend
npm install
npm run dev
# → http://localhost:3000 (proxy /api → localhost:8080 → Gateway → 各服务)
```

### 10.2 Docker Compose 部署

```bash
cd D:\Idea-program-file\largemodel

# 1. 配置 API Key
# 编辑 .env 文件，设置 OPENAI_API_KEY=sk-your-key

# 2. 构建 + 启动全部服务
docker compose up -d --build

# 3. 访问
# 前端: http://localhost
# API:  http://localhost:8080

# 4. 管理
docker compose logs -f          # 查看日志
docker compose down             # 停止
docker compose down -v          # 停止 + 清除数据
```

### 10.3 测试

```bash
cd largemodel_rearend
set JAVA_HOME=D:\Java\jdk-21
mvn test -pl codeforge-code -am
# → 19 tests: AuthControllerTest (13) + UserControllerTest (6)
```

### 10.4 IntelliJ IDEA 配置

- **Maven JRE**：File → Settings → Build Tools → Maven → JRE → `D:\Java\jdk-21`
- **Project SDK**：File → Project Structure → SDK → JDK 21

---

## 十一、设计规范

### 11.1 品牌

| 项目 | 值 |
|------|-----|
| 名称 | **CodeForge**（代码锻造） |
| 副标题 | AI 驱动的代码锻造平台 |
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
--font-sans: 'PingFang SC', 'Microsoft YaHei', sans-serif;
--font-mono: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
```

### 11.3 新页面开发 Checklist

```
□ 背景 var(--bg-primary)
□ 卡片 var(--bg-card) + border var(--border-color)
□ 主按钮 type="primary"（auto 使用 accent 色）
□ 颜色全部通过 CSS 变量引用，不硬编码 hex
□ 图标使用 @element-plus/icons-vue
□ hover 过渡 transition: all var(--transition) 0.2s ease
□ 响应式：小屏 <768px 侧栏折叠
```

---

## 十二、文件索引

### 12.1 快速导航表

| 功能域 | 前端入口 | 后端 Service | 数据表 |
|--------|---------|-------------|--------|
| 用户认证 | `Login.vue` `Register.vue` | `AuthService` (auth) | `users` |
| 个人信息 | `Profile.vue` | `UserService` (auth) | `users` |
| 后台管理 | `Users.vue` `Models.vue` `Logs.vue` | `AdminService` (admin) | `users` |
| AI 代码生成 | `Generate.vue` | `AiCodeGenService` (code) | `conversations` `messages` |
| AI 代码审查 | `Generate.vue` (review panel) | `ReviewController` (code) | — |
| 工程项目 | `Create.vue` | `ProjectService` + `AiCodeGenService` (code) | `conversations` + 磁盘 |
| 应用管理 | `AppList.vue` | `ApplicationService` (code) | `applications` |
| 知识库 | `knowledge/Index.vue` | `KnowledgeService` (knowledge) | `knowledge_documents` |
| Agent 工作流 | `agents/Index.vue` | `AgentService` (agent) | `agent_workflows` |
| 监控大盘 | `monitor/Index.vue` | (admin stats) | — |
| 沙箱预览 | `SandboxPreview.vue` | — | — |

### 12.2 后端关键文件

> 注：`SecurityConfig`、`JwtUtil`、`JwtAuthFilter`、`CorsConfig`、`LangChain4jConfig` 已统一放在 `codeforge-common`，所有模块共享。

| 文件 | 位置 | 职责 |
|------|------|------|
| `SecurityConfig.java` | common | Spring Security 无状态会话 + 端点白名单 |
| `JwtUtil.java` + `JwtAuthFilter.java` | common | JWT 签发 + 解析 + MyBatis-Plus 认证 |
| `LangChain4jConfig.java` | common | LLM Bean (Streaming + 普通) |
| `CorsConfig.java` | common | 全局 CORS |
| `MyBatisPlusConfig.java` | code | 分页插件 + @MapperScan |
| `AiController.java` | code | SSE 代码生成 + 修改 |
| `ReviewController.java` | code | SSE 代码审查 ✨ |
| `KnowledgeController.java` | knowledge | 知识库 CRUD ✨ |
| `AgentController.java` | agent | Agent 工作流 + SSE 执行 ✨ |
| `PromptTemplateService.java` | code | 5 套 System Prompt |
| `AiCodeGenService.java` | code | SSE 流式核心 + 多文件解析 |

---

## 十三、后续规划

> 以下功能已设计但尚未实现，按优先级排列。

### 🔴 P0 — 高优先级

| 功能 | 说明 | 涉及模块 |
|------|------|---------|
| **微服务真正拆分** | 将 auth/admin 代码从 code 模块迁移到各自服务，Gateway 路由到独立服务 | auth, admin |
| **Nacos 注册中心** | 服务注册发现 + 配置中心，替代硬编码 URL | gateway, all |
| **Sentinel 流控** | AI 接口限流保护，防止 API 配额耗尽 | gateway |
| **API Key 管理** | 后台配置模型 API Key，支持多 Key 轮换，无需重启 | admin |
| **对话导出** | 将对话记录导出为 Markdown/PDF | code |

### 🟡 P1 — 中优先级

| 功能 | 说明 | 涉及模块 |
|------|------|---------|
| **RAG 向量检索** | 接入 Milvus/Chroma，文档 Embedding + 语义相似度搜索 | knowledge |
| **多模型支持** | 同时支持 DeepSeek / GLM / Qwen / GPT，前端可选 | code, admin |
| **代码审查自动化** | 保存审查历史、对比修复前后代码、审查报告导出 | code |
| **项目版本管理** | Git 集成，项目文件版本回滚 | code |
| **单元测试生成** | AI 自动生成 JUnit/Vitest 测试代码 | agent |
| **国际化 (i18n)** | 中英文切换，vue-i18n 集成 | frontend |

### 🟢 P2 — 低优先级

| 功能 | 说明 | 涉及模块 |
|------|------|---------|
| **监控后端实现** | Prometheus 指标采集 + Grafana 可视化，替代前端占位图表 | admin, frontend |
| **Prometheus 集成** | Actuator metrics 暴露，JVM/API/DB 监控 | all |
| **CI/CD 流水线** | GitHub Actions 自动构建 + 测试 + Docker 推送 | devops |
| **协作功能** | 项目分享、团队空间、评论讨论 | auth, code |
| **移动端适配** | PWA 支持，移动端响应式优化 | frontend |
| **WebSocket 推送** | 对话列表实时更新，替代轮询 | gateway, code |
| **暗色/浅色主题切换** | CSS 变量扩展 `html.light`，前端主题切换按钮 | frontend |

### 🔵 技术债

| 项目 | 说明 |
|------|------|
| JPA → MyBatis-Plus 完全迁移 | 移除 JPA 依赖，Repository 全部替换为 Mapper |
| JWT 黑名单 | Redis 存储已注销 Token，防止被盗用 |
| 接口幂等性 | 关键写操作防重复提交 |
| 日志收集 | ELK/Loki 集中日志管理 |
| 压力测试 | JMeter/Gatling 对 AI 接口进行负载测试 |

---

> **相关文档**：`CodeForge-项目计划.md` — 完整项目计划与功能规划
