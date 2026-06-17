# largemodel 大模型代码生成平台

# 项目功能实现文档

## 后端实现功能 - 6.17

### 1. 架构与基础设施

| 组件 | 文件 | 说明 |
|---|---|---|
| 启动入口 | Application.java | Spring Boot 3.5.4 + Java 21 |
| 配置文件 | application.yml | 数据库连接、JWT 密钥/有效期、日志 |
| 数据库脚本 | db/init.sql | 建库建表 + 种子数据 |

---

### 2. 数据层（3 张表）

| 表 | 实体 | 关键字段 |
|---|---|---|
| users | User | id, username, password(BCrypt), nickname, email, phone, avatar, role, status, deleted, last_login_at, 审计字段 |
| roles | Role | id, name, description |
| user_roles | UserRoleRelation | user_id, role_id |

---

### 3. 认证与安全

| 功能 | 实现方式 |
|---|---|
| JWT 生成/解析/校验 | JwtUtil — HMAC-SHA256 签名，24h 有效期 |
| 请求拦截认证 | JwtAuthFilter — 从 Header 取 Bearer Token，注入 SecurityContext |
| 密码加密 | BCryptPasswordEncoder |
| 接口权限控制 | SecurityConfig + @PreAuthorize 方法级注解 |
| 跨域配置 | CorsConfig — 允许所有来源 |
| 逻辑删除 | deleted 字段软删除，查询自动过滤 |
| 账户禁用 | status 字段控制，禁用的用户即使 Token 有效也无法访问 |

---

### 4. API 接口清单（12 个）

#### 认证模块 — `/api/auth/*`（无需登录）

| 接口 | 方法 | 功能 |
|---|---|---|
| /api/auth/register | POST | 用户注册（用户名唯一校验） |
| /api/auth/login | POST | 登录（返回 Token + 用户信息） |
| /api/auth/me | GET | 获取当前登录用户信息 |

#### 用户模块 — `/api/user/*`（需登录）

| 接口 | 方法 | 功能 |
|---|---|---|
| /api/user/info | GET | 查看个人信息 |
| /api/user/info | PUT | 修改个人信息（昵称/邮箱/手机号） |
| /api/user/password | PUT | 修改密码（需验证原密码） |
| /api/user/account | DELETE | 注销账户（逻辑删除+禁用） |

#### 管理模块 — `/api/admin/*`（需 ADMIN 角色）

| 接口 | 方法 | 功能 |
|---|---|---|
| /api/admin/users | GET | 用户列表（分页 + 关键词 + 角色 + 状态筛选） |
| /api/admin/users/{id} | GET | 用户详情 |
| /api/admin/users/{id}/status | PUT | 启用/禁用用户 |
| /api/admin/users/{id}/role | PUT | 修改用户角色 |
| /api/admin/users/{id} | DELETE | 删除用户（逻辑删除） |
| /api/admin/stats | GET | 统计概览 |

---

### 5. 异常处理与响应

| 组件 | 说明 |
|---|---|
| ApiResponse\<T\> | 统一响应体 {code, message, data, timestamp} |
| PageResponse\<T\> | 分页响应体 |
| GlobalExceptionHandler | 全局异常捕获，覆盖业务异常、认证异常、参数校验异常 |
| BusinessException | 自定义业务异常（带错误码） |

---

### 6. 初始化

| 组件 | 功能 |
|---|---|
| DataInitializer | 启动时自动创建默认角色（USER/ADMIN）和默认管理员账号 |
| 默认管理员 | admin / admin123 |

---

## 前端实现功能 - 6.17

### 1. 技术栈与工具

| 库 | 作用 |
|---|---|
| Vue 3 | 框架 |
| Vite 8 | 构建工具 |
| Vue Router 4 | 路由 |
| Pinia | 状态管理 |
| Element Plus | UI 组件库（中文） |
| Axios | HTTP 请求 |

---

### 2. 路由设计

| 路径 | 页面 | 权限要求 |
|---|---|---|
| /auth/login | 登录页 | 游客可访问 |
| /auth/register | 注册页 | 游客可访问 |
| /dashboard | 工作台 | 需登录 |
| /user/profile | 个人中心 | 需登录 |
| /admin/users | 用户管理 | 需 ADMIN 角色 |

