import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/Login.vue')
  },
  {
    path: '/',
    component: () => import('@/pages/Dashboard.vue'),
    name: 'Dashboard'
  },
  {
    path: '/users',
    component: () => import('@/pages/Users.vue'),
    name: 'Users'
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router