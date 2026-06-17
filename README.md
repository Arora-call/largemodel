# 大模型代码应用生成平台

> 作者：yx | 更新时间：2026-06-17

---

## 14 天进度总览

```
第1天   SpringBoot 基础        ■■■■■■■■■■ 100% (学习完成)
第2天   Vue3 基础              ■■■■■■■■■■ 100% (学习完成)
第3天   LangChain4j 基础       ■■■■■■■■■■ 100% (学习完成)
第4天   Docker 基础            ■■■■■■■■■■ 100% (学习完成)
第5天   微服务基础              ■■■■■■■■■■ 100% (学习完成)
第6天   项目总览               ■■■■■■■■■■ 100%
第7天   项目初始化+用户模块      ■■■■■■■■■■ 100% ✅
第8天   用户体系完善            ■■■■■■■■■■ 100% ✅
第9天   AI 代码生成             ■■■■■■■■■■ 100% ✅ (刚完成)
第10天  应用模块               □□□□□□□□□□   0% ← 下一步
第11天  对话历史+工程生成        □□□□□□□□□□   0%
第12天  功能扩展+可视化编辑      □□□□□□□□□□   0%
第13天  AI 工作流+系统优化       □□□□□□□□□□   0%
第14天  部署上线+微服务          □□□□□□□□□□   0%
```

---

## 一、技术栈差异说明

| 方案要求 | 当前实现 | 处理方式 |
|:--|:--|:--|
| MyBatis + Mapper | JPA + Repository | **不迁移**。JPA 已稳定，Specification + @Query 覆盖全部需求 |
| Interceptor 拦截器 | JwtAuthFilter (Servlet Filter) | 等价，Spring Security Filter Chain 为标准做法 |
| 代码生成器 (Day7) | 手写完成 | 用户模块全部代码已手动实现 |

---

## 二、已完成功能（第七~九天）

### 2.1 数据库 — 6 张表

| 表 | 实体 | 说明 |
|:--|:--|:--|
| users | User | 用户表（BCrypt加密/逻辑删除/角色/状态） |
| roles | Role | 角色表（USER / ADMIN） |
| user_roles | UserRoleRelation | 用户角色关联表（多角色扩展） |
| applications | Application | 应用表（AI生成的代码应用） |
| conversations | Conversation | 对话会话表 |
| messages | Message | 消息表（USER / AI / SYSTEM） |

### 2.2 后端 — 接口清单（16 个）

#### 认证模块 `/api/auth/*`（公开）

| 接口 | 方法 | 说明 |
|:--|:--|:--|
| /api/auth/register | POST | 注册（用户名唯一/BCrypt加密） |
| /api/auth/login | POST | 登录（返回JWT + 用户信息） |
| /api/auth/me | GET | 获取当前用户 |
| /api/auth/forgot-password | POST | 找回密码（用户名/邮箱验证+新密码） |

#### 用户模块 `/api/user/*`（需登录）

| 接口 | 方法 | 说明 |
|:--|:--|:--|
| /api/user/info | GET | 查看个人信息 |
| /api/user/info | PUT | 修改个人信息（昵称/邮箱/手机号） |
| /api/user/password | PUT | 修改密码（验证原密码） |
| /api/user/account | DELETE | 注销账户（逻辑删除） |

#### 管理模块 `/api/admin/*`（需 ADMIN 角色）

| 接口 | 方法 | 说明 |
|:--|:--|:--|
| /api/admin/users | GET | 用户列表（分页+关键词+角色+状态筛选） |
| /api/admin/users/{id} | GET | 用户详情 |
| /api/admin/users/{id}/status | PUT | 启用/禁用 |
| /api/admin/users/{id}/role | PUT | 修改角色 |
| /api/admin/users/{id}/password | PUT | 重置密码 |
| /api/admin/users/{id} | DELETE | 删除用户（逻辑删除） |
| /api/admin/stats | GET | 用户统计 |

#### AI 模块 `/api/ai/*`（需登录）

| 接口 | 方法 | 说明 |
|:--|:--|:--|
| /api/ai/generate | POST | 非流式代码生成（一次性返回） |
| /api/ai/generate/stream | POST | 流式代码生成（SSE 逐Token推送） |