**路由守卫规则：**

- 自动拦截未登录请求，跳转至登录页
- 已登录用户访问 login/register 自动跳转到工作台

---

### 3. 页面功能

#### 登录页 (Login.vue)

| 功能 | 说明 |
|---|---|
| 表单验证 | 用户名 + 密码校验 |
| 登录跳转 | 成功后跳转至来源页或工作台 |
| 交互反馈 | 加载状态、错误提示 |

#### 注册页 (Register.vue)

| 功能 | 说明 |
|---|---|
| 表单字段 | 用户名、昵称、密码、确认密码、邮箱 |
| 密码校验 | 两次密码一致性校验 |
| 注册跳转 | 注册成功后跳转登录页 |

#### 工作台 (dashboard/Index.vue)

| 功能 | 说明 |
|---|---|
| 欢迎卡片 | 显示昵称、角色 |
| 统计信息 | 管理员可见用户总数 |
| 快速导航 | 快捷入口 |

#### 个人中心 (user/Profile.vue)

| 功能 | 说明 |
|---|---|
| 基本信息修改 | 昵称、邮箱、手机号 |
| 修改密码 | 验证原密码 → 成功后自动退出重新登录 |
| 注销账户 | 二次确认弹窗 |

#### 用户管理 (admin/Users.vue)

| 功能 | 说明 |
|---|---|
| 分页表格 | 展示所有用户 |
| 搜索 | 按用户名 / 昵称 / 邮箱 |
| 筛选 | 按角色、状态 |
| 启用/禁用 | 开关控制 |
| 修改角色 | 弹窗修改 |
| 删除用户 | 二次确认 |

---

### 4. 布局

| 布局 | 说明 |
|---|---|
| AuthLayout | 登录/注册页——深海蓝渐变背景 + 居中白色毛玻璃卡片 |
| DefaultLayout | 主界面——左侧深色侧边栏（可折叠）+ 顶栏（头像下拉菜单）+ 内容区 |

---

### 5. 状态管理 (stores/auth.js)

| 能力 | 说明 |
|---|---|
| 持久化 | Token 和用户信息存入 localStorage，刷新不丢失 |
| 状态 | isLoggedIn / isAdmin / user 实时响应 |
| 操作 | login / register / fetchUserInfo / logout |

---

### 6. 网络层 (api/request.js)

| 能力 | 说明 |
|---|---|
| 请求拦截 | 自动附加 Bearer Token |
| 响应拦截 | 统一错误处理（401 清空 Token 跳登录、403/500 提示） |
| 代理 | Vite 开发服务器代理 /api → localhost:8080 |

---

## 三、总结

| 模块 | 完成内容 |
|---|---|
| **后端** | ✅ 完成 Spring Boot 项目搭建，配置数据库连接、JWT 认证、安全拦截、跨域支持 |
| | ✅ 完成 3 张数据表设计（users / roles / user_roles）及实体类、Repository 层 |
| | ✅ 完成 JWT 工具类、认证过滤器、密码加密器 |
| | ✅ 完成 12 个 API 接口开发（认证 3 个 + 用户 4 个 + 管理 5 个） |
| | ✅ 完成全局异常处理、统一响应封装、分页响应 |
| | ✅ 完成启动初始化（默认角色 + 管理员账号） |
| **前端** | ✅ 完成 Vue 3 项目搭建，集成 Vue Router、Pinia、Element Plus、Axios |
| | ✅ 完成路由设计及守卫配置 |
| | ✅ 完成登录页、注册页开发 |
| | ✅ 完成工作台、个人中心开发 |
| | ✅ 完成用户管理页开发（分页 / 搜索 / 筛选 / 启用禁用 / 修改角色 / 删除） |
| | ✅ 完成布局组件（AuthLayout / DefaultLayout） |
| | ✅ 完成状态管理（登录/注册/获取信息/退出） |
| | ✅ 完成网络层封装（请求拦截 / 响应拦截 / 代理配置） |
| **联调** | 🔄 前后端接口对接测试中 |

---
