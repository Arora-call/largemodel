# CodeForge（代码锻造）— 大模型代码应用生成平台

> 作者：yx | 更新时间：2026-06-25（可视化元素编辑 + 知识库 RAG + 文件级修改 + 管理端应用管理）

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
- [十、Nginx 部署预览](#十nginx-部署预览)
- [十一、部署运行](#十一部署运行)
- [十二、设计规范](#十二设计规范)
- [十三、文件索引](#十三文件索引)
- [十四、后续规划](#十四后续规划)

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

### 2026-06-25 更新（第五批）

- 🎯 **可视化元素编辑**：预览界面点击元素 → 识别标签/类名/CSS选择器 → AI 精准修改指定文件。支持单文件/多文件/Vue项目（跨域 iframe postMessage 通信 + 部署时注入选取脚本）
- 📝 **文件级修改**：`FileModificationService` — AI 只输出修改片段，后端自动合并回完整原始文件，保留未修改文件。修改后预览自动刷新（`:key` 强制 iframe 重建）
- 📚 **知识库 RAG 集成**：`KnowledgeContextService` — 用户可手动选择知识库文档或开启自动检索，生成时将相关内容注入 AI Prompt。`GenerateCodeRequest` 新增 `knowledgeDocIds` + `autoSearchKnowledge` 字段
- 🛡️ **管理端应用管理**：侧栏新增「应用管理」（仅管理员可见），查看/编辑/删除所有用户应用
- 📌 **应用置顶/精选**：修改 `applications.priority`（0-默认/99-精选/999-置顶），卡片按优先级排序
- 📊 **监控用户隔离**：`api_call_logs` 修复 `user_id` 记录，新增 `ByUser` 查询方法。`/api/monitor/*` 端点用户只看自己数据，管理员通过 `/api/admin/stats/*` 看全局
- 📋 **系统日志增强**：AOP 覆盖注册/登录/找回密码/应用管理操作，修复 `LogAspect` 未加载问题
- 🐛 **关键修复**：对话删除级联清理、模型配置端点权限粒化、单文件修改后刷新加载失败（JSON 解析 CSS 花括号报错）、多文件修改后文件丢失（合并保留未修改文件）、Admin/Auth 模块 `scanBasePackages` 过窄

### 2026-06-25 更新（第四批）

- 🏗️ **Vue 项目构建**：新增 `VueProjectBuilder`，AI 生成源码后自动执行 `npm install` + `npm run build`，产出 `dist/` 目录
- 🚀 **Nginx dist 部署**：Vue 项目部署时使用构建后的 `dist/` 产物，CSS/JS/路由完整可用，替代不稳定的 Sandpack 沙箱
- 🔄 **双预览策略**：Vue 生成完 → Sandpack 即时预览 → 后台构建 → Nginx 就绪后自动切换 iframe
- ⏱️ **异步构建**：Java 21 虚拟线程异步执行，不阻塞 SSE 响应。部署时 `waitForDist()` 最多等 120s
- 🖥️ **OS 适配**：`VueProjectBuilder` 自动检测 Windows/Linux，Windows 下 npm 命令加 `.cmd` 后缀

### 2026-06-25 更新（第三批）

- 📚 **知识库上线**：文档上传（PDF/TXT/Markdown）、PDFBox 解析、内容分块、全文搜索、语义搜索、集合管理、下载
- 🤖 **Agent 工作流上线**：5 Agent 链式编排（analyzer → architect → coder → tester → reviewer），SSE 流式执行，实时进度可视化
- 📝 **Agent 提示词外部化**：5 个角色提示词移至 `prompt/agent-*.txt`，修改无需重新编译
- 🔧 **Agent 执行优化**：`CountDownLatch` 替代忙等轮询，每 Agent 3 分钟超时保护
- 📥 **结果导出**：Agent 执行结果支持一键导出 Markdown 文件
- 📐 **页面居中**：知识库、Agent 工作流、监控大盘统一居中布局（`max-width` + `margin: auto`）
- 🧹 **代码清理**：移除 `DeployService` / `ApplicationService` / `KnowledgeService` 中未用方法和常量

### 2026-06-25 更新（第二批）

- 🏷️ **应用保存重构**：保存应用不再存储 `sourceCode`，改为保存对话链接（`conversation_id`），解决代码过长被截断问题
- 🎴 **卡片部署**：每个应用卡片新增 🚀 部署按钮，部署成功后在卡片内联显示 Nginx URL（可复制/打开）
- 🔗 **点击跳转**：点击应用卡片 → 跳转 AI 工作台 `?appId=xxx`，自动加载关联对话 + 完整文件树 + 预览
- 🔍 **类型筛选**：我的应用筛选从语言（Vue/Java/HTML...）改为生成类型（单文件/多文件/Vue3项目）
- 🖼️ **封面图**：自动使用 `picsum.photos` 生成封面图片
- ✏️ **PUT 更新端点**：新增 `PUT /api/applications/{id}` 支持更名/描述/封面修改
- 📂 **侧栏默认收起**：侧边栏默认折叠，释放工作空间
- 🧹 **顶栏简化**：移除伪搜索框，保留用户下拉菜单
- 📝 **详情简化**：应用详情弹窗移除文件树/代码查看，改为「打开编辑」跳转 Workspace
- ⚡ **代码简化**：`DeployService` 合并重复方法，`ApplicationService` 删除 `buildConfig()` 等废弃逻辑
- 🔧 **下载优化**：下载优先从 `project_files` 打包独立文件 ZIP

### 2026-06-25 更新（第一批）

- 🏗️ **AI 工作台重构**：合并 Generate.vue + Create.vue → `CodeGenWorkspace.vue`，统一三栏布局（对话列表 + 聊天 + 预览）
- 🎯 **三种生成模式**：单文件 (SINGLE_FILE) / 多文件 (MULTI_FILE) / Vue3 项目 (VUE_PROJECT)，采用 Executor + Strategy + Template Method 设计模式
- 📋 **JSON 结构化输出**：Prompt 引导 AI 输出 JSON 格式，`CodeGenJsonParser` 括号深度追踪提取，正则兜底兼容
- 🗂️ **项目文件表**：新增 `project_files` 表追踪每个对话的文件（路径 + 内容 + 大小），DB + 磁盘双写存储
- 🚀 **Nginx 部署预览**：deployKey 6 位唯一标识，`DeployService` 将文件复制到 Nginx 目录，iframe 加载真实 URL
- 📝 **Prompt 外部化**：System Prompt 移至 `resources/prompt/*.txt` 资源文件，修改无需重新编译
- 🔧 **焦点收窄**：移除 Python/Java 后端代码生成，专注前端代码（HTML/CSS/JS/Vue）
- 🔄 **SSE JSON 包裹**：后端 SSE 数据统一 `{"d":"..."}` 格式，前端 `unwrapJsonD()` 解包
- 🛠️ **部署 API**：`POST /api/codegen/deploy` + `POST /api/codegen/deploy-by-app`，支持对话和应用双入口

### 2026-06-23 更新

- 🔗 **Nacos 接入**：全服务接入 Nacos 3.1.2 注册中心（:8848），支持服务发现 + 配置管理
- 📋 **配置中心**：5 业务服务集成 Nacos Config，支持 `spring.config.import` 动态拉取配置
- 🚪 **Gateway 注册**：网关接入 Nacos Discovery，为后续 `lb://` 负载均衡路由做准备
- 🔇 **日志修复**：禁用 Nacos 内置 Logback 配置（`nacos.logging.default.config.enabled=false`），消除 CONFIG_LOG_FILE Appender 冲突
- 🔧 **服务名补全**：`codeforge-code` 补充缺失的 `spring.application.name: codeforge-code`

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
| 注册配置中心 | Nacos Server | 3.1.2 |
| 服务治理 | Spring Cloud Alibaba | 2023.0.3.2 |
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
                    │   Nginx :80 (生产)        │
                    │   前端 SPA + 部署应用      │
                    │   Vite :5173 (开发)        │
                    └──────────┬───────────────┘
                               │ proxy /api → :8080
                               │ /<deployKey>/ → 静态文件
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
  │认证   │ │AI+部署 │  │文档+检索    │ │工作流  │ │管理   │
  └──┬───┘ └───┬────┘  └─────┬──────┘ └───┬────┘ └───┬───┘
     │         │              │             │          │
     └─────────┴──────────────┴─────────────┴──────────┘
                               │
                    ┌──────────▼───────────┐
                    │   Nacos :8848         │
                    │   注册中心 + 配置中心   │
                    └──────────────────────┘
                    ┌──────────▼───────────┐
                    │   MySQL :3306         │
                    │   Redis :6379         │
                    └──────────────────────┘
                    ┌──────────▼───────────┐
                    │ tmp/code_deploy/      │
                    │   磁盘部署文件 (Nginx) │
                    └──────────────────────┘
```

---

## 四、功能清单

| 模块 | 状态 | 功能点 |
|------|------|--------|
| **用户体系** | ✅ | 注册/登录、JWT 认证、角色权限、找回密码、头像上传、账号切换 |
| **AI 工作台** | ✅ | 统一三栏布局、三种生成模式（单文件/多文件/Vue3）、流式输出、文件树、Nginx 部署预览、可视化元素选取编辑、知识库 RAG 注入 |
| **API Key 管理** | ✅ | AES-256 加密存储、多模型动态切换、在线测试连接 |
| **工程项目** | ✅ | 多文件生成、文件树、Sandpack 即时预览、npm 构建 Vue 项目、dist/ Nginx 部署、ZIP 下载 |
| **AI 代码审查** | ✅ | SSE 流式审查、4 维度评分（安全/性能/规范/最佳实践） |
| **对话导出** | ✅ | 一键导出 Markdown、单对话删除、级联清理 |
| **接口限流** | ✅ | AI 5 QPS、登录 20 QPS，超限返回 429 |
| **应用管理** | ✅ | CRUD、重命名、类型筛选、封面图、下载、卡片部署、置顶/精选、点击跳转Workspace、管理员全局管理 |
| **Nginx 部署** | ✅ | deployKey 机制、共享卷、SSE 直通、SPA 路由、Gzip 压缩 |
| **知识库** | ✅ | 文档上传（PDF/TXT/MD）、全文搜索、语义搜索、集合管理、下载、RAG 注入 AI Prompt |
| **Agent 工作流** | ✅ | 5 Agent 链式编排（analyzer→architect→coder→tester→reviewer）、SSE 实时进度、Markdown 导出 |
| **仪表盘** | ✅ | 项目数 + 对话数 + 角色 + 用户数（管理员） |
| **管理后台** | ✅ | 用户管理、模型配置（含测试连接）、系统日志 |
| **对话管理** | ✅ | MySQL 持久化、类型隔离 (NATIVE/ENGINEERING) |
| **监控大盘** | ✅ | ECharts 实时图表、调用量/Token/耗时/模型占比、用户数据隔离（普通用户/管理员） |

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
| codeforge-gateway | Spring Cloud Gateway + Nacos Discovery |
| codeforge-auth | Common + Web + Security + MySQL + JPA + H2 (test) + Nacos Discovery/Config |
| codeforge-code | Common + Web + Security + MySQL + Redis + Nacos Discovery/Config |
| codeforge-knowledge | Common + Web + Security + MySQL + Nacos Discovery/Config |
| codeforge-agent | Common + Web + Security + MySQL + Nacos Discovery/Config |
| codeforge-admin | Common + Web + Security + MySQL + JPA + Nacos Discovery/Config |

### Gateway 路由表

| 前缀 | → 服务 | 端口 |
|------|--------|------|
| `/api/auth/**`, `/api/user/**` | auth-service | 8081 |
| `/api/ai/**`, `/api/projects/**`, `/api/applications/**`, `/api/conversations/**`, `/api/dashboard/**`, `/api/codegen/**`, `/api/monitor/**` | code-service | 8082 |
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

### 6.4 统一工作台 `/api/codegen/*`（需登录，新架构）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/codegen/stream | 统一 SSE 流式生成（mode: SINGLE_FILE/MULTI_FILE/VUE_PROJECT） |
| POST | /api/codegen/modify/stream | SSE 流式代码修改 |
| POST | /api/codegen/deploy | 部署对话文件到 Nginx（`{conversationId}` → `{deployKey, url}`） |
| POST | /api/codegen/deploy-by-app | 部署应用到 Nginx（`{appId}` → `{deployKey, url}`） |

### 6.5 项目 `/api/projects/*`（需登录）

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
| POST | /api/applications | 保存应用（仅元信息 + conversationId，不存代码） |
| PUT | /api/applications/{id} | 更新名称/描述/封面 |
| PUT | /api/applications/{id}/priority | 设置优先级（0-默认/99-精选/999-置顶） |
| GET | /api/applications | 分页 + 关键词 + 类型筛选（SINGLE_FILE/MULTI_FILE/VUE_PROJECT） |
| GET | /api/applications/{id} | 详情（含 conversationId / deployKey） |
| DELETE | /api/applications/{id} | 删除 |
| GET | /api/applications/{id}/download | 下载 ZIP（优先 project_files 打包） |

### 6.6 知识库 `/api/knowledge/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/knowledge/documents | 上传文档（PDF/TXT/MD，最大 10MB） |
| GET | /api/knowledge/documents | 文档列表（分页 + 集合筛选） |
| GET | /api/knowledge/documents/{id} | 详情 |
| GET | /api/knowledge/documents/{id}/download | 下载原文件 |
| DELETE | /api/knowledge/documents/{id} | 删除（软删除） |
| POST | /api/knowledge/search | 语义搜索（多关键词交集 + 全文回退） |
| GET | /api/knowledge/stats | 统计（文档数 + 集合数） |
| GET | /api/knowledge/collections | 集合列表 |
| DELETE | /api/knowledge/collections/{name} | 删除集合 |

### 6.7 Agent `/api/agents/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/agents/workflow | 创建工作流 |
| PUT | /api/agents/workflow/{id} | 更新名称/描述 |
| GET | /api/agents/workflow | 工作流列表（分页） |
| GET | /api/agents/workflow/{id} | 详情（含执行结果） |
| DELETE | /api/agents/workflow/{id} | 删除 |
| POST | /api/agents/workflow/{id}/execute | SSE 流式执行（5 Agent 阶段式输出） |
| GET | /api/agents/tasks/{id} | 查询任务结果 |

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
| GET | /api/admin/applications | 所有用户应用列表 |
| PUT | /api/admin/applications/{id} | 编辑任意应用 |
| DELETE | /api/admin/applications/{id} | 删除任意应用 |
| GET | /api/admin/logs | 系统操作日志 |
| GET | /api/admin/stats/overview | 全局监控概览 |
| GET | /api/admin/stats/calls | 全局调用趋势 |
| GET | /api/admin/stats/tokens | 全局Token趋势 |
| GET | /api/admin/stats/models | 全局模型分布 |

### 6.9 其他

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/monitor/overview | 用户监控概览（只看自己数据） |
| GET | /api/monitor/calls | 用户调用趋势 |
| GET | /api/monitor/tokens | 用户Token趋势 |
| GET | /api/monitor/models | 用户模型分布 |
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
│   │   ├── ai/CodeGenWorkspace.vue    # AI 工作台（统一三栏布局，三种模式）
│   │   ├── ai/Generate.vue            # AI 代码生成（旧，含模型选择器）
│   │   ├── project/Create.vue         # 项目创建（旧）
│   │   ├── app/AppList.vue            # 我的应用（卡片跳转Workspace + 部署URL + 类型筛选 + 重命名）
│   │   ├── dashboard/Index.vue        # 工作台
│   │   ├── knowledge/Index.vue        # 知识库（上传/搜索/下载/集合管理）
│   │   ├── agents/Index.vue           # Agent 工作流（5 Agent 链 SSE 执行 + 结果导出）
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
| `/workspace` | AI 工作台（新） | auth |
| `/ai/generate` | AI 代码生成（旧） | auth |
| `/project/create` | 创建项目（旧） | auth |
| `/app/list` | 我的应用 | auth |
| `/knowledge` | 知识库 | auth |
| `/agents` | Agent 工作流 | auth |
| `/monitor` | 监控大盘 | auth |
| `/user/profile` | 个人中心 | auth |
| `/admin/users` | 用户管理 | admin |
| `/admin/models` | 模型配置 | admin |
| `/admin/logs` | 系统日志 | admin |
| `/admin/applications` | 应用管理 | admin |

---

## 八、数据库设计

### 8.1 数据表

| 表名 | 说明 |
|------|------|
| `users` | 用户 (BCrypt 密码 / 角色 / 状态 / 逻辑删除) |
| `applications` | 应用 (类型 / 语言 / deployKey / 部署时间 / 优先级) |
| `conversations` | 对话会话 (type: SINGLE_FILE/MULTI_FILE/VUE_PROJECT) |
| `messages` | 消息 (USER/AI 角色 / 内容 / token 数) |
| `project_files` | 项目文件 (对话ID / 路径 / 内容 / 大小) — DB + 磁盘双写 |
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

### 9.2 代码生成架构（新）

采用 **Executor + Strategy + Template Method** 设计模式：

```
POST /api/codegen/stream { type, prompt, conversationId, modelId }
         │
    CodeGenerationExecutor.resolveMode(type)
         │
    ┌────┴────┬────────────┐
    │         │            │
SINGLE_FILE  MULTI_FILE  VUE_PROJECT
    │         │            │
    └────┬────┴────────────┘
         │
  AbstractCodeGenerationStrategy.doStream()
         │
    ┌────┴────┐
    │  Token  │ → SSE event: token {"d":"..."}
    │  Done   │ → SSE event: done  {"d":"{...json...}"}
    └─────────┘
         │
  parseResponse() → JSON 优先（CodeGenJsonParser）+ 正则兜底
         │
  persistFiles() → DB (project_files) + 磁盘 (tmp/code_output/)
```

### 9.3 Prompt 模板（外部化）

| 模式 | 资源文件 | 输出格式 |
|------|---------|---------|
| SINGLE_FILE | `prompt/single-file-system-prompt.txt` | `{"htmlCode":"...", "description":"..."}` |
| MULTI_FILE | `prompt/multi-file-system-prompt.txt` | `{"htmlCode":"...", "cssCode":"...", "jsCode":"...", "description":"..."}` |
| VUE_PROJECT | `prompt/vue-project-system-prompt.txt` | `{"files":[{"path":"...","content":"..."}], "description":"...", "projectName":"..."}` |

### 9.4 AI 功能总览

| 功能 | 端点 | 模式 |
|------|------|------|
| AI 工作台（新） | `/api/codegen/stream` | SINGLE_FILE / MULTI_FILE / VUE_PROJECT |
| 单文件生成（旧） | `/api/ai/generate/stream` | NATIVE |
| 项目生成（旧） | `/api/projects/generate` | ENGINEERING |
| 代码修改 | `/api/codegen/modify/stream` | — |
| 代码审查 | `/api/ai/review` | 4 维度评分报告 |
| Agent 执行 | `/api/agents/workflow/{id}/execute` | 5 角色链式编排 |

---

## 十、Nginx 部署预览

### 10.1 工作流程

```
AI 生成代码 → project_files 表 + 磁盘
         │
  点击「🚀 部署预览」
         │
  DeployService.deployConversation(conversationId)
         │
  ┌──────┴──────┐
  │  生成 6 位   │  deployKey (例: aB3xK9)
  │  唯一标识    │
  └──────┬──────┘
         │
  复制文件到 tmp/code_deploy/<deployKey>/
         │
  返回 URL: http://localhost/<deployKey>/
         │
  iframe src 切换 → Nginx 提供静态文件服务
         │
  index.html 中 <link href="style.css"> 正常加载 ✅
```

### 10.2 存储路径

| 目录 | 用途 |
|------|------|
| `tmp/code_output/<type>_<id>/` | AI 生成原始文件（开发/预览） |
| `tmp/code_deploy/<deployKey>/` | Nginx 部署文件（生产访问） |
| `project_files` 表 | 数据库文件追踪（conversation_id + file_path + content + file_size） |

### 10.3 Nginx 配置要点

```nginx
# 部署应用访问 — 6 位 deployKey，支持 SPA
location ~ "^/([a-zA-Z0-9]{6})(.*)$" {
    root /path/to/tmp/code_deploy;
    try_files /$1$2 /$1/index.html =404;
}

# SSE 流式生成 — 禁用缓冲
location /api/codegen/ {
    proxy_pass http://gateway:8080;
    proxy_buffering off;
    proxy_read_timeout 600s;
}
```

### 10.4 本地开发 Nginx

```powershell
# Windows — 使用项目提供的 nginx-local.conf
.\nginx -c D:\Idea-program-file\largemodel\nginx-local.conf
```

配置文件：`nginx-local.conf`（项目根目录），监听 :80，代理 API 到 :8080，部署文件从 `tmp/code_deploy/` 提供。

### 10.5 Docker 部署

`docker-compose.yml` 中 `code-service` 和 `frontend` 通过 `deploy_data` 共享卷互通：

```yaml
code-service:
  volumes:
    - deploy_data:/app/tmp/code_deploy
frontend:
  volumes:
    - deploy_data:/var/www/deployed
```

---

## 十一、部署运行

### 11.1 本地开发

```bash
# 前置：MySQL 8 + Redis 7 + Nacos 3.1.2 已启动 + Nginx 已启动
#   Nacos 启动：bin/startup.cmd -m standalone  （:8848，用户/密码: nacos/123456）
#   Nginx 启动：./nginx -c D:\Idea-program-file\largemodel\nginx-local.conf

# 1. 重建数据库
#    MySQL 中执行: SOURCE .../codeforge-code/src/main/resources/db/init.sql;

# 2. 编译
set JAVA_HOME=D:\Java\jdk-21
cd largemodel_rearend
mvn compile -q

# 3. 启动服务（6 个终端，按依赖顺序）
mvn spring-boot:run -pl codeforge-auth        # :8081
mvn spring-boot:run -pl codeforge-code        # :8082
mvn spring-boot:run -pl codeforge-knowledge   # :8083
mvn spring-boot:run -pl codeforge-agent       # :8084
mvn spring-boot:run -pl codeforge-admin       # :8085
mvn spring-boot:run -pl codeforge-gateway     # :8080（最后启动）

# 4. 前端
cd largemodel_frontend
npm install && npm run dev
# → http://localhost:3000
```

### 11.2 Docker 部署

```bash
# 1. 克隆项目
git clone ...

# 2. 确认 .env 中有 OPENAI_API_KEY
echo "OPENAI_API_KEY=你的密钥" >> .env

# 3. 一键构建启动
bash docker-build.sh

# 4. 查看虚拟机ip
ip addr show | grep "inet " | grep -v 127.0.0.1
inet 192.168.x.x 或 inet 10.x.x.x 就是虚拟机的 IP
http://那个IP 即可
```

> 详细部署步骤（Ubuntu/VMware 环境、分步构建、故障排查等）见 **[Deployment.md](Deployment.md)**。

### 11.3 测试

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

## 十二、设计规范

### 12.1 品牌

| 项目 | 值 |
|------|-----|
| 名称 | **CodeForge**（代码锻造） |
| 风格 | 深色科技风（Dark Tech） |

### 12.2 Design Tokens

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

## 十三、文件索引

### 13.1 快速导航

| 功能域 | 前端入口 | 后端模块 | 数据表 |
|--------|---------|---------|--------|
| 用户认证 | `Login.vue` | auth :8081 | `users` |
| 个人信息 | `Profile.vue` | auth :8081 | `users` |
| 后台管理 | `Users.vue` `Models.vue` | admin :8085 | `users` `model_configs` |
| AI 工作台（新） | `CodeGenWorkspace.vue` | code :8082 | `conversations` `messages` `project_files` |
| AI 代码生成（旧） | `Generate.vue` | code :8082 | `conversations` `messages` |
| AI 代码审查 | `Generate.vue` | code :8082 | — |
| 工程项目（旧） | `Create.vue` | code :8082 | `conversations` + 磁盘 |
| 应用管理 | `AppList.vue` | code :8082 | `applications` + `project_files` |
| 知识库 | `knowledge/Index.vue` | knowledge :8083 | `knowledge_documents` |
| Agent 工作流 | `agents/Index.vue` | agent :8084 | `agent_workflows` |
| 监控 | `monitor/Index.vue` | — | — |

### 13.2 后端关键文件

| 文件 | 位置 | 职责 |
|------|------|------|
| `SecurityConfig.java` | common | Spring Security 无状态 + 白名单 |
| `JwtUtil.java` + `JwtAuthFilter.java` | common | JWT 签发 + 认证 (MyBatis-Plus) |
| `LangChain4jConfig.java` | common | LLM Bean (条件加载) |
| `DynamicModelProvider.java` | common | 动态模型工厂 (DB/fallback) |
| `RateLimitFilter.java` | gateway | 滑动窗口限流 |
| `AesUtil.java` | common | AES-256-GCM 加密 |
| `PromptTemplateService.java` | code | 外部化 Prompt 加载（resources/prompt/*.txt） |
| `CodeGenerationExecutor.java` | code | 执行器 — mode 路由到策略 |
| `AbstractCodeGenerationStrategy.java` | code | 模板方法 — SSE 流式引擎 + JSON 包裹 |
| `SingleFileStrategy.java` | code | 单文件模式（JSON + 正则） |
| `MultiFileStrategy.java` | code | 多文件模式（JSON + 正则） |
| `VueProjectStrategy.java` | code | Vue3 项目模式（JSON + [FILE] 标记） |
| `CodeGenJsonParser.java` | code | JSON 括号深度追踪提取 |
| `VueProjectBuilder.java` | code | Vue 项目构建器 — npm install + build + dist/ 产出 |
| `DeployService.java` | code | 部署服务 — deployKey 生成 + dist 复制 + 选取脚本注入 + 磁盘清理 |
| `FileModificationService.java` | code | 文件级修改 — AI 片段合并回完整文件 + 保留未修改文件 |
| `KnowledgeContextService.java` | code | 知识库 RAG — 文档检索 + Prompt 注入 |
| `prompt/agent-*.txt` | agent | Agent 角色提示词（analyzer/architect/coder/tester/reviewer） |
| `AiCodeGenService.java` | code | SSE 流式核心（旧，兼容保留） |
| `application.yml` | 各服务模块 | 业务配置 + Nacos 注册/配置地址 |

### 13.3 Docker / 部署关键文件

| 文件 | 位置 | 职责 |
|------|------|------|
| `docker-compose.yml` | `largemodel_rearend/` | 8 服务编排（MySQL / Redis / 6 后端），一键启动 |
| `Dockerfile` | `largemodel_rearend/` | 通用 Spring Boot 镜像（temurin:21-jre-alpine），`ARG MODULE` 区分服务 |
| `Dockerfile` | `largemodel_frontend/` | 多阶段构建（Node:22-alpine 编译 → Nginx:1.25-alpine 运行） |
| `nginx.conf` | `largemodel_frontend/` | SPA 路由 + `/api/` 反代 Gateway + SSE 直通（`proxy_buffering off`） |
| `application-docker.yml` | `codeforge-gateway/src/main/resources/` | Gateway Docker profile — 路由 URI 使用 Compose 服务名（`http://auth-service:8081` 等） |
| `nginx-local.conf` | 项目根目录 | Windows 本地 Nginx 配置（API 代理 + 部署文件服务） |
| `init.sql` | `codeforge-code/src/main/resources/db/` | 数据库初始化脚本（10 表，v2.3） |

> 部署步骤详见 **[Deployment.md](Deployment.md)**。

---
