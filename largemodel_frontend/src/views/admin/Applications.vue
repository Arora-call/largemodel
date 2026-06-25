<template>
  <div class="admin-apps">
    <div class="page-header">
      <div>
        <h2 class="page-title">应用管理</h2>
        <p class="page-subtitle">查看和管理所有用户的应用</p>
      </div>
      <span class="total-badge" v-if="total > 0">共 {{ total }} 个应用</span>
    </div>

    <!-- Filter -->
    <div class="filter-bar glass-card">
      <div class="filter-row">
        <el-input
          v-model="keyword"
          placeholder="搜索应用名称..."
          clearable
          size="default"
          style="width: 240px"
          @keyup.enter="fetchList"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-select v-model="typeFilter" placeholder="全部类型" clearable size="default" style="width: 140px" @change="fetchList">
          <el-option label="单文件" value="SINGLE_FILE" />
          <el-option label="多文件" value="MULTI_FILE" />
          <el-option label="Vue3项目" value="VUE_PROJECT" />
        </el-select>

        <el-button type="primary" @click="fetchList">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- Table -->
    <div class="table-wrapper glass-card">
      <el-table
        v-loading="loading"
        :data="apps"
        border
        empty-text="暂无应用数据"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="应用名称" min-width="170">
          <template #default="{ row }">
            <span class="app-name-link">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="dark">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属用户" min-width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="20" class="user-avatar">
                {{ row.userName?.charAt(0)?.toUpperCase() || 'U' }}
              </el-avatar>
              <div>
                <span class="user-name-text">{{ row.userName }}</span>
                <span class="user-id-text">ID: {{ row.userId }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.priority >= 999" type="danger" size="small" effect="dark">📌 置顶</el-tag>
            <el-tag v-else-if="row.priority >= 99" type="warning" size="small" effect="dark">⭐ 精选</el-tag>
            <span v-else class="text-dim">-</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">
            {{ row.updatedAt?.substring(0, 16) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button size="small" text type="danger" @click="confirmDelete(row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="table-footer" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="size"
          :page-sizes="[12, 24, 48]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchList"
          @current-change="fetchList"
          small
        />
      </div>
    </div>

    <!-- Edit dialog -->
    <el-dialog v-model="editVisible" title="编辑应用" width="480px" top="20vh" destroy-on-close>
      <el-form v-if="editApp" @submit.prevent="handleEdit" label-position="top">
        <el-form-item label="应用名称">
          <el-input v-model="editName" placeholder="输入名称" maxlength="200" clearable />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editDesc" type="textarea" :rows="3" placeholder="应用描述" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="editType" style="width:100%">
            <el-option label="单文件" value="SINGLE_FILE" />
            <el-option label="多文件" value="MULTI_FILE" />
            <el-option label="Vue3项目" value="VUE_PROJECT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEdit" :disabled="!editName.trim()">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Edit, Delete } from '@element-plus/icons-vue'
import { listAllApplications, adminUpdateApplication, adminDeleteApplication } from '@/api/app'

const apps = ref([])
const loading = ref(false)
const total = ref(0)
const size = ref(12)
const currentPage = ref(1)
const keyword = ref('')
const typeFilter = ref('')

const editVisible = ref(false)
const editApp = ref(null)
const editName = ref('')
const editDesc = ref('')
const editType = ref('')

function typeLabel(type) {
  const map = { SINGLE_FILE: '单文件', MULTI_FILE: '多文件', VUE_PROJECT: 'Vue3项目' }
  return map[type] || type || '-'
}

function typeTagType(type) {
  const map = { SINGLE_FILE: 'success', MULTI_FILE: 'primary', VUE_PROJECT: '' }
  return map[type] || 'info'
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listAllApplications({
      page: currentPage.value - 1, size: size.value,
      keyword: keyword.value, type: typeFilter.value
    })
    apps.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch { apps.value = []; total.value = 0 }
  finally { loading.value = false }
}

function handleReset() {
  keyword.value = ''
  typeFilter.value = ''
  currentPage.value = 1
  fetchList()
}

function openEdit(row) {
  editApp.value = row
  editName.value = row.name
  editDesc.value = row.description || ''
  editType.value = row.type
  editVisible.value = true
}

async function handleEdit() {
  if (!editName.value.trim() || !editApp.value) return
  try {
    await adminUpdateApplication(editApp.value.id, {
      name: editName.value.trim(),
      description: editDesc.value,
      type: editType.value
    })
    ElMessage.success('已更新')
    editVisible.value = false
    fetchList()
  } catch { ElMessage.error('更新失败') }
}

async function confirmDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.name}」（用户: ${row.userName}）？`,
      '确认删除',
      { type: 'warning' }
    )
    await adminDeleteApplication(row.id)
    ElMessage.success('已删除')
    fetchList()
  } catch { /* cancelled */ }
}

onMounted(fetchList)
</script>

<style scoped>
.admin-apps {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-heading);
  margin: 0 0 4px;
}

.page-subtitle {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0;
}

.total-badge {
  font-size: 12px;
  color: var(--text-dim);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  padding: 4px 12px;
  border-radius: 12px;
  white-space: nowrap;
}

.filter-bar {
  margin-bottom: 16px;
  padding: 14px 18px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.table-wrapper {
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Deep override el-table dark styles */
.table-wrapper :deep(.el-table) {
  --el-table-bg-color: var(--bg-card);
  --el-table-tr-bg-color: var(--bg-card);
  --el-table-header-bg-color: var(--bg-secondary);
  --el-table-border-color: var(--border-color);
  --el-table-text-color: var(--text-primary);
  --el-table-header-text-color: var(--text-secondary);
  --el-table-row-hover-bg-color: var(--bg-hover);
  background: var(--bg-card);
  color: var(--text-primary);
}

.table-wrapper :deep(.el-table th.el-table__cell) {
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-color: var(--border-color);
}

.table-wrapper :deep(.el-table td.el-table__cell) {
  border-color: var(--border-color);
  color: var(--text-primary);
}

.table-wrapper :deep(.el-table__empty-text) {
  color: var(--text-dim);
}

.table-wrapper :deep(.el-loading-mask) {
  background: rgba(13, 17, 23, 0.6);
}

.app-name-link {
  font-weight: 500;
  color: var(--accent, #7c8aff);
  cursor: pointer;
}

.app-name-link:hover {
  text-decoration: underline;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  flex-shrink: 0;
  font-size: 11px;
}

.user-name-text {
  display: block;
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.2;
}

.user-id-text {
  font-size: 11px;
  color: var(--text-dim);
}

.text-dim { color: var(--text-dim); }

.table-footer {
  padding: 14px 20px;
  display: flex;
  justify-content: center;
  border-top: 1px solid var(--border-color);
  background: var(--bg-card);
}
</style>