### 2.3 后端架构

```
controller/
  AuthController           认证接口 (register/login/me/forgot-password)
  UserController           个人信息 (info/password/account)
  AdminController          后台管理 (users CRUD + 重置密码 + 统计)
  AiController             AI生成 (generate + generate/stream SSE)
  HealthController         健康检查

service/
  AuthService              认证业务 (注册/登录/JWT/找回密码)
  UserService              个人信息业务
  AdminService             后台管理业务 (分页/角色/状态/密码重置/删除)
  ai/AiCodeGenService      AI生成核心 (流式/非流式/多轮对话/消息持久化)
  ai/PromptTemplateService Prompt模板 (原生/工程/修改 三种场景)

repository/
  UserRepository / RoleRepository / UserRoleRelationRepository
  ApplicationRepository / ConversationRepository / MessageRepository

entity/
  User / Role / UserRoleRelation / BaseEntity
  Application / Conversation / Message

dto/
  request/     LoginRequest, RegisterRequest, UserUpdateRequest,
               UserPasswordRequest, ForgetPasswordRequest, GenerateCodeRequest
  response/    ApiResponse<T>, LoginResponse, UserInfoResponse, PageResponse<T>

config/
  SecurityConfig            Spring Security 配置
  JwtUtil                   JWT 工具 (生成/解析/校验)
  JwtAuthFilter             JWT 认证过滤器
  CorsConfig                跨域配置
  DataInitializer           启动初始化 (角色/管理员)
  LangChain4jConfig         LLM 配置 (OpenAI/Ollama 双Provider)

aop/
  AuthCheckAspect           @AuthCheck 切面实现

annotation/
  AuthCheck                 自定义权限注解

exception/
  BusinessException         业务异常
  GlobalExceptionHandler    全局异常处理
```

### 2.4 前端 — 7 个页面

| 路由 | 页面 | 功能 |
|:--|:--|:--|
| /auth/login | Login.vue | 表单验证 / JWT存储 / 跳转 |
| /auth/register | Register.vue | 两次密码一致性校验 |
| /auth/forgot-password | ForgotPassword.vue | 用户名+邮箱验证 → 重置密码 |
| /dashboard | Index.vue | 欢迎卡片 / 统计 / 快捷导航 |
| /ai/generate | Generate.vue | **左右分屏** — 对话区 + 代码预览区 / SSE流式渲染 |
| /user/profile | Profile.vue | 信息修改 / 改密码 / 注销 |
| /admin/users | Users.vue | 分页 / 搜索 / 筛选 / 开关 / 角色 / 重置密码 / 删除 |

### 2.5 前端架构

```
layouts/
  AuthLayout               登录/注册页（渐变背景+毛玻璃卡片）
  DefaultLayout            主界面（深色侧边栏可折叠 + 顶栏下拉菜单）

views/
  auth/   Login / Register / ForgotPassword
  dashboard/   Index
  ai/    Generate           左右分屏：对话流 + 深色代码预览
  user/  Profile
  admin/ Users

api/
  request.js               Axios实例（Token拦截 / 401智能区分登录页/其他页）
  auth.js / user.js / admin.js / ai.js

stores/
  auth.js                  Pinia状态（Token+User持久化 / isAdmin / logout）

router/
  index.js                 路由守卫（未登录拦截 / ADMIN校验 / 游客自动跳转）
```

### 2.6 安全机制

```
JWT 无状态认证            24h 有效期，HMAC-SHA256 签名
BCrypt 密码加密            不可逆哈希
@AuthCheck("ADMIN")       自定义 AOP 注解，替代 @PreAuthorize
逻辑删除                   deleted 字段软删除
账户禁用                   status 字段，禁用后 Token 也无效
全局异常统一处理            GlobalExceptionHandler → ApiResponse
SSE 流式认证                fetch + Authorization Header
```

---

## 三、第九天新增内容详情

### 3.1 新增 10 个后端文件

