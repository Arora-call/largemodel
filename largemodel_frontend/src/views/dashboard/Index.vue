<template>
  <div class="dashboard">
    <!-- 欢迎区 -->
    <div class="welcome-banner glass-card">
      <div class="welcome-content">
        <h1 class="welcome-greeting">
          你好，<span class="highlight">{{ authStore.nickname || authStore.username }}</span>
        </h1>
        <p class="welcome-date">{{ todayDate }}</p>
      </div>
      <div class="welcome-decoration">
        <div class="deco-orb"></div>
        <div class="deco-orb small"></div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card stat-projects">
        <div class="stat-icon">
          <el-icon :size="22"><FolderOpened /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.totalApps ?? '-' }}</span>
          <span class="stat-label">我的项目</span>
        </div>
      </div>

      <div class="stat-card stat-generate">
        <div class="stat-icon">
          <el-icon :size="22"><MagicStick /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.totalConversations ?? '-' }}</span>
          <span class="stat-label">对话次数</span>
        </div>
      </div>

      <div class="stat-card stat-role">
        <div class="stat-icon">
          <el-icon :size="22"><User /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ authStore.user?.roleDisplayName || authStore.userRole }}</span>
          <span class="stat-label">当前角色</span>
        </div>
      </div>

      <div v-if="authStore.isAdmin" class="stat-card stat-users">
        <div class="stat-icon">
          <el-icon :size="22"><UserFilled /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.totalUsers ?? '-' }}</span>
          <span class="stat-label">用户总数</span>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="section">
      <h3 class="section-title">快捷操作</h3>
      <div class="quick-actions">
        <button class="action-card" @click="$router.push('/ai/generate')">
          <div class="action-icon ai-icon">
            <el-icon :size="20"><MagicStick /></el-icon>
          </div>
          <div class="action-text">
            <span class="action-name">AI 代码生成</span>
            <span class="action-desc">用自然语言描述，AI 帮你写代码</span>
          </div>
          <el-icon class="action-arrow"><ArrowRight /></el-icon>
        </button>

        <button class="action-card" @click="$router.push('/project/create')">
          <div class="action-icon project-icon">
            <el-icon :size="20"><FolderAdd /></el-icon>
          </div>
          <div class="action-text">
            <span class="action-name">创建项目</span>
            <span class="action-desc">生成完整的多文件工程项目</span>
          </div>
          <el-icon class="action-arrow"><ArrowRight /></el-icon>
        </button>

        <button class="action-card" @click="$router.push('/app/list')">
          <div class="action-icon apps-icon">
            <el-icon :size="20"><FolderOpened /></el-icon>
          </div>
          <div class="action-text">
            <span class="action-name">我的应用</span>
            <span class="action-desc">查看和管理已生成的应用</span>
          </div>
          <el-icon class="action-arrow"><ArrowRight /></el-icon>
        </button>

        <button class="action-card" @click="$router.push('/knowledge')">
          <div class="action-icon knowledge-icon">
            <el-icon :size="20"><Collection /></el-icon>
          </div>
          <div class="action-text">
            <span class="action-name">知识库</span>
            <span class="action-desc">管理文档知识，增强 AI 上下文</span>
          </div>
          <el-icon class="action-arrow"><ArrowRight /></el-icon>
        </button>

        <button class="action-card" @click="$router.push('/agents')">
          <div class="action-icon agent-icon">
            <el-icon :size="20"><Connection /></el-icon>
          </div>
          <div class="action-text">
            <span class="action-name">Agent 工作流</span>
            <span class="action-desc">多 Agent 协作完成复杂任务</span>
          </div>
          <el-icon class="action-arrow"><ArrowRight /></el-icon>
        </button>
      </div>
    </div>

    <!-- 最近项目 -->
    <div v-if="recentApps.length > 0" class="section">
      <h3 class="section-title">最近项目</h3>
      <div class="recent-list">
        <div
          v-for="app in recentApps"
          :key="app.id"
          class="recent-item"
          @click="$router.push('/app/list')"
        >
          <div class="recent-left">
            <span class="recent-lang-tag">{{ app.language || 'code' }}</span>
            <span class="recent-name">{{ app.name }}</span>
          </div>
          <div class="recent-right">
            <span class="recent-date">{{ formatDate(app.updatedAt) }}</span>
            <el-icon class="recent-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getDashboardStats, listApplications } from '@/api/app'
