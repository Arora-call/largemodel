import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, getCurrentUser } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const username = computed(() => user.value?.username || '')
  const nickname = computed(() => user.value?.nickname || '')
  const userRole = computed(() => user.value?.role || '')

  // Actions
  async function login(loginData) {
    const res = await loginApi(loginData)
    const { token: jwtToken, user: userInfo } = res.data

    token.value = jwtToken
    user.value = userInfo

    localStorage.setItem('token', jwtToken)
    localStorage.setItem('user', JSON.stringify(userInfo))

    return userInfo
  }

  async function register(registerData) {
    const res = await registerApi(registerData)
    return res
  }

  async function fetchUserInfo() {
    try {
      const res = await getCurrentUser()
      user.value = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
      return res.data
    } catch {
      logout()
      return null
    }
  }

  function setUser(userInfo) {
    user.value = userInfo
    localStorage.setItem('user', JSON.stringify(userInfo))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function initFromStorage() {
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')
    if (savedToken) token.value = savedToken
    if (savedUser) user.value = JSON.parse(savedUser)
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    username,
    nickname,
    userRole,
    login,
    register,
    fetchUserInfo,
    setUser,
    logout,
    initFromStorage
  }
})
