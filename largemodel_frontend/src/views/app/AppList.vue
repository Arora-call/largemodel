<template>
  <div class="app-list-page">
    <div class="page-header">
      <h2 class="page-title">我的应用</h2>
      <div class="header-actions">
        <el-input
          v-model="keyword"
          placeholder="搜索应用..."
          clearable
          size="default"
          style="width:220px"
          @keyup.enter="fetchList"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="langFilter" placeholder="全部语言" clearable size="default" style="width:130px" @change="fetchList">
          <el-option label="全部" value="" />
          <el-option label="Vue" value="vue" />
          <el-option label="Java" value="java" />
          <el-option label="HTML" value="html" />
          <el-option label="Python" value="python" />
        </el-select>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="apps.length === 0 && !loading" class="empty-state">
      <div class="empty-icon">📁</div>
      <h3>还没有生成任何应用</h3>
      <p>去 AI 代码生成页面开始创建你的第一个应用</p>
      <el-button type="primary" @click="$router.push('/ai/generate')">
        ✨ 开始生成
      </el-button>
    </div>

    <!-- App grid -->
    <div v-else class="app-grid">
      <div v-for="app in apps" :key="app.id" class="app-card" @click="viewDetail(app)">
        <div class="card-top">
          <div class="card-type-badge" :class="app.type === 'ENGINEERING' ? 'type-project' : 'type-native'">
            {{ app.type === 'ENGINEERING' ? '📦 工程' : '⚡ 原生' }}
          </div>
          <span class="card-lang">{{ app.language || 'code' }}</span>
        </div>
        <div class="card-body">
          <h4 class="card-name">{{ app.name }}</h4>
          <p class="card-desc">{{ app.description || '暂无描述' }}</p>
        </div>
        <div class="card-footer">
          <span class="card-date">{{ app.updatedAt?.substring(0, 10) || '-' }}</span>
          <div class="card-actions">
            <button class="card-btn edit-btn" @click.stop="openRename(app)" title="重命名">✏️</button>
            <button class="card-btn download-btn" @click.stop="handleDownload(app)" title="下载">⬇</button>
            <button class="card-btn delete-btn" @click.stop="confirmDelete(app)" title="删除">🗑</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="total > size" class="pagination-area">
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

    <!-- Detail dialog — 项目结构 + 代码查看 + 重新编辑 -->
    <el-dialog v-model="detailVisible" :title="detailApp?.name || '应用详情'" width="860px" destroy-on-close top="3vh">
      <div v-if="detailApp" class="detail-container">
        <div class="detail-meta">
          <div class="meta-item">
            <span class="meta-label">类型</span>
            <span class="meta-value">{{ detailApp.type === 'ENGINEERING' ? '📦 工程项目' : '⚡ 原生应用' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">语言</span>
            <span class="meta-value">{{ detailApp.language || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">文件数</span>
            <span class="meta-value">{{ projectFiles.length }} 个</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">更新时间</span>
            <span class="meta-value">{{ detailApp.updatedAt?.substring(0, 16) || '-' }}</span>
          </div>
        </div>

        <!-- 文件树 + 代码查看双栏 -->
        <div class="detail-filetree-panel" v-if="isEngineering">
          <div class="filetree-sidebar">
            <div class="filetree-header">📂 项目文件</div>
            <div class="filetree-list">
              <div
                v-for="f in projectFiles"
                :key="f"
                class="filetree-node"
                :class="{ active: selectedDetailFile === f }"
                @click="selectDetailFile(f)"
              >
                <span class="filetree-icon">{{ f.endsWith('/') ? '📁' : iconForFile(f) }}</span>
                <span class="filetree-name">{{ f }}</span>
              </div>
            </div>
          </div>
          <div class="filetree-code">
            <CodeViewer
              v-if="selectedDetailCode !== null"
              :code="selectedDetailCode"
              :language="detailApp.language || 'text'"
              height="360px"
              max-height="360px"
            />
            <div v-else class="no-file-selected">👈 从左侧文件列表选择文件查看代码</div>
          </div>
        </div>

        <!-- 原生应用：直接展示代码 -->
        <div v-else class="detail-block">
          <div class="code-area">
            <CodeViewer
              v-if="detailApp?.sourceCode"
              :code="detailApp.sourceCode.substring(0, 5000)"
              :language="detailApp.language || 'text'"
              height="360px"
              max-height="360px"
            />
            <p v-else class="no-code">无代码</p>
          </div>
        </div>

        <div class="detail-actions">
          <el-button v-if="isEngineering" type="primary" @click="openInEditor(detailApp)">
            ✏️ 在项目创建中编辑
          </el-button>
          <el-button @click="handleDownload(detailApp)">
            <el-icon><Download /></el-icon> 下载 ZIP
          </el-button>
          <el-button type="danger" plain @click="detailVisible = false; confirmDelete(detailApp)">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- Rename dialog -->
    <el-dialog v-model="renameVisible" title="重命名应用" width="420px" top="25vh">
      <el-form @submit.prevent="handleRename">
        <el-form-item label="应用名称">
          <el-input v-model="renameName" placeholder="输入新名称" maxlength="200" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRename" :disabled="!renameName.trim()">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Download, Delete } from '@element-plus/icons-vue'
import { listApplications, getApplication, deleteApplication, downloadApplication, updateApplication } from '@/api/app'
import CodeViewer from '@/components/CodeViewer.vue'

const router = useRouter()

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
  try {
    const cfg = JSON.parse(detailApp.value?.configJson || '{}')
    return cfg.files || cfg.tree?.map(f => f.path) || (Array.isArray(cfg) ? cfg.map(f => f.path || f) : [])
  } catch { return [] }
})
const isEngineering = computed(() => detailApp.value?.type === 'ENGINEERING')
const selectedDetailFile = ref('')
const selectedDetailCode = ref(null)

function iconForFile(path) {
  const ext = path.split('.').pop()?.toLowerCase()
  const map = { vue: '🟩', js: '🟨', ts: '🟦', html: '🟧', css: '🟦', json: '📋', py: '🐍', java: '☕', xml: '📋', md: '📝' }
  return map[ext] || '📄'
}

const renameVisible = ref(false)
const renameApp = ref(null)
const renameName = ref('')

function openRename(app) {
  renameApp.value = app
  renameName.value = app.name
  renameVisible.value = true
}

async function handleRename() {
  const name = renameName.value.trim()
  if (!name || !renameApp.value) return
  try {
    await updateApplication(renameApp.value.id, { name })
    ElMessage.success('已重命名')
    renameVisible.value = false
    fetchList()
  } catch { ElMessage.error('重命名失败') }
}

function selectDetailFile(filePath) {
  selectedDetailFile.value = filePath
  // 从 sourceCode 中提取对应文件的代码
  const code = detailApp.value?.sourceCode || ''
  const marker = '// ===== ' + filePath + ' ====='
  const regex = new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\n([\\s\\S]*?)(?=\\n// ===== |$)', 'g')
  const match = regex.exec(code)
  selectedDetailCode.value = match ? match[1].trim() : '// 代码未找到'
}

function openInEditor(app) {
  detailVisible.value = false
  router.push({ path: '/project/create', query: { projectId: app.id } })
}

async function handleDownload(app) {
  try { await downloadApplication(app.id, (app.name || 'code') + '.zip') }
  catch { ElMessage.error('下载失败') }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listApplications({
      page: currentPage.value - 1, size: size.value,
      keyword: keyword.value, language: langFilter.value
    })
    apps.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch { apps.value = []; total.value = 0 }
  finally { loading.value = false }
}

async function viewDetail(app) {
  try {
    const res = await getApplication(app.id)
    detailApp.value = res.data
    detailVisible.value = true
  } catch { ElMessage.error('加载详情失败') }
}

async function confirmDelete(app) {
  try {
    await ElMessageBox.confirm(`确定删除「${app.name}」？`, '确认删除', { type: 'warning' })
    await deleteApplication(app.id)
    ElMessage.success('已删除')
    if (detailVisible.value) detailVisible.value = false
    fetchList()
  } catch { /* cancelled */ }
}

onMounted(fetchList)
</script>

<style scoped>
.app-list-page {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

/* ====== Empty ====== */
.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 18px;
  color: var(--text-heading);
  margin: 0 0 8px;
}

.empty-state p {
  font-size: 14px;
  color: var(--text-dim);
  margin: 0 0 24px;
}

/* ====== App Grid ====== */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.app-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.app-card:hover {
  border-color: var(--border-hover);
  box-shadow: var(--shadow);
  transform: translateY(-3px);
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 0;
}

.card-type-badge {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.type-project {
  background: var(--accent-bg);
  color: var(--accent);
}

.type-native {
  background: rgba(52, 211, 153, 0.1);
  color: var(--success);
}

.card-lang {
  font-size: 10px;
  text-transform: uppercase;
  color: var(--text-dim);
  background: var(--bg-primary);
  padding: 2px 8px;
  border-radius: 8px;
  font-weight: 500;
}

.card-body {
  padding: 12px 16px;
  flex: 1;
}

.card-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-heading);
  margin: 0 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-desc {
  font-size: 12px;
  color: var(--text-dim);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-top: 1px solid var(--border-color);
}

.card-date {
  font-size: 11px;
  color: var(--text-dim);
}

.card-actions {
  display: flex;
  gap: 6px;
}

.card-btn {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition);
}

.edit-btn:hover {
  background: rgba(251, 191, 36, 0.1);
  border-color: var(--warning);
}

.download-btn:hover {
  background: var(--accent-bg);
  border-color: var(--accent);
}

.delete-btn:hover {
  background: var(--danger-bg);
  border-color: var(--danger);
}

.pagination-area {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

/* ====== Detail Dialog ====== */
.detail-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 12px;
  color: var(--text-dim);
}

.meta-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.detail-block {
  margin-bottom: 20px;
}

.detail-block h5 {
  font-size: 13px;
  color: var(--text-heading);
  margin: 0 0 10px;
}

.file-list {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  padding: 10px 14px;
}

.file-item {
  font-family: 'Cascadia Code', 'JetBrains Mono', monospace;
  font-size: 12px;
  color: var(--text-secondary);
  padding: 3px 0 3px 16px;
  position: relative;
}

.file-item::before {
  content: '└';
  position: absolute;
  left: 0;
  color: var(--text-dim);
}

.dep-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dep-tag {
  margin: 0;
}

.code-area {
  background: var(--bg-code);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  overflow: hidden;
}

.no-code {
  color: var(--text-dim);
  padding: 20px;
  text-align: center;
}

.detail-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}
.detail-container { display: flex; flex-direction: column; gap: 16px; }
.detail-filetree-panel { display: flex; gap: 0; border: 1px solid var(--border-color); border-radius: var(--radius); overflow: hidden; min-height: 360px; }
.filetree-sidebar { width: 260px; flex-shrink: 0; background: var(--bg-secondary); border-right: 1px solid var(--border-color); overflow-y: auto; }
.filetree-header { padding: 10px 14px; font-size: 12px; font-weight: 600; color: var(--text-heading); border-bottom: 1px solid var(--border-color); }
.filetree-list { padding: 4px 0; }
.filetree-node { padding: 5px 14px 5px 20px; font-size: 12px; font-family: 'SF Mono','Fira Code',monospace; color: var(--text-secondary); cursor: pointer; display: flex; align-items: center; gap: 6px; transition: all .15s; }
.filetree-node:hover { background: var(--bg-hover); color: var(--text-primary); }
.filetree-node.active { background: var(--accent-bg); color: var(--accent); }
.filetree-icon { font-size: 13px; flex-shrink: 0; }
.filetree-name { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.filetree-code { flex: 1; background: var(--bg-code); min-width: 0; }
.no-file-selected { height: 100%; display: flex; align-items: center; justify-content: center; color: var(--text-dim); font-size: 13px; }
</style>
