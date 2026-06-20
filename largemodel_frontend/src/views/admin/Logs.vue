<template>
  <div class="logs-page">
    <h1 class="page-title">📋 系统日志</h1>
    <p class="section-subtitle">查看 API 调用记录、错误日志与系统事件</p>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="filterLevel" placeholder="日志级别" style="width: 140px" clearable>
        <el-option label="全部" value="" />
        <el-option label="INFO" value="INFO" />
        <el-option label="WARN" value="WARN" />
        <el-option label="ERROR" value="ERROR" />
      </el-select>
      <el-select v-model="filterModule" placeholder="服务模块" style="width: 160px" clearable>
        <el-option label="全部" value="" />
        <el-option label="认证服务" value="auth" />
        <el-option label="代码服务" value="code" />
        <el-option label="知识库" value="knowledge" />
        <el-option label="Agent" value="agent" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 260px"
      />
      <el-input
        v-model="searchKeyword"
        placeholder="搜索日志内容..."
        prefix-icon="Search"
        style="width: 240px"
        clearable
      />
      <el-button type="primary" :icon="Search">搜索</el-button>
      <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
    </div>

    <!-- 日志表格 -->
    <el-card class="table-card">
      <el-table :data="logs" style="width: 100%" stripe>
        <el-table-column prop="timestamp" label="时间" width="180">
          <template #default="{ row }">
            <span class="log-time">{{ row.timestamp }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)" size="small" effect="dark">
              {{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="message" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="user" label="用户" width="120" />
      </el-table>

      <div class="empty-state">
        <el-icon :size="48"><Tickets /></el-icon>
        <p style="margin-top: 12px; font-size: 15px; color: var(--text-secondary)">
          系统日志功能即将上线
        </p>
        <p>接入日志采集系统后可查看详细的调用链路与错误追踪信息</p>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[20, 50, 100]"
          :total="0"
          layout="total, sizes, prev, pager, next"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Search, Refresh, Tickets } from '@element-plus/icons-vue'

const filterLevel = ref('')
const filterModule = ref('')
const dateRange = ref(null)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const logs = ref([])

function levelTag(level) {
  const map = { INFO: 'info', WARN: 'warning', ERROR: 'danger' }
  return map[level] || 'info'
}

function handleRefresh() {
  // TODO: 刷新日志
}
</script>

<style scoped>
.logs-page {
  max-width: 1400px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
}

.table-card {
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
}

.log-time {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-secondary);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
  }
  .filter-bar > * {
    width: 100% !important;
  }
}
</style>
