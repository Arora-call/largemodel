<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="sidebar-header">
        <div class="logo-icon">
          <span class="logo-dot"></span>
        </div>
        <transition name="fade">
          <span v-show="!isCollapse" class="logo-text">CodeForge</span>
        </transition>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :router="true"
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>

        <el-menu-item index="/workspace">
          <el-icon><MagicStick /></el-icon>
          <template #title>AI 工作台</template>
        </el-menu-item>

        <el-menu-item index="/app/list">
          <el-icon><FolderOpened /></el-icon>
          <template #title>我的应用</template>
        </el-menu-item>

        <el-menu-item index="/knowledge">
          <el-icon><Collection /></el-icon>
          <template #title>知识库</template>
        </el-menu-item>

        <el-menu-item index="/agents">
          <el-icon><Connection /></el-icon>
          <template #title>Agent 工作流</template>
        </el-menu-item>

        <el-menu-item index="/monitor">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>监控大盘</template>
        </el-menu-item>

        <el-menu-item index="/user/profile">
          <el-icon><User /></el-icon>
          <template #title>个人中心</template>
        </el-menu-item>

        <el-sub-menu v-if="authStore.isAdmin" index="admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/admin/users">
            <el-icon><UserFilled /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/models">
            <el-icon><Box /></el-icon>
            <template #title>模型配置</template>
          </el-menu-item>
          <el-menu-item index="/admin/logs">
            <el-icon><Tickets /></el-icon>
            <template #title>系统日志</template>
          </el-menu-item>
          <el-menu-item index="/admin/applications">
            <el-icon><FolderOpened /></el-icon>
            <template #title>应用管理</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

      <!-- 底部用户区域 -->
      <div class="sidebar-footer">
        <div class="user-mini" :class="{ collapsed: isCollapse }">
          <el-avatar :size="isCollapse ? 28 : 32" :src="avatarSrc">
            {{ (authStore.nickname || authStore.username).charAt(0).toUpperCase() }}
          </el-avatar>
          <div v-show="!isCollapse" class="user-meta">
            <span class="user-name">{{ authStore.nickname || authStore.username }}</span>
            <span class="user-role">{{ authStore.user?.roleDisplayName || authStore.userRole }}</span>
          </div>
        </div>
      </div>
    </el-aside>

    <!-- 主区域 -->
    <el-container class="main-area">
      <!-- 顶栏 -->
      <el-header class="topbar">
        <div class="topbar-left">
          <button class="collapse-btn" @click="toggleCollapse" :title="isCollapse ? '展开侧栏' : '收起侧栏'">
            <el-icon :size="18"><component :is="isCollapse ? Expand : Fold" /></el-icon>
          </button>
          <div class="breadcrumb">
            <span class="breadcrumb-item">{{ pageTitle }}</span>
          </div>
        </div>

        <div class="topbar-right">
          <el-dropdown trigger="click" @command="handleCommand" popper-class="user-dropdown">
            <div class="user-trigger">
              <el-avatar :size="30" :src="avatarSrc">
                {{ (authStore.nickname || authStore.username).charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username-text">{{ authStore.nickname || authStore.username }}</span>
              <el-icon class="caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <div class="dropdown-account-header">
                  <el-avatar :size="36" :src="avatarSrc">
                    {{ (authStore.nickname || authStore.username).charAt(0).toUpperCase() }}
                  </el-avatar>
                  <div>
                    <div class="dropdown-name">{{ authStore.nickname || authStore.username }}</div>
                    <div class="dropdown-role">{{ authStore.user?.roleDisplayName || authStore.userRole }}</div>
                  </div>
                </div>

                <!-- 已保存账号 -->
                <template v-if="otherAccounts.length > 0">
                  <el-dropdown-item disabled class="section-label">
                    已保存账号
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-for="account in otherAccounts"
                    :key="account.username"
                    class="account-item"
                  >
                    <div class="account-row" @click.stop="handleSwitchAccount(account)">
                      <el-avatar :size="22" style="margin-right:8px">
                        {{ (account.nickname || account.username).charAt(0).toUpperCase() }}
                      </el-avatar>
                      <span>{{ account.nickname || account.username }}</span>
                    </div>
                    <el-icon
                      class="account-remove"
                      title="移除"
                      @click.stop="handleRemoveAccount(account.username)"
                    ><Close /></el-icon>
                  </el-dropdown-item>
                </template>

                <el-dropdown-item divided command="addAccount">
                  <el-icon><Plus /></el-icon>添加账号
                </el-dropdown-item>

                <el-dropdown-item divided command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="content-area">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Odometer, User, UserFilled, Setting,
  Fold, Expand, SwitchButton, MagicStick, FolderOpened, FolderAdd,
  Plus, Close, ArrowDown, Collection, Connection, DataAnalysis,
  Box, Tickets
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isCollapse = ref(true)

const activeMenu = computed(() => route.path)

// Page title from route meta
const pageTitle = computed(() => {
  const titles = {
    '/dashboard': '工作台',
    '/workspace': 'AI 工作台',
    '/app/list': '我的应用',
    '/knowledge': '知识库',
    '/agents': 'Agent 工作流',
    '/monitor': '监控大盘',
    '/user/profile': '个人中心',
    '/admin/users': '用户管理',
    '/admin/models': '模型配置',
    '/admin/logs': '系统日志',
    '/admin/applications': '应用管理'
  }
  return titles[route.path] || route.meta.title || ''
})

const avatarSrc = computed(() => {
  const avatar = authStore.user?.avatar
  if (!avatar) return ''
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) return avatar
  return avatar
})

const otherAccounts = computed(() =>
  authStore.savedAccounts.filter(a => a.username !== authStore.username)
)

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

function handleCommand(command) {
  if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'logout') {
    authStore.logout()
    router.push('/auth/login')
  } else if (command === 'addAccount') {
    authStore.saveCurrentAccount()
    router.push('/auth/login')
  }
}

