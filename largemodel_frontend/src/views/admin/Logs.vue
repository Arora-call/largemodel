<template>
  <div class="logs-page">
    <h1 class="page-title">📋 系统日志</h1>
    <p class="section-subtitle">查看管理员操作记录与系统事件</p>

    <div class="filter-bar">
      <el-select v-model="filterLevel" placeholder="模块" style="width:140px" clearable @change="search">
        <el-option label="全部" value="" />
        <el-option label="ADMIN" value="admin" />
        <el-option label="AUTH" value="auth" />
        <el-option label="CODE" value="code" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索用户名或内容..." style="width:220px" clearable @keyup.enter="search" />
      <el-button type="primary" @click="search">搜索</el-button>
    </div>

    <el-card class="table-card">
      <el-table :data="logs" style="width:100%" v-loading="loading" stripe empty-text="暂无操作记录">
        <el-table-column prop="timestamp" label="时间" width="170" />
        <el-table-column prop="level" label="模块" width="80">
          <template #default="{ row }">
            <el-tag :type="row.level === 'AUTH' ? 'warning' : row.level === 'ERROR' ? 'danger' : 'info'" size="small" effect="dark">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="分类" width="100" />
        <el-table-column prop="message" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column prop="user" label="操作人" width="120" />
      </el-table>
      <div v-if="total > 20" class="pagination-wrapper">
        <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" @current-change="fetchList" small background />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOperationLogs } from '@/api/monitor'

const logs = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const filterLevel = ref('')
const keyword = ref('')

async function fetchList() {
  loading.value = true
  try {
    const res = await getOperationLogs({ page: page.value - 1, size: 20, level: filterLevel.value, keyword: keyword.value })
    const data = res.data || {}
    logs.value = data.content || []
    total.value = data.totalElements || 0
  } catch { logs.value = [] }
  finally { loading.value = false }
}

function search() { page.value = 1; fetchList() }
onMounted(fetchList)
</script>

<style scoped>
.logs-page { max-width: 1400px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.table-card { background: var(--bg-card) !important; border-color: var(--border-color) !important; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--border-color); }
</style>