import { listConversations } from '@/api/app'
import {
  FolderOpened, MagicStick, User, UserFilled,
  FolderAdd, ArrowRight, Collection, Connection
} from '@element-plus/icons-vue'

const authStore = useAuthStore()
const stats = ref({})
const recentApps = ref([])

const todayDate = computed(() => {
  return new Date().toLocaleDateString('zh-CN', {
    year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
  })
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data || {}
  } catch { stats.value = {} }

  try {
    const res = await listApplications({ page: 0, size: 5 })
    recentApps.value = res.data?.content?.slice(0, 5) || []
  } catch { recentApps.value = [] }
})
</script>

<style scoped>
.dashboard {
  max-width: 900px;
  margin: 0 auto;
}

/* ====== Welcome Banner ====== */
.welcome-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px 32px;
  margin-bottom: 28px;
  position: relative;
  overflow: hidden;
}

.welcome-content {
  position: relative;
  z-index: 1;
}

.welcome-greeting {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-heading);
  margin: 0 0 6px;
}

.highlight {
  background: linear-gradient(135deg, var(--accent), #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-date {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0;
}

.welcome-decoration {
  position: relative;
  width: 80px;
  height: 80px;
}

.deco-orb {
  position: absolute;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(124,138,255,0.2), rgba(167,139,250,0.1));
  top: 10px;
  right: 10px;
  filter: blur(20px);
}

.deco-orb.small {
  width: 30px;
  height: 30px;
  top: 0;
  right: 40px;
  background: rgba(124,138,255,0.15);
}

/* ====== Stats Grid ====== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all var(--transition);
}

.stat-card:hover {
  border-color: var(--border-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-projects .stat-icon {
  background: rgba(245, 87, 108, 0.1);
  color: #f5576c;
}

.stat-generate .stat-icon {
  background: var(--accent-bg);
  color: var(--accent);
}

.stat-role .stat-icon {
  background: rgba(52, 211, 153, 0.1);
  color: var(--success);
}

.stat-users .stat-icon {
  background: rgba(251, 191, 36, 0.1);
  color: var(--warning);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-heading);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 2px;
}

/* ====== Section ====== */
.section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-heading);
  margin: 0 0 14px;
}

/* ====== Quick Actions ====== */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 18px 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition);
  color: var(--text-primary);
  font-family: inherit;
}

.action-card:hover {
  border-color: var(--border-hover);
  background: var(--bg-hover);
  transform: translateX(4px);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-icon { background: var(--accent-bg); color: var(--accent); }
.project-icon { background: rgba(245, 87, 108, 0.1); color: #f5576c; }
.apps-icon { background: rgba(52, 211, 153, 0.1); color: var(--success); }
.knowledge-icon { background: rgba(251, 191, 36, 0.1); color: var(--warning); }
.agent-icon { background: rgba(239, 68, 68, 0.1); color: var(--danger); }

.action-text {
  display: flex;
  flex-direction: column;
  text-align: left;
  flex: 1;
}

.action-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.action-desc {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 2px;
}

.action-arrow {
  color: var(--text-dim);
  font-size: 16px;
  transition: transform var(--transition);
}

.action-card:hover .action-arrow {
  transform: translateX(4px);
  color: var(--accent);
}

/* ====== Recent List ====== */
.recent-list {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  cursor: pointer;
  transition: background var(--transition);
}

.recent-item:hover {
  background: var(--bg-hover);
}

.recent-item + .recent-item {
  border-top: 1px solid var(--border-color);
}

.recent-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recent-lang-tag {
  padding: 2px 10px;
  background: var(--accent-bg);
  color: var(--accent);
  border-radius: 10px;
  font-size: 11px;
  text-transform: uppercase;
  font-weight: 500;
}

.recent-name {
  font-size: 14px;
  color: var(--text-primary);
}

.recent-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recent-date {
  font-size: 12px;
  color: var(--text-dim);
}

.recent-arrow {
  color: var(--text-dim);
  font-size: 14px;
}
</style>