function handleSwitchAccount(account) {
  authStore.switchToAccount(account)
  ElMessage.success(`已切换到账号：${account.nickname || account.username}`)
  location.reload()
}

async function handleRemoveAccount(username) {
  try {
    await ElMessageBox.confirm(
      `确定要移除账号「${username}」吗？`,
      '移除账号',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const wasCurrent = authStore.removeAccount(username)
    ElMessage.success('账号已移除')
    if (wasCurrent) location.reload()
  } catch { /* cancelled */ }
}
</script>

<style scoped>
/* ====== Layout ====== */
.app-layout {
  height: 100vh;
  background: var(--bg-primary);
}

/* ====== Sidebar ====== */
.sidebar {
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  transition: width 0.3s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid var(--border-color);
}

.logo-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--accent-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(124, 138, 255, 0.3);
}

.logo-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #fff;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-heading);
  letter-spacing: -0.5px;
  white-space: nowrap;
}

/* ====== Sidebar Menu ====== */
.sidebar-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 8px;
}

.sidebar-menu:deep(.el-menu-item),
.sidebar-menu:deep(.el-sub-menu__title) {
  border-radius: var(--radius);
  margin-bottom: 2px;
  color: var(--text-secondary);
  transition: all var(--transition);
}

.sidebar-menu:deep(.el-menu-item:hover),
.sidebar-menu:deep(.el-sub-menu__title:hover) {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.sidebar-menu:deep(.el-menu-item.is-active) {
  background: var(--accent-bg);
  color: var(--accent);
  border-right: none;
  font-weight: 500;
}

.sidebar-menu:deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--accent);
}

.sidebar-menu:deep(.el-menu--inline) {
  background: transparent;
}

/* ====== Sidebar Footer ====== */
.sidebar-footer {
  padding: 10px 12px;
  border-top: 1px solid var(--border-color);
}

.user-mini {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: var(--radius);
  transition: background var(--transition);
}

.user-mini:hover {
  background: var(--bg-hover);
}

.user-mini.collapsed {
  justify-content: center;
  padding: 6px 0;
}

.user-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 11px;
  color: var(--text-dim);
}

/* ====== Main Area ====== */
.main-area {
  flex-direction: column;
  min-width: 0;
}

/* ====== Topbar ====== */
.topbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: rgba(13, 17, 23, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition);
  padding: 0;
}

.collapse-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
  border-color: var(--border-hover);
}

.breadcrumb-item {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

/* ====== Topbar Right ====== */
.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  color: var(--text-dim);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition);
}

.search-trigger:hover {
  border-color: var(--border-hover);
  color: var(--text-secondary);
}

.search-trigger kbd {
  display: inline-block;
  padding: 1px 6px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  font-size: 11px;
  font-family: inherit;
  color: var(--text-dim);
  margin-left: auto;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  border-radius: 24px;
  background: transparent;
  cursor: pointer;
  transition: background var(--transition);
}

.user-trigger:hover {
  background: var(--bg-hover);
}

.username-text {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
}

.caret {
  font-size: 12px;
  color: var(--text-dim);
}

/* ====== Dropdown ====== */
.dropdown-account-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  margin: -4px -4px 6px;
  background: var(--bg-hover);
  border-radius: var(--radius-sm);
}

.dropdown-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.dropdown-role {
  font-size: 11px;
  color: var(--text-dim);
}

.section-label {
  font-size: 11px !important;
  color: var(--text-dim) !important;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.account-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.account-row {
  display: flex;
  align-items: center;
  flex: 1;
  cursor: pointer;
}

.account-row:hover {
  color: var(--accent);
}

.account-remove {
  font-size: 13px;
  color: var(--text-dim);
  cursor: pointer;
  transition: color var(--transition);
}

.account-remove:hover {
  color: var(--danger);
}

/* ====== Content ====== */
.content-area {
  background: var(--bg-primary);
  padding: 24px;
  overflow-y: auto;
  min-height: 0;
}

/* ====== Transitions ====== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ====== Collapse ====== */
.sidebar-menu:deep(.el-menu--collapse) {
  width: 100%;
}

.sidebar-menu:deep(.el-menu--collapse .el-menu-item),
.sidebar-menu:deep(.el-menu--collapse .el-sub-menu__title) {
  padding: 0 !important;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
