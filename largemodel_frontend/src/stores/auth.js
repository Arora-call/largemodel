import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, getCurrentUser } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const savedAccounts = ref(JSON.parse(localStorage.getItem('savedAccounts') || '[]'))

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const username = computed(() => user.value?.username || '')
  const nickname = computed(() => user.value?.nickname || '')
  const userRole = computed(() => user.value?.role || '')
  const hasSavedAccounts = computed(() => savedAccounts.value.length > 1)

  // ==================== Actions ====================

  async function login(loginData) {
    const res = await loginApi(loginData)
    const { token: jwtToken, user: userInfo } = res.data

    token.value = jwtToken
    user.value = userInfo

    localStorage.setItem('token', jwtToken)
    localStorage.setItem('user', JSON.stringify(userInfo))

    // 登录成功后自动保存到账号列表
    saveCurrentAccount()

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

  function logout(keepOthers = true) {
    // 从 savedAccounts 中移除当前账号
    if (keepOthers && user.value?.username) {
      removeAccountSilent(user.value.username)
    }

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
    // 同步 savedAccounts
    const accounts = localStorage.getItem('savedAccounts')
    if (accounts) savedAccounts.value = JSON.parse(accounts)
  }

  // ==================== 多账号切换 ====================

  /**
   * 保存当前账号到列表（按 username 去重 + 更新 token）
   */
  function saveCurrentAccount() {
    if (!user.value || !token.value) return

    const currentUsername = user.value.username
    const entry = {
      username: currentUsername,
      nickname: user.value.nickname || currentUsername,
      token: token.value,
      user: user.value
    }

    const idx = savedAccounts.value.findIndex(a => a.username === currentUsername)
    if (idx >= 0) {
      savedAccounts.value[idx] = entry
    } else {
      savedAccounts.value.push(entry)
    }

    _persistAccounts()
  }

  /**
   * 切换到指定账号
   * @param {Object} account - { username, token, user }
   */
  function switchToAccount(account) {
    if (!account || !account.token) return

    // 先保存当前账号的最新状态
    saveCurrentAccount()

    // 加载目标账号
    token.value = account.token
    user.value = account.user

    localStorage.setItem('token', account.token)
    localStorage.setItem('user', JSON.stringify(account.user))
  }

  /**
   * 从列表中移除指定账号
   * @param {string} username
   * @returns {boolean} 是否移除了当前活跃账号
   */
  function removeAccount(username) {
    const isCurrent = user.value?.username === username

    savedAccounts.value = savedAccounts.value.filter(a => a.username !== username)
    _persistAccounts()

    if (isCurrent) {
      // 切换到列表中的下一个账号，或多个账号时回退
      if (savedAccounts.value.length > 0) {
        switchToAccount(savedAccounts.value[0])
      } else {
        logout(false)
      }
    }

    return isCurrent
  }

  /**
   * 静默移除（不清除 localStorage token/user，由调用方处理）
   */
  function removeAccountSilent(username) {
    savedAccounts.value = savedAccounts.value.filter(a => a.username !== username)
    _persistAccounts()
  }

  /**
   * 判断账号是否已在列表中
   */
  function isAccountSaved(username) {
    return savedAccounts.value.some(a => a.username === username)
  }

  function _persistAccounts() {
    localStorage.setItem('savedAccounts', JSON.stringify(savedAccounts.value))
  }

  return {
    token,
    user,
    savedAccounts,
    isLoggedIn,
    isAdmin,
    username,
    nickname,
    userRole,
    hasSavedAccounts,
    login,
    register,
    fetchUserInfo,
    setUser,
    logout,
    initFromStorage,
    saveCurrentAccount,
    switchToAccount,
    removeAccount,
    isAccountSaved
  }
})
