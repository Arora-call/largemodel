# 大模型代码应用生成平台

> 作者：yx | 更新时间：2026-06-19（项目编辑模式 / 元素拾取 + AI 对话修改 / 预览拖拽 / 项目持久化 / 下载修复）

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
| 沙箱预览 | sandpack-vue3 (Sandpack) + 内联 SFC 编译 (Vue 3 CDN) | 3.1.x |
| 状态管理 | Pinia | 3.x |
| 构建工具 | Vite | 8.x |
| 包管理 | npm | — |

---

## 二、已完成功能总览

```
✅ 用户体系       注册/登录/JWT/找回密码/个人信息/注销/管理员后台
✅ AI 代码生成     SSE 流式/纯代码输出/多轮对话/中断续写/系统指令限制/单文件元素拾取编辑
✅ 工程项目创建     文件系统/文件树/CDN 沙箱预览/多项目切换/面板拖拽/元素拾取+AI对话修改
✅ 可视化编辑       内联 SFC 编译 / 编辑模式元素拾取 / AI 对话修改样式 / 错误捕获+AI修复
✅ 应用管理        CRUD/详情弹窗/代码预览/封面图/下载(fetch+blob)
✅ 对话持久化      MySQL 存储/Redis 会话/历史加载/自动同步/项目列表不丢失
✅ Swagger 文档    springdoc-openapi/所有接口自动文档
✅ 集成测试        19 个测试用例 (Auth + User)
✅ 账号切换        多账号保存/切换
✅ 头像上传        文件上传/静态资源映射
✅ Monaco Editor  只读代码查看器/语法高亮
✅ 预览拖拽        鼠标拖拽调节预览面板宽度 (280~800px)
✅ JWT查询参数认证  下载链接支持 ?token= 查询参数认证 (JwtAuthFilter 兜底)
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
│          │  │   └─ 预览面板 (sandpack/iframe)    │
│          │  └─ 继续生成 (中断后)               │
│          ├──────────────────────────────────┤
│          │ 输入框 ← Enter发送 Shift+Enter换行  │
└──────────┴──────────────────────────────────┘
```

**核心能力**:
- SSE 流式 token 推送，实时展示 AI 生成过程
- 智能解析：自动提取代码块，识别代码语言（**过滤文字说明，仅保留代码**）
- 多轮对话：自动携带历史代码上下文，支持增量修改
- 中断续写：ESC 终止，点击"继续生成"续写
- 代码块独立折叠/展开，一键复制（Clipboard API + execCommand 兜底）
- 前端单文件 (Vue/HTML) 支持 iframe 实时预览 + 元素拾取编辑修改
- 手动保存：顶栏按钮一键保存到「我的应用」（不会自动保存）
- **职责分离**：系统指令限制只能生成单文件/代码片段；遇到"项目"请求直接拒绝并引导至项目创建页面
- **对话标题**：始终从第一条用户消息提取，无视后端可能回传的系统指令文本
- **对话类型隔离**：`listConversations({ type: 'NATIVE' })` 仅加载代码生成类对话；客户端二次过滤：逐出 `type !== 'NATIVE'` 及系统指令标题条目，确保项目对话不会出现在代码生成页

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
┌──────────────────────────────────────────────────────────────────┐
│ 顶栏：[+ 新建项目] [保存项目] [下载 ZIP]                            │
├──────────┬────────────────────────┬──────────────────────────────┤
│ 项目列表 │ 代码查看 (Monaco)       │ CDN 沙箱预览 (可拖拽调宽度)     │
│ + 文件树 │ ← 可拖拽 →             │ ← 可拖拽 → 280~800px         │
│ 240px    │ flex:1                 │                              │
│          │                        │ ┌──────────────────────────┐ │
│ 📋 项目1  │ ┌────────────────────┐ │ │ 📦 项目预览   [✏️ 编辑]  │ │
│ 📋 项目2  │ │ <template>         │ │ ├────────────────────────┤ │
│ 📂 src/  │ │   <Calculator>     │ │ │  已选: button .btn      │ │
│  ├App.vue│ │ </template>        │ │ │  点击元素开始编辑        │ │
│  └main.js│ └────────────────────┘ │ │          (iframe)        │ │
│          │                        │ │ ├────────────────────────┤ │
│          │                        │ │ │ 描述修改要求...  [➤ 修改]│ │
│          │                        │ │ └──────────────────────────┘ │
└──────────┴────────────────────────┴──────────────────────────────┘
```

### 4.2 工作流程

```
1. 用户输入需求 → 后端 PromptTemplateService 构建 [PROJECT]/[FILE] 格式系统指令
2. LLM 流式返回 → 后端 parseProjectResponse() 多层解析:
   Layer 1: [PROJECT] 标签提取项目类型
   Layer 2: [DESC] 提取项目描述
   Layer 3: [FILE] path + ``` 代码块提取每个文件
   兜底0: [FILE] 标记存在但 ``` 块缺失 → 按 [FILE] 切分提取裸代码
   兜底1: 标准 ``` 代码块提取(兼容 // File: 注释)
   兜底2: 整个响应作为 output.txt
3. 后端创建真实目录: uploads/projects/project_{id}/
4. 前端 GET /api/projects/{id}/tree → 显示文件树
5. 点击文件 → GET /api/projects/{id}/file?path=... → Monaco 显示代码
6. 前端项目 → 批量加载所有文件 → SandboxPreview (CDN 内联 SFC 编译) 预览
7. 下载 → fetch + Authorization header → blob 下载
8. 保存 → POST /api/projects/{id}/save → applications 表
```

