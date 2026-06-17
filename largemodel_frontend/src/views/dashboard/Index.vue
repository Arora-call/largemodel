<template>
  <div class="dashboard">
    <h2 class="page-title">工作台</h2>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">欢迎回来</div>
              <div class="stat-value">{{ authStore.nickname || authStore.username }}</div>
            </div>
            <el-icon class="stat-icon" :size="48" color="#409eff"><UserFilled /></el-icon>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">角色</div>
              <div class="stat-value">{{ authStore.user?.roleDisplayName || authStore.userRole }}</div>
            </div>
            <el-icon class="stat-icon" :size="48" color="#67c23a"><Opportunity /></el-icon>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card v-if="authStore.isAdmin" shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">用户总数</div>
              <div class="stat-value">{{ stats.totalUsers ?? '-' }}</div>
            </div>
            <el-icon class="stat-icon" :size="48" color="#e6a23c"><User /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="welcome-card" shadow="hover">
      <template #header>
        <span>快速开始</span>
      </template>
      <div class="welcome-content">
        <p>欢迎使用大模型代码应用生成平台！</p>
        <ul>
          <li>完善个人信息 - 前往 <router-link to="/user/profile">个人中心</router-link></li>
          <li v-if="authStore.isAdmin">管理用户 - 前往 <router-link to="/admin/users">用户管理</router-link></li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getStats } from '@/api/admin'
import { UserFilled, User, Opportunity } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const stats = ref({})

onMounted(async () => {
  if (authStore.isAdmin) {
    try {
      const res = await getStats()
      stats.value = res.data || {}
    } catch {
      // ignore
    }
  }
})
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 20px;
  color: #1a1a2e;
}

.stat-card {
  margin-bottom: 20px;
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.welcome-card {
  margin-top: 8px;
}

.welcome-content p {
  margin-top: 0;
  color: #666;
}

.welcome-content ul {
  padding-left: 20px;
}

.welcome-content li {
  margin-bottom: 8px;
  color: #666;
}

.welcome-content a {
  color: #409eff;
  text-decoration: none;
}

.welcome-content a:hover {
  text-decoration: underline;
}
</style>
