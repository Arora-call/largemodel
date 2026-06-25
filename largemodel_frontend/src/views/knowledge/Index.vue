<template>
  <div class="knowledge-page">
    <div class="page-container">
      <!-- 头部 -->
      <div class="page-header">
        <div>
          <h2>📚 知识库</h2>
          <p class="subtitle">上传文档，构建你的 AI 知识库</p>
        </div>
        <div class="header-actions">
          <el-button @click="searchVisible = true">🔍 语义搜索</el-button>
          <el-button type="primary" @click="uploadVisible = true">
            <el-icon><Upload /></el-icon> 上传文档
          </el-button>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-num">{{ stats.totalDocuments || 0 }}</span>
          <span class="stat-label">文档总数</span>
        </div>
        <div class="stat-card">
          <span class="stat-num">{{ stats.totalCollections || 0 }}</span>
          <span class="stat-label">集合数</span>
        </div>
      </div>

      <!-- 搜索 + 筛选 -->
      <div class="toolbar">
        <el-input v-model="searchQuery" placeholder="搜索文档..." clearable size="default"
          style="width: 300px" @keyup.enter="fetchList" @clear="fetchList">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filterCollection" placeholder="全部集合" clearable size="default"
          style="width: 160px">
          <el-option v-for="c in collections" :key="c" :label="c" :value="c" />
        </el-select>
      </div>

      <!-- 文档列表 -->
      <div v-if="docs.length" class="doc-list">
        <div v-for="doc in docs" :key="doc.id" class="doc-card" @click="viewDoc(doc)">
          <div class="doc-icon">{{ iconFor(doc.docType) }}</div>
          <div class="doc-info">
            <h4 class="doc-title">{{ doc.title }}</h4>
            <p class="doc-summary">{{ doc.summary || (doc.content || '').substring(0, 200) || '无内容' }}</p>
            <div class="doc-meta">
              <span>{{ doc.collection }}</span>
              <span>{{ (doc.docType || '').toUpperCase() }}</span>
              <span>{{ formatSize(doc.fileSize) }}</span>
              <span>{{ formatDate(doc.createdAt) }}</span>
            </div>
          </div>
          <div class="doc-actions" @click.stop>
            <el-button size="small" text @click="downloadDoc(doc)">⬇</el-button>
            <el-button size="small" text type="danger" @click="confirmDelete(doc)">🗑</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无文档，点击上传文档开始构建知识库" :image-size="80" />

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination-area">
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize"
          :total="total" layout="total, prev, pager, next" small @current-change="fetchList" />
      </div>
    </div>

    <!-- 上传弹窗 -->
    <el-dialog v-model="uploadVisible" title="上传文档" width="480px" top="15vh">
      <el-upload ref="uploadRef" drag :auto-upload="false" :limit="1"
        :on-change="handleFileChange" action="#">
        <el-icon :size="48"><UploadFilled /></el-icon>
        <p>拖拽文件到此处，或点击选择</p>
        <p class="upload-hint">支持 PDF / TXT / Markdown / 代码文件，最大 10MB</p>
      </el-upload>
      <div class="upload-collection">
        <span class="label">集合：</span>
        <el-input v-model="uploadCollection" placeholder="default" size="small" style="width: 200px" />
      </div>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUpload" :disabled="!pendingFile">上传</el-button>
      </template>
    </el-dialog>

    <!-- 文档详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detailDoc?.title || '文档详情'" width="720px" top="5vh">
      <div v-if="detailDoc" class="doc-detail">
        <div class="detail-meta">
          <span>类型: {{ detailDoc.docType }}</span>
          <span>集合: {{ detailDoc.collection }}</span>
          <span>大小: {{ formatSize(detailDoc.fileSize) }}</span>
        </div>
        <pre class="detail-content">{{ (detailDoc.content || '').substring(0, 10000) }}</pre>
      </div>
    </el-dialog>

    <!-- 语义搜索弹窗 -->
    <el-dialog v-model="searchVisible" title="🔍 语义搜索" width="640px" top="10vh">
      <el-input v-model="aiQuery" placeholder="输入你的问题..." size="default"
        @keyup.enter="doSemanticSearch" />
      <div v-if="searchResults.length" class="search-results">
        <div v-for="r in searchResults" :key="r.id" class="search-item" @click="viewDoc(r); searchVisible = false">
          <h5>{{ r.title }}</h5>
          <p>{{ r.summary || (r.content || '').substring(0, 150) }}</p>
        </div>
      </div>
      <el-empty v-else-if="searched" description="未找到相关文档" :image-size="60" />
      <template #footer>
        <el-button @click="searchVisible = false">关闭</el-button>
        <el-button type="primary" @click="doSemanticSearch">搜索</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, UploadFilled } from '@element-plus/icons-vue'
import { listDocuments, uploadDocument, deleteDocument, semanticSearch, listCollections } from '@/api/knowledge'

const docs = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const filterCollection = ref('')
const collections = ref([])
const stats = ref({ totalDocuments: 0, totalCollections: 0 })

const uploadVisible = ref(false)
const pendingFile = ref(null)
const uploadCollection = ref('default')
const uploadRef = ref(null)

const detailVisible = ref(false)
const detailDoc = ref(null)

