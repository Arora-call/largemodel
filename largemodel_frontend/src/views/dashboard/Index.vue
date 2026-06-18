<template>
  <div class="dashboard">
    <h2 class="page-title">工作台</h2>

    <div class="stats-grid">
      <div class="stat-card card-welcome">
        <div class="stat-num">{{ authStore.nickname || authStore.username }}</div>
        <div class="stat-label">欢迎回来</div>
      </div>
      <div class="stat-card card-projects">
        <div class="stat-num">{{ stats.totalApps ?? '-' }}</div>
        <div class="stat-label">我的项目</div>
      </div>
      <div class="stat-card card-role">
        <div class="stat-num">{{ authStore.user?.roleDisplayName || authStore.userRole }}</div>
        <div class="stat-label">当前角色</div>
      </div>
      <div v-if="authStore.isAdmin" class="stat-card card-users">
        <div class="stat-num">{{ stats.totalUsers ?? '-' }}</div>
        <div class="stat-label">用户总数</div>
      </div>
    </div>

    <div class="quick-row">
      <el-button type="primary" size="large" @click="$router.push('/ai/generate')">
        ✨ 生成代码
      </el-button>
      <el-button size="large" @click="$router.push('/app/list')">
        📁 我的应用
      </el-button>
      <el-button size="large" @click="$router.push('/project/create')">
        🏗️ 创建项目
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getDashboardStats } from '@/api/app'

const authStore = useAuthStore()
const stats = ref({})

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data || {}
  } catch { stats.value = {} }
})
</script>

<style scoped>
.dashboard { max-width: 900px; margin:0 auto }
.page-title { font-size: 24px; font-weight: 700; margin: 0 0 24px; color: #1f2937 }

.stats-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; margin-bottom: 28px }
.stat-card { padding: 20px 22px; border-radius: 14px; color: #fff; box-shadow: 0 4px 16px rgba(0,0,0,.08) }
.card-welcome { background: linear-gradient(135deg, #667eea, #764ba2) }
.card-projects { background: linear-gradient(135deg, #f093fb, #f5576c) }
.card-role { background: linear-gradient(135deg, #4facfe, #00f2fe) }
.card-users { background: linear-gradient(135deg, #43e97b, #38f9d7) }
.stat-num { font-size: 28px; font-weight: 700; margin-bottom: 4px }
.stat-label { font-size: 13px; opacity: 0.85 }

.quick-row { display: flex; gap: 12px; flex-wrap: wrap }
.quick-row .el-button { border-radius: 10px; padding: 14px 24px; font-size: 15px; font-weight: 500 }
</style>
