# 大模型代码应用生成平台

> 作者：yx | 更新时间：2026-06-18

---

## 目录

- [一、技术栈](#一技术栈)
- [二、已完成功能总览](#二已完成功能总览)
- [三、AI 代码生成模块](#三ai-代码生成模块)
- [四、工程项目创建模块](#四工程项目创建模块)
- [五、可视化编辑 + 预览](#五可视化编辑--预览)
- [六、应用管理 + 下载](#六应用管理--下载)
- [七、用户体系 + 权限](#七用户体系--权限)
- [八、前端架构](#八前端架构)
- [九、后端架构](#九后端架构)
- [十、API 接口清单](#十api-接口清单)
- [十一、数据存储](#十一数据存储)
- [十二、部署运行](#十二部署运行)

---

## 一、技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.5.4 |
| JDK | Java | 21 |
| 数据库 | MySQL + JPA (Hibernate) | 8.x |
| 缓存 | Redis + Lettuce | — |
| 安全 | Spring Security + JWT (jjwt) | 0.12.6 |
| AI | LangChain4j + Zhipu AI (GLM-4.5-air) | 1.0.0-beta2 |
| API 文档 | Springdoc OpenAPI (Swagger) | 2.7.0 |
| 前端框架 | Vue 3 (Composition API) | 3.5.x |
| UI 库 | Element Plus | 2.14.x |
| 代码编辑器 | Monaco Editor | 0.52.x |
| 状态管理 | Pinia | 3.x |
| 构建工具 | Vite | 8.x |
| 包管理 | npm | — |

---

## 二、已完成功能总览

```
✅ 用户体系       注册/登录/JWT/找回密码/个人信息/注销/管理员后台
✅ AI 代码生成     SSE 流式/智能解析/多轮对话/中断续写/代码折叠/复制
✅ 工程项目创建     真实文件系统/文件树/代码查看/前端预览/ZIP 下载
✅ 可视化编辑       iframe 预览/元素拾取/修改模式
✅ 应用管理        CRUD/详情弹窗/代码预览/封面图/下载
✅ 对话持久化      MySQL 存储/Redis 会话/历史加载/自动同步
✅ Swagger 文档    springdoc-openapi/所有接口自动文档
✅ 集成测试        19 个测试用例 (Auth + User)
✅ 账号切换        多账号保存/切换
✅ 头像上传        文件上传/静态资源映射
✅ Monaco Editor  只读代码查看器/语法高亮
```

---

## 三、AI 代码生成模块

### 3.1 代码生成（聊天模式）

**路由**: `/ai/generate`

```
┌──────────┬──────────────────────────────────┐
│ 侧边栏   │ 对话区                            │
│ 对话列表 │  ├─ 用户消息                       │
│ +新建    │  ├─ AI 文字说明 (Markdown 渲染)     │
│          │  ├─ 代码块 (Monaco Editor)          │
│          │  │   ├─ 预览按钮 (仅前端代码)        │
│          │  │   ├─ 复制 / 折叠                 │
│          │  │   └─ 预览面板 (iframe 沙箱)       │
│          │  └─ 继续生成 (中断后)               │
│          ├──────────────────────────────────┤
│          │ 输入框 ← Enter发送 Shift+Enter换行  │
└──────────┴──────────────────────────────────┘
```

**核心能力**:
- SSE 流式 token 推送，实时展示 AI 生成过程
- 智能解析：自动分离文字说明 + 代码块，识别代码语言
- 多轮对话：自动携带历史代码上下文，支持增量修改
- 中断续写：ESC 终止，点击"继续生成"续写
- Markdown 渲染：标题、粗体、列表、行内代码、代码块
- 代码块独立折叠/展开，一键复制（Clipboard API + execCommand 兜底）
- 前端代码 (Vue/HTML) 支持 iframe 实时预览 + 元素拾取编辑
- 页面离开校验、Enter 发送防抖、输入框自动聚焦

### 3.2 SSE 数据流

```
浏览器                                    后端
  │                                         │
  ├─ POST /api/ai/generate/stream ─────────▶│
  │  { prompt, type, language,              │
  │    conversationId, originalPrompt }     │
  │                                         ├─ PromptTemplateService 构建消息
  │                                         ├─ LangChain4j 调用 Zhipu AI
  │                                         │
  │◀── event: token ◀──────────────────────┤ 逐 token 推送
  │    data: "public"                       │
  │◀── event: token ◀──────────────────────┤
  │    data: " class"                       │
  │       ...                               │
  │◀── event: done ◀──────────────────────┤ 生成完成
  │    data: {"code":"...","text":"...",    │
  │           "language":"java",             │
  │           "conversationId":123}          │
  │                                         ├─ 持久化 USER + AI 消息到 MySQL
  │                                         ├─ extractCode/extractText 解析
```

---

## 四、工程项目创建模块

### 4.1 项目创建页面

**路由**: `/project/create`

```
┌──────────────────────────────────────────────────────┐
│ 顶栏：项目概述 | [保存项目] [下载 ZIP]                  │
├──────────┬────────────────────────┬──────────────────┤
│ 文件树   │ 代码查看 (Monaco)       │ 预览 (前端项目)    │
│ 240px    │ flex:1                 │ 420px            │
│          │                        │                  │
│ 📂 src/  │ ┌────────────────────┐ │ ┌──────────────┐ │
│  ├App.vue│ │ <template>         │ │ │  iframe      │ │
│  ├main.js│ │   <DataTable>      │ │ │  实时渲染     │ │
│  └data.js│ │ </template>        │ │ │              │ │
│ 📄 pkg   │ └────────────────────┘ │ └──────────────┘ │
│ 📄 vite  │                        │ [预览] [编辑]     │
│ 📄 index │                        │                  │
└──────────┴────────────────────────┴──────────────────┘
```

### 4.2 工作流程

```
1. 用户输入需求 → AI 流式生成 → 解析 [PROJECT] + [FILE] 标记
2. 后端创建真实目录: uploads/projects/project_{id}/
   ├── package.json
   ├── src/App.vue
   ├── src/main.js
   └── ...
3. 前端 GET /api/projects/{id}/tree → 显示文件树
4. 点击文件 → GET /api/projects/{id}/file?path=... → 显示代码
5. 前端项目 → 读取 HTML/App.vue → iframe 预览
6. 下载 → GET /api/projects/{id}/download → ZIP
7. 保存 → POST /api/projects/{id}/save → applications 表
```

### 4.3 项目存储

| 存储位置 | 内容 |
|---------|------|
| **磁盘** `uploads/projects/project_{id}/` | 真实项目文件 (App.vue, package.json 等) |
| **MySQL** `applications` 表 | 项目元数据 (名称、类型、文件列表 JSON) |

### 4.4 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/projects/generate` | SSE 流式生成项目 |
| GET | `/api/projects/{id}/tree` | 获取文件树 |
| GET | `/api/projects/{id}/file?path=` | 读取单个文件 |
| GET | `/api/projects/{id}/download` | 下载 ZIP |
| POST | `/api/projects/{id}/save` | 保存到应用库 |

---

## 五、可视化编辑 + 预览

### 5.1 iframe 预览

- 前端代码 (Vue/HTML) 支持实时预览
- 构建完整 HTML (Vue 3 CDN + Element Plus CDN + 模板编译)
- 沙箱隔离 (`sandbox="allow-scripts allow-same-origin"`)

### 5.2 元素拾取编辑

- 编辑模式：鼠标悬浮高亮页面 DOM 元素 (`#__picker` 浮层)
- 点击选中：`postMessage` 回传元素 tag/id/class/text 信息
- 修改输入：描述修改需求 → AI 增量更新代码
- 状态管理：清空选中、取消修改、加载状态

---

## 六、应用管理 + 下载

### 6.1 我的应用

**路由**: `/app/list`

- 卡片布局展示所有已保存应用
- 关键词搜索 + 语言筛选
- 分页 (12条/页)
- 点击卡片 → 详情弹窗 (类型/语言/时间/代码预览/项目结构/依赖)
- 下载按钮 (带 JWT 认证的 blob 下载)
- 删除确认

### 6.2 封面图

- AI 生成完成后自动保存
- 按语言分色: Vue→绿, Java→橙, Python→蓝, HTML→红

---

## 七、用户体系 + 权限

### 7.1 认证

- JWT 无状态认证 (HMAC-SHA256, 24h 有效期)
- BCrypt 密码加密
- 注册/登录/找回密码 (用户名+邮箱验证)
- 个人信息修改 / 密码修改 / 账户注销 (逻辑删除)

### 7.2 权限

- USER / ADMIN 两级角色
- `@AuthCheck("ADMIN")` 自定义 AOP 注解
- 前端路由守卫 (`requiresAuth` / `requiresAdmin`)
- Axios 拦截器 (Token 注入 / 401 智能区分 / 403 提示)

### 7.3 分布式会话

- Redis 存储 (localhost:6379, 1h TTL)
- JSON 序列化 (GenericJackson2JsonRedisSerializer)

### 7.4 账号切换

- 多账号保存到 localStorage `savedAccounts`
- Header 下拉菜单显示已保存账号列表
- 切换/添加/移除账号
- 切换后自动刷新页面

### 7.5 头像上传

- `POST /api/user/avatar` (multipart/form-data, ≤2MB)
- 存储到 `uploads/avatars/` (UUID 文件名)
- 静态资源映射 `/uploads/**` → `file:uploads/`
- 前端 Profile.vue 头像区域 + 预览弹窗

---

## 八、前端架构

```
src/
├── api/                      # API 函数
│   ├── request.js            # Axios 实例 (拦截器)
│   ├── auth.js               # 认证
│   ├── user.js               # 用户
│   ├── admin.js              # 管理
│   ├── ai.js                 # AI 生成 (SSE)
│   ├── app.js                # 应用管理
│   └── project.js            # 工程项目
├── components/
│   ├── CodeViewer.vue        # Monaco Editor 只读查看器
│   └── project/
│       └── FileTree.vue       # 文件树组件 (递归渲染)
├── layouts/
│   ├── AuthLayout.vue        # 登录/注册布局
│   └── DefaultLayout.vue     # 主界面布局 (侧边栏+顶栏)
├── router/
│   └── index.js              # 路由 + 守卫
├── stores/
│   └── auth.js               # Pinia 状态管理
├── utils/
│   └── markdown.js           # Markdown 渲染 + 复制工具
└── views/
    ├── ai/
    │   └── Generate.vue      # AI 代码生成 (聊天模式)
    ├── app/
    │   └── AppList.vue       # 我的应用
    ├── auth/
    │   ├── Login.vue
    │   ├── Register.vue
    │   └── ForgotPassword.vue
    ├── dashboard/
    │   └── Index.vue         # 工作台
    ├── project/
    │   └── Create.vue        # 项目创建 (三栏 IDE)
    ├── user/
    │   └── Profile.vue       # 个人中心
    └── admin/
        └── Users.vue         # 用户管理
```

---

## 九、后端架构

```
src/main/java/org/example/
├── config/
│   ├── SecurityConfig.java       # Spring Security
│   ├── JwtUtil.java              # JWT 工具
│   ├── JwtAuthFilter.java       # JWT 认证过滤器
│   ├── CorsConfig.java           # 跨域
│   ├── DataInitializer.java      # 启动初始化管理员
│   ├── LangChain4jConfig.java    # LLM 配置
│   ├── OpenApiConfig.java        # Swagger
│   ├── RedisConfig.java          # Redis
│   └── WebMvcConfig.java         # 静态资源映射
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── AdminController.java
│   ├── AiController.java
│   ├── ApplicationController.java
│   ├── ConversationController.java
│   ├── DashboardController.java
│   ├── ProjectController.java    # 工程项目
│   └── HealthController.java
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── AdminService.java
│   ├── ProjectService.java       # 文件系统服务
│   └── ai/
│       ├── AiCodeGenService.java # AI 生成核心
│       └── PromptTemplateService.java # Prompt 模板
├── entity/
│   ├── User.java
│   ├── Application.java
│   ├── Conversation.java
│   └── Message.java
├── repository/
│   ├── UserRepository.java
│   ├── ApplicationRepository.java
│   ├── ConversationRepository.java
│   └── MessageRepository.java
├── dto/
│   ├── request/ (LoginRequest, RegisterRequest, GenerateCodeRequest, ...)
│   └── response/ (ApiResponse, LoginResponse, UserInfoResponse, PageResponse)
├── enums/ (UserRole, UserStatus)
├── exception/
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
└── aop/ (AuthCheckAspect)
```

---

## 十、API 接口清单

### 10.1 认证 `/api/auth/*`（公开）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册 |
| POST | /api/auth/login | 登录 |
| GET | /api/auth/me | 获取当前用户 |
| POST | /api/auth/forgot-password | 找回密码 |

### 10.2 用户 `/api/user/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/user/info | 查看个人信息 |
| PUT | /api/user/info | 修改个人信息 |
| PUT | /api/user/password | 修改密码 |
| DELETE | /api/user/account | 注销账户 |
| POST | /api/user/avatar | 上传头像 |

### 10.3 管理 `/api/admin/*`（需 ADMIN）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/admin/users | 用户列表 (分页+筛选) |
| GET | /api/admin/users/{id} | 用户详情 |
| PUT | /api/admin/users/{id}/status | 启用/禁用 |
| PUT | /api/admin/users/{id}/role | 修改角色 |
| DELETE | /api/admin/users/{id} | 删除用户 |
| PUT | /api/admin/users/{id}/password | 重置密码 |

### 10.4 AI 生成 `/api/ai/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/ai/generate/stream | SSE 流式代码生成 |
| POST | /api/ai/modify/stream | SSE 流式代码修改 |

### 10.5 应用 `/api/applications/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/applications | 保存应用 |
| GET | /api/applications | 分页列表 + 筛选 |
| GET | /api/applications/{id} | 应用详情 |
| DELETE | /api/applications/{id} | 删除应用 |
| GET | /api/applications/{id}/download | 下载代码 |

### 10.6 对话 `/api/conversations/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/conversations | 对话列表 |
| GET | /api/conversations/{id}/messages | 对话消息 |
| DELETE | /api/conversations/{id} | 删除对话 |
| DELETE | /api/conversations/clear | 清空全部 |

### 10.7 工程项目 `/api/projects/*`（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/projects/generate | SSE 流式生成 |
| GET | /api/projects/{id}/tree | 文件树 |
| GET | /api/projects/{id}/file?path= | 读取文件 |
| GET | /api/projects/{id}/download | 下载 ZIP |
| POST | /api/projects/{id}/save | 保存到应用库 |

### 10.8 其他

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/health | 健康检查 (公开) |
| GET | /api/dashboard/stats | 仪表盘统计 |

---

## 十一、数据存储

### 11.1 MySQL 数据库 `largemodel`

| 表 | 说明 |
|----|------|
| `users` | 用户 (BCrypt 密码 / 角色 / 状态 / 逻辑删除 / 头像) |
| `applications` | 应用 (类型 / 语言 / 源码 / 配置 JSON / 封面图) |
| `conversations` | 对话会话 (关联用户 + 应用) |
| `messages` | 消息 (USER / AI 角色 / 原始内容 / token 数) |

### 11.2 Redis

| 用途 | Key 模式 | TTL |
|------|---------|-----|
| 分布式会话 | `spring:session:*` | 1h |
| 对话缓存 | conversation cache | 1h |

### 11.3 磁盘文件

| 目录 | 内容 |
|------|------|
| `uploads/avatars/` | 用户头像 (UUID 文件名) |
| `uploads/projects/project_{id}/` | 工程项目真实文件 |
| `uploads/projects/project_{id}.zip` | 项目 ZIP 包 |

---

## 十二、部署运行

### 后端

```bash
cd largemodel_rearend

# 确保 MySQL + Redis 已启动
# 数据库: largemodel (JPA ddl-auto: update 自动建表)
# 管理员: admin / admin123 (DataInitializer 自动创建)

mvn spring-boot:run
# → http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui/index.html
```

### 前端

```bash
cd largemodel_frontend

npm install
npm run dev
# → http://localhost:3000 (proxy /api → localhost:8080)
```

### 测试

```bash
cd largemodel_rearend
mvn test
# 19 个集成测试 (H2 内存数据库)
```

---

> **相关文档**: `需求.md` — 详细功能需求与计划
