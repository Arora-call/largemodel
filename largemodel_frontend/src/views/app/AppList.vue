<template>
  <div class="app-list-page">
    <div class="page-header">
      <h2>我的应用</h2>
      <div class="header-actions">
        <el-input v-model="keyword" placeholder="搜索应用..." clearable size="small" style="width:220px" @keyup.enter="fetchList" />
        <el-select v-model="langFilter" placeholder="语言" clearable size="small" style="width:120px" @change="fetchList">
          <el-option label="全部" value="" />
          <el-option label="Vue" value="vue" />
          <el-option label="Java" value="java" />
          <el-option label="HTML" value="html" />
          <el-option label="Python" value="python" />
        </el-select>
      </div>
    </div>

    <div v-if="apps.length === 0 && !loading" class="empty">
      <p>还没有生成任何应用</p>
      <el-button type="primary" @click="$router.push('/ai/generate')">去生成</el-button>
    </div>

    <div v-else class="app-grid">
      <div v-for="app in apps" :key="app.id" class="app-card" @click="viewDetail(app)">
        <div class="card-cover" :style="{ background: app.coverImage || 'linear-gradient(135deg,#6366f1,#8b5cf6)' }">
          <span class="cover-lang">{{ app.language || app.type }}</span>
          <span v-if="app.type === 'ENGINEERING'" class="cover-badge">📦 工程</span>
        </div>
        <div class="card-body">
          <h4 class="card-name">{{ app.name }}</h4>
          <p class="card-desc">{{ app.description || '暂无描述' }}</p>
          <div class="card-actions">
            <button class="btn-dl" @click.stop="handleDownload(app)">下载</button>
            <button @click.stop="confirmDelete(app)" class="btn-del">删除</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="total > size" class="pagination">
      <el-pagination
        v-model:current-page="currentPage" :page-size="size"
        :total="total" layout="prev, pager, next" @current-change="fetchList" small />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detailApp?.name" width="700px" destroy-on-close>
      <div v-if="detailApp" class="detail-content">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="类型">{{ detailApp.type === 'ENGINEERING' ? '工程项目' : '原生应用' }}</el-descriptions-item>
          <el-descriptions-item label="语言">{{ detailApp.language || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailApp.createdAt?.substring(0,10) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detailApp.updatedAt?.substring(0,10) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 项目文件结构 -->
        <div v-if="projectFiles.length" class="proj-structure">
          <h5>📂 项目结构</h5>
          <div v-for="f in projectFiles" :key="f" class="file-item">{{ f }}</div>
        </div>

        <!-- 依赖列表 -->
        <div v-if="projectDeps.length" class="proj-deps">
          <h5>📦 依赖</h5>
          <el-tag v-for="d in projectDeps" :key="d" size="small" class="dep-tag">{{ d }}</el-tag>
        </div>

        <div class="code-preview">
          <CodeViewer
            v-if="detailApp?.sourceCode"
            :code="detailApp.sourceCode.substring(0, 5000)"
            :language="detailApp.language || 'text'"
            height="350px"
            max-height="350px"
          />
          <p v-else class="no-code">无代码</p>
        </div>
        <div class="detail-actions">
          <button class="btn-primary" @click="handleDownload(detailApp)">下载代码</button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApplications, getApplication, deleteApplication, downloadApplication } from '@/api/app'
import CodeViewer from '@/components/CodeViewer.vue'

const apps = ref([])
const loading = ref(false)
const total = ref(0)
const size = ref(12)
const currentPage = ref(1)
const keyword = ref('')
const langFilter = ref('')
const detailVisible = ref(false)
const detailApp = ref(null)
const projectFiles = computed(() => {
  try { return JSON.parse(detailApp.value?.configJson || '{}').files || [] } catch { return [] }
})
const projectDeps = computed(() => {
  try { return JSON.parse(detailApp.value?.configJson || '{}').dependencies || [] } catch { return [] }
})

async function handleDownload(app) {
  try {
    await downloadApplication(app.id, (app.name || 'code') + '.zip')
  } catch {
    ElMessage.error('下载失败，请检查网络或登录状态')
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listApplications({ page: currentPage.value - 1, size: size.value, keyword: keyword.value, language: langFilter.value })
    apps.value = res.data.content || []; total.value = res.data.totalElements || 0
  } catch { apps.value = []; total.value = 0 }
  finally { loading.value = false }
}

async function viewDetail(app) {
  try {
    const res = await getApplication(app.id)
    detailApp.value = res.data; detailVisible.value = true
  } catch { ElMessage.error('加载详情失败') }
}

async function confirmDelete(app) {
  try {
    await ElMessageBox.confirm(`确定删除「${app.name}」？`, '确认', { type: 'warning' })
    await deleteApplication(app.id)
    ElMessage.success('已删除')
    fetchList()
  } catch { /* cancelled */ }
}

onMounted(fetchList)
</script>

<style scoped>
.app-list-page { max-width:1200px }
.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:20px }
.page-header h2 { font-size:20px; font-weight:600; margin:0; color:#1a1a2e }
.header-actions { display:flex; gap:8px }
.empty { text-align:center; padding:60px 0; color:#999 }
.app-grid { display:grid; grid-template-columns:repeat(auto-fill, minmax(260px, 1fr)); gap:16px }
.app-card { background:#fff; border-radius:12px; overflow:hidden; cursor:pointer; transition:all .25s; border:1px solid #e5e7eb; box-shadow:0 1px 3px rgba(0,0,0,.04) }
.app-card:hover { box-shadow:0 8px 24px rgba(0,0,0,.1); transform:translateY(-3px) }
.card-cover { height:110px; background:linear-gradient(135deg,#5b6af0,#7c3aed); display:flex; align-items:flex-end; padding:10px 14px }
.cover-lang { background:rgba(255,255,255,.2); color:#fff; padding:2px 10px; border-radius:10px; font-size:12px; text-transform:uppercase }
.card-body { padding:14px 16px }
.card-name { font-size:15px; font-weight:600; margin:0 0 6px; color:#1f2937 }
.card-desc { font-size:13px; color:#9ca3af; margin:0 0 12px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis }
.card-actions { display:flex; gap:8px }
.btn-dl { padding:5px 14px; background:#5b6af0; color:#fff; border-radius:6px; font-size:12px; text-decoration:none; border:none; cursor:pointer; transition:all .15s }
.btn-dl:hover { background:#4f5de0; box-shadow:0 2px 8px rgba(91,106,240,.25) }
.btn-del { padding:5px 12px; background:transparent; color:#ef4444; border:1px solid #fecaca; border-radius:6px; font-size:12px; cursor:pointer; transition:all .15s }
.btn-del:hover { background:#fef2f2; border-color:#ef4444 }
.pagination { margin-top:20px; display:flex; justify-content:center }
.code-preview { background:#1e1e1e; border-radius:6px; margin-top:12px; overflow:hidden }
.code-preview .no-code { color:#999; padding:20px; text-align:center }
.detail-actions { margin-top:12px; text-align:right }
.btn-primary { display:inline-block; padding:8px 22px; background:#5b6af0; color:#fff; border-radius:8px; text-decoration:none; font-size:14px; transition:all .15s }
.btn-primary:hover { background:#4f5de0; box-shadow:0 4px 12px rgba(91,106,240,.25) }
.cover-badge { background:rgba(255,255,255,.2); color:#fff; padding:2px 10px; border-radius:10px; font-size:11px; margin-left:auto }
.proj-structure { margin-top:12px; background:#f9fafb; border-radius:8px; padding:10px 14px }
.proj-structure h5 { margin:0 0 8px; font-size:13px; color:#374151 }
.file-item { font-family:monospace; font-size:12px; color:#6b7280; padding:2px 0; padding-left:12px }
.proj-deps { margin-top:12px }
.proj-deps h5 { margin:0 0 8px; font-size:13px; color:#374151 }
.dep-tag { margin-right:6px; margin-bottom:6px }
</style>