| 文件 | 说明 |
|:--|:--|
| entity/Application.java | 应用实体，NATIVE/ENGINEERING 两种类型 |
| entity/Conversation.java | 对话会话实体，关联用户和应用 |
| entity/Message.java | 消息实体，USER/AI/SYSTEM 三种角色 |
| repository/ApplicationRepository.java | 按用户+状态分页查询 |
| repository/ConversationRepository.java | 按用户+更新时间排序 |
| repository/MessageRepository.java | 按对话排序 + 游标分页 @Query |
| dto/request/GenerateCodeRequest.java | 生成请求（prompt/type/language/conversationId） |
| service/ai/AiCodeGenService.java | 核心 — 流式/非流式生成、多轮对话历史、消息持久化 |
| service/ai/PromptTemplateService.java | 三类 Prompt 模板 + 消息构建 |
| controller/AiController.java | /api/ai/generate + /api/ai/generate/stream (SSE) |
| config/LangChain4jConfig.java | OpenAI / Ollama 双 Provider 切换 |

### 3.2 新增 2 个前端文件

| 文件 | 说明 |
|:--|:--|
| api/ai.js | SSE 流式请求封装（fetch + ReadableStream + 事件解析） |
| views/ai/Generate.vue | 左右分屏 — 左对话区 + 右深色代码预览 / 示例Prompt / Ctrl+Enter / 复制/保存 |

### 3.3 配置变更

| 文件 | 变更 |
|:--|:--|
| pom.xml | + langchain4j + langchain4j-open-ai 依赖 |
| application.yml | + langchain4j 配置（provider / openai / ollama） |
| db/init.sql | + applications / conversations / messages 建表 DDL |
| router/index.js | + /ai/generate 路由 |
| layouts/DefaultLayout.vue | + 侧栏"AI 代码生成"菜单项 |

---

## 四、AI 代码生成 SSE 数据流

```
浏览器                                 后端
  │                                      │
  ├─ POST /api/ai/generate/stream ──────▶│
  │  { prompt, type, language }          │
  │                                      ├─ PromptTemplateService 构建消息
  │                                      ├─ ChatLanguageModel 调用 LLM
  │                                      │
  │◀── event: token ◀───────────────────┤ token 逐个推送
  │    data: "public"                    │
  │◀── event: token ◀───────────────────┤
  │    data: " class"                    │
  │◀── event: token ◀───────────────────┤
  │    data: " Hello"                    │
  │       ...                            │
  │◀── event: done ◀───────────────────┤ 生成完成
  │    data: "public class Hello..."     │
  │                                      ├─ 持久化 USER + AI 消息到 MySQL
  │                                      ├─ 自动创建/关联 Conversation
```

### LLM Provider 切换

```yaml
# OpenAI (默认)
langchain4j:
  provider: openai
  openai:
    api-key: ${OPENAI_API_KEY}
    model-name: gpt-4o

# Ollama 本地模型 (零成本开发)
langchain4j:
  provider: ollama
  ollama:
    base-url: http://localhost:11434
    model-name: qwen2.5:7b
```

---

## 五、下一步：第十天 — 应用模块

- Application CRUD 接口 + 前端页面（列表/详情/新建）
- 三种部署方案（预览运行 / 下载源码包 / 推送到 Git）
- 前端页面生成和优化

## 六、第十一天 — 对话历史 + 工程项目生成
- 对话历史 CRUD + 游标分页
- Redis 接入（分布式 Session + 对话记忆持久化）
- Tool Calling 工程项目生成
- 封面图自动生成 + 源码打包下载

## 七、第十二天 — 功能扩展 + 可视化编辑
- AI 智能路由（分场景修改分发）
- Monaco Editor 集成 → 在线可视化编辑
- 原生应用全量修改 + 工程项目增量修改

## 八、第十三天 — AI 工作流 + 系统优化
- LangGraph4j 流程编排编排
- 性能优化 / 安全性优化 / 稳定性优化 / 成本优化

## 九、第十四天 — 部署上线 + 微服务 + 监控
- SpringCloud Alibaba 微服务拆分
- Nacos 注册中心 / Gateway 网关 / Sentinel 限流
- Docker Compose 容器化部署
- Prometheus + Grafana 可观测性监控
- 业务数据看板