### 4.3 编辑模式（项目级 AI 对话修改）

- 点击预览工具栏 **「✏️ 编辑」** 进入编辑模式 → iframe 重新挂载（注入元素选择器）
- **元素拾取**：鼠标悬浮红框高亮 → 点击绿框选中 → `postMessage` 回传 tag/id/class/text
- **AI 修改输入栏**始终可见 → 输入修改要求 → Enter 发送
- 所有项目文件作为上下文拼接 `// ===== path =====` 格式 → 调用 `POST /api/ai/modify/stream`
- AI 返回修改后代码 → `parseAiFiles()` 解析多文件 → 更新本地文件 → 预览即时刷新
- **错误捕获**：CDN 预览中的 `showErr()` 同时 `postMessage` 错误到父窗口 → 显示 **🤖 AI 修复** 按钮
- ESC 取消修改，退出编辑模式自动清理状态

### 4.4 多项目切换与持久化

- 左侧项目列表加载后端 `listConversations({ type: 'ENGINEERING' })` — 刷新/切换页面不丢失
- 点击项目 → `switchProject(id)` → 重新加载文件树 + 预览
- **「+ 新建项目」**按钮（右上角蓝色高亮）→ 清空当前内容，开始新对话
- 生成完成后自动加入项目列表

### 4.5 预览面板拖拽

- 代码区 ←→ 预览区之间可拖拽分隔条 → 鼠标按住拖动调节预览宽度 (280px ~ 800px)
- `startResize()` — mousedown/mousemove/mouseup 实现

### 4.6 项目存储

| 存储位置 | 内容 |
|---------|------|
| **磁盘** `uploads/projects/project_{id}/` | 真实项目文件 (App.vue, package.json 等) |
| **MySQL** `conversations` 表 | 对话记录 (关联用户，type=ENGINEERING) |
| **MySQL** `applications` 表 | 保存后的项目元数据 (名称、类型、文件列表 JSON) |
| **MySQL** `messages` 表 | 用户与 AI 的消息历史 |

### 4.7 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/projects/generate` | SSE 流式生成项目 |
| GET | `/api/projects/{id}/tree` | 获取文件树 |
| GET | `/api/projects/{id}/file?path=` | 读取单个文件 |
| GET | `/api/projects/{id}/download` | 下载 ZIP (fetch+blob) |
| POST | `/api/projects/{id}/save` | 保存到应用库 |
| POST | `/api/ai/modify/stream` | AI 对话修改项目代码 |

---

## 五、可视化编辑 + 预览

### 5.1 sandpack 沙箱预览

项目预览采用 `sandpack-vue3`（基于 CodeSandbox Sandpack），并根据项目类型自动选择运行模式：

**CDN 模式（Vue 项目默认）**:
- 检测到 `.vue` 多文件项目时自动启用，使用 `<iframe srcdoc>` 直接渲染（绕过 Sandpack 外部脚本限制）
- 调用 `buildVueCDNHtml()`（`src/utils/sandboxCDN.js`）生成自包含 HTML：
  - 正则解析 `<template>` / `<script setup>` / `<style>`
  - 移除 import 语句（Vue API 从 CDN 全局解构获取，30+ API 全覆盖）
  - 逐行扫描 `const/let/var/function` 声明并生成 `setup()` return 语句
  - 支持多声明（`const a=1,b=2`）、解构（`{x,y}`）、箭头函数、async 函数
  - 组件依赖拓扑排序（BFS），确保子组件先于父组件定义
- 仅依赖 Vue 3 CDN（`unpkg.com`），无需额外外部库
- 绕过 Sandpack `vite-vue` 模板中 Vite → Rollup/esbuild 原生二进制依赖链

**vite-vue 模式（回退）**:
- 手动指定 `template="vite-vue"` 或设置 `useCDN="false"` 时使用
- 在 CodeSandbox Nodebox 中运行完整 Vite 构建器（需原生二进制，部分环境可能失败）

