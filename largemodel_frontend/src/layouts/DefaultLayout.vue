<template>
  <el-container class="default-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="sidebar-header">
        <span v-show="!isCollapse" class="logo-text">大模型平台</span>
        <span v-show="isCollapse" class="logo-text-short">LM</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :router="true"
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#fff"
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>

        <el-menu-item index="/ai/generate">
          <el-icon><MagicStick /></el-icon>
          <template #title>AI 代码生成</template>
        </el-menu-item>

        <el-menu-item index="/project/create">
          <el-icon><FolderAdd /></el-icon>
          <template #title>创建项目</template>
        </el-menu-item>

        <el-menu-item index="/app/list">
          <el-icon><FolderOpened /></el-icon>
          <template #title>我的应用</template>
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
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 主区域 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-button :icon="isCollapse ? Expand : Fold" text @click="toggleCollapse" />
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="avatarSrc">
                {{ (authStore.nickname || authStore.username).charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username">{{ authStore.nickname || authStore.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <!-- 当前活跃账号 -->
                <el-dropdown-item disabled class="account-header-item">
                  <span class="account-tag">当前</span>
                  {{ authStore.nickname || authStore.username }}
                </el-dropdown-item>

                <!-- 已保存的其他账号 -->
                <template v-if="otherAccounts.length > 0">
                  <el-dropdown-item divided disabled>
                    <span style="color: #999; font-size: 12px">已保存账号</span>
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-for="account in otherAccounts"
                    :key="account.username"
                    class="account-item"
                  >
                    <div class="account-row" @click.stop="handleSwitchAccount(account)">
                      <span>{{ account.nickname || account.username }}</span>
                    </div>
                    <el-icon
                      class="account-remove"
                      title="移除账号"
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
                  <el-icon><SwitchButton /></el-icon>退出登录（当前账号）
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
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
  Plus, Close
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isCollapse = ref(false)

const activeMenu = computed(() => route.path)

// 当前用户头像（支持自定义头像）
const avatarSrc = computed(() => {
  const avatar = authStore.user?.avatar
  if (!avatar) return ''
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) return avatar
  return avatar
})

// 非当前活跃的其他已保存账号
const otherAccounts = computed(() => {
  return authStore.savedAccounts.filter(
    a => a.username !== authStore.username
  )
})

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
    // 保存当前账号后跳转登录页
    authStore.saveCurrentAccount()
    router.push('/auth/login')
  }
}

// 切换到指定账号
function handleSwitchAccount(account) {
  authStore.switchToAccount(account)
  ElMessage.success(`已切换到账号：${account.nickname || account.username}`)
  // 刷新页面以确保所有状态（对话、生成等）使用新账号数据
  location.reload()
}

// 移除已保存的账号
async function handleRemoveAccount(username) {
  try {
    await ElMessageBox.confirm(
      `确定要移除账号「${username}」吗？移除后需要重新登录。`,
      '移除账号',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    const wasCurrent = authStore.removeAccount(username)
    if (wasCurrent) {
      ElMessage.success('账号已移除')
      location.reload()
    } else {
      ElMessage.success('账号已移除')
    }
  } catch {
    // cancelled
  }
}
</script>

<style scoped>
.default-layout {
  height: 100vh;
}

.sidebar {
  background-color: #001529;
  transition: width 0.3s;
  overflow: hidden;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  white-space: nowrap;
}

.logo-text-short {
  font-size: 16px;
}

.sidebar-menu {
  border-right: none;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 16px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #333;
}

.main-content {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

/* 账号切换 */
.account-header-item {
  font-weight: 600;
  cursor: default !important;
}

.account-tag {
  display: inline-block;
  background: #409eff;
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  margin-right: 6px;
  vertical-align: middle;
}

.account-item {
  justify-content: space-between;
}

.account-row {
  flex: 1;
  cursor: pointer;
}

.account-row:hover {
  color: #409eff;
}

.account-remove {
  font-size: 14px;
  color: #c0c4cc;
  cursor: pointer;
  transition: color 0.2s;
}

.account-remove:hover {
  color: #f56c6c;
}
</style>
