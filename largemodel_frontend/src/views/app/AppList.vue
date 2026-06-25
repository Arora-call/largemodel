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
        <el-select v-model="typeFilter" placeholder="全部类型" clearable size="default" style="width:130px" @change="fetchList">
          <el-option label="全部" value="" />
          <el-option label="单文件" value="SINGLE_FILE" />
          <el-option label="多文件" value="MULTI_FILE" />
          <el-option label="Vue3项目" value="VUE_PROJECT" />
        </el-select>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="sortedApps.length === 0 && !loading" class="empty-state">
      <div class="empty-icon">📁</div>
      <h3>还没有生成任何应用</h3>
      <p>去 AI 代码生成页面开始创建你的第一个应用</p>
      <el-button type="primary" @click="$router.push('/ai/generate')">
        ✨ 开始生成
      </el-button>
    </div>

    <!-- App grid -->
    <div v-else class="app-grid">
      <div v-for="app in sortedApps" :key="app.id" class="app-card" @click="openApp(app)">
        <div class="card-top">
          <div class="card-type-badge" :class="app.type === 'ENGINEERING' ? 'type-project' : 'type-native'">
            {{ typeLabel(app.type) }}
          </div>
          <span v-if="app.priority >= 999" class="badge-pinned">📌 置顶</span>
          <span v-else-if="app.priority >= 99" class="badge-featured">⭐ 精选</span>
          <span class="card-lang">{{ app.language || 'code' }}</span>
        </div>
        <div class="card-body">
          <h4 class="card-name">{{ app.name }}</h4>
          <p class="card-desc">{{ app.description || '暂无描述' }}</p>
        </div>
        <div class="card-footer">
          <span class="card-date">{{ app.updatedAt?.substring(0, 10) || '-' }}</span>
          <div class="card-actions">
            <button class="card-btn pin-btn"
              :class="{ active: app.priority >= 99 }"
              @click.stop="handlePriority(app)"
              :title="app.priority >= 999 ? '取消置顶' : app.priority >= 99 ? '取消精选' : '置顶/精选'">
              {{ app.priority >= 999 ? '📌' : app.priority >= 99 ? '⭐' : '📌' }}
            </button>
            <button class="card-btn deploy-btn"
              :class="{ deploying: deployingAppId === app.id }"
              @click.stop="handleDeployApp(app)"
              :disabled="deployingAppId === app.id"
              title="部署预览">
              {{ deployingAppId === app.id ? '⏳' : '🚀' }}
            </button>
            <button class="card-btn edit-btn" @click.stop="openRename(app)" title="重命名">✏️</button>
            <button class="card-btn download-btn" @click.stop="handleDownload(app)" title="下载">⬇</button>
            <button class="card-btn delete-btn" @click.stop="confirmDelete(app)" title="删除">🗑</button>
          </div>
        </div>
        <!-- 部署成功 → 显示 URL -->
        <div v-if="deployUrls[app.id]" class="card-deploy-url" @click.stop>
          <a :href="deployUrls[app.id]" target="_blank" class="deploy-link">{{ deployUrls[app.id] }}</a>
          <button class="copy-btn" @click.stop="copyDeployUrl(app.id)" title="复制">📋</button>
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

    <!-- Detail dialog — 部署预览 + 重命名 + 下载 -->
    <el-dialog v-model="detailVisible" :title="detailApp?.name || '应用详情'" width="660px" destroy-on-close top="3vh">
      <div v-if="detailApp" class="detail-container">
        <div class="detail-meta">
          <div class="meta-item">
            <span class="meta-label">类型</span>
            <span class="meta-value">{{ typeLabel(detailApp.type) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">语言</span>
            <span class="meta-value">{{ detailApp.language || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">更新时间</span>
            <span class="meta-value">{{ detailApp.updatedAt?.substring(0, 16) || '-' }}</span>
          </div>
        </div>

        <!-- 封面 -->
        <div class="detail-cover" v-if="detailApp.coverImage">
          <img :src="detailApp.coverImage" alt="封面" @error="e => e.target.style.display='none'" />
        </div>

        <div class="detail-actions">
          <el-button type="primary" @click="openInWorkspace(detailApp)">
            ✏️ 打开编辑
          </el-button>
          <el-button type="warning" :loading="deploying" @click="handleDeployApp(detailApp)">
            🚀 部署预览
          </el-button>
          <el-button @click="handleDownload(detailApp)">
            <el-icon><Download /></el-icon> 下载 ZIP
          </el-button>
          <el-button @click="openRename(detailApp); detailVisible = false">
            ✏️ 重命名
          </el-button>
          <el-button type="danger" plain @click="detailVisible = false; confirmDelete(detailApp)">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>

        <!-- Nginx 部署预览 -->
        <div v-if="deployedUrl" class="deploy-preview">
          <div class="deploy-preview-header">
            <span>🚀 部署预览</span>
            <div>
              <el-button size="small" text @click="copyUrl">📋 复制 URL</el-button>
              <el-button size="small" text @click="openDeployUrl">🔗 新窗口打开</el-button>
            </div>
          </div>
          <div class="deploy-url">{{ deployedUrl }}</div>
          <iframe :src="deployedUrl" class="deploy-iframe"></iframe>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Download, Delete } from '@element-plus/icons-vue'
import { listApplications, deleteApplication, downloadApplication, updateApplication, setAppPriority } from '@/api/app'
import { deployByAppId } from '@/api/ai'

const router = useRouter()

const apps = ref([])
const sortedApps = computed(() =>
  [...apps.value].sort((a, b) => (b.priority || 0) - (a.priority || 0))
)
const loading = ref(false)
const total = ref(0)
const size = ref(12)
const currentPage = ref(1)
const keyword = ref('')
const typeFilter = ref('')
const detailVisible = ref(false)
const detailApp = ref(null)

const renameVisible = ref(false)
const renameApp = ref(null)
const renameName = ref('')
const deployedUrl = ref('')
const deploying = ref(false)
const deployingAppId = ref(null)        // 正在部署的卡片 ID
const deployUrls = reactive({})         // appId → URL 缓存

function typeLabel(type) {
  const map = { SINGLE_FILE: '⚡ 单文件', MULTI_FILE: '📦 多文件', VUE_PROJECT: '🟩 Vue3项目', ENGINEERING: '📦 工程' }
  return map[type] || type || '-'
}

/** 点击卡片 → 跳转 AI 工作台并加载关联对话 */
function openApp(app) {
  router.push({ path: '/workspace', query: { appId: app.id } })
}

/** 打开应用对应的对话编辑 */
function openInWorkspace(app) {
  detailVisible.value = false
  router.push({ path: '/workspace', query: { appId: app.id } })
}

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

async function handleDeployApp(app) {
  deployingAppId.value = app.id
  deploying.value = true
  deployedUrl.value = ''
  try {
    const res = await deployByAppId(app.id)
    if (res?.code === 200 && res?.data) {
      deployedUrl.value = res.data.url
      deployUrls[app.id] = res.data.url
      ElMessage.success('部署成功！')
    } else {
      ElMessage.error(res?.message || '部署失败')
    }
  } catch (e) {
    ElMessage.error('部署失败: ' + e.message)
  } finally {
    deployingAppId.value = null
    deploying.value = false
  }
}

/** 置顶/精选 轮转: 默认→精选(99)→置顶(999)→默认(0) */
async function handlePriority(app) {
  const next = (app.priority || 0) >= 999 ? 0 : (app.priority || 0) >= 99 ? 999 : 99
  try {
    await setAppPriority(app.id, next)
    app.priority = next
    const labels = { 999: '已置顶', 99: '已设为精选', 0: '已取消' }
    ElMessage.success(labels[next] || '已更新')
  } catch { ElMessage.error('操作失败') }
}

function copyDeployUrl(appId) {
  const url = deployUrls[appId]
  if (url) {
    navigator.clipboard.writeText(url).then(
      () => ElMessage.success('URL 已复制'),
      () => ElMessage.info(url)
    )
  }
}

function copyUrl() {
  if (deployedUrl.value) {
    navigator.clipboard.writeText(deployedUrl.value).then(
      () => ElMessage.success('已复制'),
      () => ElMessage.info('请手动复制')
    )
  }
}

function openDeployUrl() {
  if (deployedUrl.value) window.open(deployedUrl.value, '_blank')
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
      keyword: keyword.value, type: typeFilter.value
    })
    apps.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch { apps.value = []; total.value = 0 }
  finally { loading.value = false }
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

.badge-pinned {
  font-size: 10px;
  color: var(--danger, #ef4444);
  background: rgba(239, 68, 68, 0.1);
  padding: 2px 6px;
  border-radius: 6px;
  font-weight: 600;
}

.badge-featured {
  font-size: 10px;
  color: var(--warning, #f59e0b);
  background: rgba(245, 158, 11, 0.1);
  padding: 2px 6px;
  border-radius: 6px;
  font-weight: 600;
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

.pin-btn.active {
  background: rgba(251, 191, 36, 0.15);
  border-color: var(--warning, #f59e0b);
}
.pin-btn:hover { background: rgba(251, 191, 36, 0.1); border-color: var(--warning, #f59e0b); }
.deploy-btn:hover {
  background: rgba(251, 146, 60, 0.15);
  border-color: #fb923c;
}
.deploy-btn.deploying {
  animation: spin 1s linear infinite;
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.delete-btn:hover {
  background: var(--danger-bg);
  border-color: var(--danger);
}
.card-deploy-url {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(124, 138, 255, 0.08);
  border-top: 1px solid rgba(124, 138, 255, 0.2);
  margin: 0;
}
.deploy-link {
  flex: 1;
  font-family: 'Fira Code', monospace;
  font-size: 11px;
  color: var(--accent, #7c8aff);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-decoration: none;
}
.deploy-link:hover { text-decoration: underline; }
.copy-btn {
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--text-dim);
  cursor: pointer;
  font-size: 12px;
  padding: 2px 6px;
  flex-shrink: 0;
}
.copy-btn:hover { background: var(--bg-hover); color: var(--text-primary); }

.pagination-area {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

/* ====== Detail Dialog ====== */
.detail-container { display: flex; flex-direction: column; gap: 16px; }
.detail-meta {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}
.meta-item { display: flex; flex-direction: column; gap: 4px; }
.meta-label { font-size: 12px; color: var(--text-dim); }
.meta-value { font-size: 14px; color: var(--text-primary); font-weight: 500; }
.detail-cover { border-radius: var(--radius); overflow: hidden; max-height: 200px; }
.detail-cover img { width: 100%; height: 200px; object-fit: cover; }
.detail-actions { display: flex; gap: 10px; justify-content: flex-end; padding-top: 16px; border-top: 1px solid var(--border-color); flex-wrap: wrap; }
.deploy-preview { border: 1px solid var(--accent,#7c8aff); border-radius: var(--radius); overflow: hidden; margin-top: 8px; }
.deploy-preview-header { display:flex; align-items:center; justify-content:space-between; padding:8px 12px; background:rgba(124,138,255,.1); font-size:13px; font-weight:600; }
.deploy-url { padding:6px 12px; font-family:'Fira Code',monospace; font-size:11px; color:var(--accent,#7c8aff); background:var(--bg-code); }
.deploy-iframe { width:100%; height:420px; border:none; }
</style>