const searchVisible = ref(false)
const aiQuery = ref('')
const searchResults = ref([])
const searched = ref(false)

function iconFor(type) {
  const map = { pdf: '📕', md: '📝', txt: '📄', html: '🌐', js: '🟨', css: '🟦', json: '📋', java: '☕', py: '🐍', vue: '🟩' }
  return map[type] || '📄'
}
function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN')
}

async function fetchList() {
  try {
    const params = { page: currentPage.value - 1, size: pageSize.value }
    if (searchQuery.value.trim()) params.keyword = searchQuery.value.trim()
    if (filterCollection.value) params.collection = filterCollection.value
    const res = await listDocuments(params)
    docs.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { /* silent */ }
}

async function fetchStats() {
  try {
    const res = await listDocuments({ page: 0, size: 1 })
    stats.value.totalDocuments = res.data?.total || 0
  } catch { /* silent */ }
  try {
    const cols = await listCollections()
    collections.value = cols?.data || []
    stats.value.totalCollections = collections.value.length
  } catch { /* silent */ }
}

async function doSemanticSearch() {
  if (!aiQuery.value.trim()) return
  searched.value = true
  try {
    const res = await semanticSearch(aiQuery.value, { topK: 10 })
    searchResults.value = res.data || []
  } catch { ElMessage.error('搜索失败') }
}

function handleFileChange(file) { pendingFile.value = file.raw }
async function submitUpload() {
  if (!pendingFile.value) return
  const formData = new FormData()
  formData.append('file', pendingFile.value)
  formData.append('collection', uploadCollection.value || 'default')
  try {
    await uploadDocument(formData)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    pendingFile.value = null
    uploadCollection.value = 'default'
    uploadRef.value?.clearFiles()
    fetchList(); fetchStats()
  } catch { ElMessage.error('上传失败') }
}

async function confirmDelete(doc) {
  try {
    await ElMessageBox.confirm(`确定删除「${doc.title}」？`, '确认删除', { type: 'warning' })
  } catch { return }
  try {
    await deleteDocument(doc.id)
    ElMessage.success('已删除')
    fetchList(); fetchStats()
    if (detailDoc.value?.id === doc.id) detailVisible.value = false
  } catch { ElMessage.error('删除失败') }
}

function viewDoc(doc) { detailDoc.value = doc; detailVisible.value = true }

function downloadDoc(doc) {
  const token = localStorage.getItem('token')
  fetch(`/api/knowledge/documents/${doc.id}/download`, { headers: { Authorization: `Bearer ${token}` } })
    .then(r => r.blob()).then(blob => {
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = doc.fileName || doc.title + '.txt'
      a.click()
      URL.revokeObjectURL(a.href)
    })
}

onMounted(() => { fetchList(); fetchStats() })
</script>

<style scoped>
.knowledge-page { padding: 24px 0; display: flex; justify-content: center; }
.page-container { width: 100%; max-width: 900px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 22px; color: var(--text-heading); }
.subtitle { color: var(--text-dim); font-size: 13px; margin: 4px 0 0; }
.header-actions { display: flex; gap: 8px; }

.stats-row { display: flex; gap: 16px; margin-bottom: 20px; }
.stat-card { flex: 1; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius); padding: 16px 20px; display: flex; flex-direction: column; gap: 4px; }
.stat-num { font-size: 28px; font-weight: 700; color: var(--accent); }
.stat-label { font-size: 12px; color: var(--text-dim); }

.toolbar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; }

.doc-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.doc-card { display: flex; align-items: center; gap: 14px; padding: 14px 16px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius); cursor: pointer; transition: all .15s; }
.doc-card:hover { border-color: var(--accent); background: rgba(124,138,255,.04); }
.doc-icon { font-size: 28px; flex-shrink: 0; }
.doc-info { flex: 1; min-width: 0; }
.doc-title { margin: 0 0 4px; font-size: 14px; color: var(--text-heading); }
.doc-summary { margin: 0 0 6px; font-size: 12px; color: var(--text-dim); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.doc-meta { display: flex; gap: 12px; font-size: 11px; color: var(--text-dim); }
.doc-actions { flex-shrink: 0; display: flex; gap: 4px; }

.upload-hint { font-size: 12px; color: var(--text-dim); margin-top: 8px; }
.upload-collection { display: flex; align-items: center; gap: 8px; margin-top: 12px; }
.upload-collection .label { font-size: 13px; color: var(--text-dim); white-space: nowrap; }

.doc-detail .detail-meta { display: flex; gap: 20px; font-size: 12px; color: var(--text-dim); margin-bottom: 12px; }
.detail-content { background: var(--bg-code); border: 1px solid var(--border-color); border-radius: var(--radius); padding: 16px; font-size: 13px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; max-height: 500px; overflow-y: auto; color: var(--text-primary); }

.search-results { margin-top: 16px; display: flex; flex-direction: column; gap: 8px; }
.search-item { padding: 12px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius); cursor: pointer; }
.search-item:hover { border-color: var(--accent); }
.search-item h5 { margin: 0 0 4px; font-size: 14px; }
.search-item p { margin: 0; font-size: 12px; color: var(--text-dim); }

.pagination-area { display: flex; justify-content: center; }
</style>
