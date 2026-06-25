import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/auth',
    component: () => import('@/layouts/AuthLayout.vue'),
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/auth/Login.vue'),
        meta: { title: '登录', guest: true }
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/auth/Register.vue'),
        meta: { title: '注册', guest: true }
      },
      {
        path: 'forgot-password',
        name: 'ForgotPassword',
        component: () => import('@/views/auth/ForgotPassword.vue'),
        meta: { title: '找回密码', guest: true }
      }
    ]
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '工作台', requiresAuth: true }
      },
      // 旧路由 → 重定向到 AI 工作台
      {
        path: 'ai/generate',
        redirect: '/workspace',
        meta: { title: 'AI代码生成', requiresAuth: true }
      },
      {
        path: 'workspace',
        name: 'CodeGenWorkspace',
        component: () => import('@/views/ai/CodeGenWorkspace.vue'),
        meta: { title: 'AI 工作台', requiresAuth: true }
      },
      {
        path: 'user/profile',
        name: 'UserProfile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'app/list',
        name: 'AppList',
        component: () => import('@/views/app/AppList.vue'),
        meta: { title: '我的应用', requiresAuth: true }
      },
      // 旧路由 → 重定向到 AI 工作台
      {
        path: 'project/create',
        redirect: '/workspace',
        meta: { title: '创建项目', requiresAuth: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/models',
        name: 'AdminModels',
        component: () => import('@/views/admin/Models.vue'),
        meta: { title: '模型配置', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/Logs.vue'),
        meta: { title: '系统日志', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/applications',
        name: 'AdminApplications',
        component: () => import('@/views/admin/Applications.vue'),
        meta: { title: '应用管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/Index.vue'),
        meta: { title: '知识库', requiresAuth: true }
      },
      {
        path: 'agents',
        name: 'Agents',
        component: () => import('@/views/agents/Index.vue'),
        meta: { title: 'Agent 工作流', requiresAuth: true }
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/Index.vue'),
        meta: { title: '监控大盘', requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  document.title = to.meta.title ? `${to.meta.title} - CodeForge` : 'CodeForge'

  // 需要认证的页面
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  // 游客页面（已登录用户不显示登录/注册页）
  if (to.meta.guest && token) {
    next({ name: 'Dashboard' })
    return
  }

  // 需要管理员权限
  if (to.meta.requiresAdmin && (!user || user.role !== 'ADMIN')) {
    next({ name: 'Dashboard' })
    return
  }

  next()
})

export default router