**`SandboxPreview.vue`** — 可复用沙箱预览组件，Props：
| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| files | Object | {} | 文件映射 `{ 'src/App.vue': 'code...' }` |
| template | String | '' | 手动指定模板，为空时自动检测 |
| customSetup | Object | {} | 额外 Sandpack 设置（依赖等） |
| readOnly | Boolean | true | 编辑器只读 |
| showEditor | Boolean | true | 显示代码编辑器面板 |
| height | String | '100%' | 容器高度 |
| useCDN | Boolean | true | Vue 项目使用 CDN 模式（避免原生二进制问题） |
| editMode | Boolean | false | 编辑模式：注入元素选择器 + postMessage |

- **模板自动检测** — 有 `.vue` + CDN 模式 → `static`，否则 `vite-vue`；纯 `.html` → `vanilla`
- **Events**: `@element-selected(payload)` — 元素被点击选中；`@preview-error(payload)` — 预览渲染错误
- **路径规范化** — 自动剥离公共顶层目录（`calculator/src/App.vue` → `/src/App.vue`）
- **依赖自动检测** — 扫描 import 自动添加 `element-plus` / `vue-router` / `pinia` / `axios` / `echarts`
- **延迟挂载** — `nextTick` 后挂载 sandpack，避免容器尺寸为 0 时初始化失败
- **错误恢复** — 全局监听 shell 错误，显示重试按钮
- **按钮隐藏** — 屏蔽 Open in CodeSandbox（需认证，匿名用户报错）
- **深色主题** — `theme="dark"` 匹配应用整体暗色 UI
- **强制重挂载** — 文件变化时 `sandpackKey++` 触发全新挂载，防止 shell ID 残留

### 5.2 AI 响应多文件解析

`Generate.vue` 中的 `parseAiFiles()` 函数自动解析 AI 响应中的多文件项目结构：

```
AI 生成格式:
// File: src/App.vue
```vue
<template>...</template>
<script setup>...</script>
```

// File: src/main.js
```javascript
import { createApp } from 'vue'
import App from './App.vue'
createApp(App).mount('#app')
```
```

解析结果 → `{ 'src/App.vue': '...', 'src/main.js': '...' }` → 传入 `<SandboxPreview>`

### 5.3 单文件 iframe 预览（兼容保留）

- 单文件代码（单个 Vue SFC 或 HTML）仍使用 iframe 预览
- 构建完整 HTML (Vue 3 CDN + Element Plus CDN + 模板编译)
- 沙箱隔离 (`sandbox="allow-scripts allow-same-origin"`)

### 5.4 元素拾取编辑（项目级 + 单文件）

**项目创建页面（CDN 模式）**:
- 编辑模式：点击 **「✏️ 编辑」** → iframe 重新挂载，注入 `#__picker` 浮层
- 鼠标悬浮高亮页面 DOM 元素（红框），点击选中（绿框）
- `postMessage` 回传元素 tag/id/class/text/html 信息到父窗口
- AI 修改输入栏始终可见 → 输入要求 → Enter → `POST /api/ai/modify/stream`
- 所有项目文件拼接为上下文，AI 返回修改后代码 → 解析更新 → 预览即时刷新
- CDN 预览中的渲染错误同时 `postMessage` 到父窗口 → 显示 "AI 修复" 按钮
- ESC 取消修改

**AI 代码生成页面（iframe 模式）**:
- 单文件预览切换「预览」/「编辑」标签进入编辑模式
- 同样的元素拾取 + AI 修改流程
- modifyCodeStream → 替换当前代码

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

### 6.2 保存方式

- **手动保存**：顶栏「💾 保存」按钮，由用户主动触发保存到「我的应用」
- **不会自动保存**：AI 生成/续写完成后仅保留在当前对话中，需手动保存
- 保存时自动按语言生成封面图: Vue→绿, Java→橙, Python→蓝, HTML→红

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
│   ├── SandboxPreview.vue    # sandpack-vue3 沙箱预览组件
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
│   ├── markdown.js           # Markdown 渲染 + 复制工具
│   └── sandboxCDN.js         # CDN 模式 HTML 生成（内联 SFC 编译）
├── sandpack-vue3             # 浏览器端 Vite 打包器 (沙箱依赖)
└── views/
    ├── ai/
    │   └── Generate.vue      # AI 代码生成 (聊天模式 + sandpack/iframe 预览)
    ├── app/
    │   └── AppList.vue       # 我的应用
    ├── auth/
    │   ├── Login.vue
    │   ├── Register.vue
    │   └── ForgotPassword.vue
    ├── dashboard/
    │   └── Index.vue         # 工作台
    ├── project/
    │   └── Create.vue        # 项目创建 (多项目切换 + 可拖拽三栏 + sandpack)
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
| GET | /api/conversations | 对话列表（可选 `?type=NATIVE\|ENGINEERING` 过滤） |
| GET | /api/conversations/{id}/messages | 对话消息 |
| DELETE | /api/conversations/{id} | 删除对话（同时清理项目文件） |
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
