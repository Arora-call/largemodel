<template>
  <div class="project-create">
    <!-- 顶栏 -->
    <header class="proj-header">
      <div class="header-left">
        <span class="logo-dot"></span>
        <span class="logo-text">项目创建</span>
      </div>
      <div class="header-right">
        <button class="btn-hdr" :disabled="!files.length" @click="handleSave">
          <el-icon><FolderChecked /></el-icon> 保存项目
        </button>
        <button class="btn-hdr" :disabled="!files.length" @click="handleDownload">
          <el-icon><Download /></el-icon> 下载 ZIP
        </button>
      </div>
    </header>

    <!-- 主区域：三栏 -->
    <div class="proj-main">
      <!-- 左：文件树 -->
      <div class="panel-tree">
        <FileTree :files="treeFiles" :active-file="activeFile" @select="selectFile" />
      </div>

      <!-- 中：代码 / 输入 -->
      <div class="panel-center">
        <!-- 未生成时：输入区 -->
        <div v-if="!files.length && !generating" class="welcome-panel">
          <div class="welcome-icon">🏗️</div>
          <h2>创建工程项目</h2>
          <p>AI 会生成完整的项目文件结构，你可以在左侧文件树中查看每个文件</p>
          <div class="quick-prompts">
            <button v-for="p in examplePrompts" :key="p" class="quick-prompt" @click="inputText = p">{{ p }}</button>
          </div>
          <div class="input-area">
            <textarea ref="inputEl" v-model="inputText" class="proj-input"
              placeholder="例如：创建一个 Vue3 数据表格组件，支持分页、排序和搜索..."
              rows="3" :disabled="generating"
              @keydown.enter.exact="handleGenerate"></textarea>
            <button class="btn-generate" :disabled="!inputText.trim() || generating" @click="handleGenerate">
              {{ generating ? '生成中...' : '开始生成 →' }}
            </button>
          </div>
        </div>

        <!-- 生成中 -->
        <div v-if="generating" class="generating-panel">
          <div class="gen-status">
            <span class="gen-spinner"></span>
            <span>AI 正在创建项目文件...</span>
          </div>
          <div v-if="streamText" class="gen-log">{{ streamText.substring(Math.max(0, streamText.length - 500)) }}</div>
        </div>

        <!-- 文件代码查看 -->
        <div v-if="files.length && !generating" class="code-panel">
          <div class="code-panel-header">
            <span v-if="currentPath" class="file-path">{{ currentPath }}</span>
            <span v-else class="file-path">选择一个文件查看代码</span>
          </div>
          <CodeViewer v-if="currentContent !== null"
            :code="currentContent"
            :language="currentLanguage || 'text'"
            height="100%" />
          <div v-else class="no-file-selected">👈 从左侧文件树选择文件查看代码</div>
        </div>
      </div>

      <!-- 右：预览面板（仅前端项目） -->
      <div v-if="isFrontend && files.length" class="panel-preview">
        <div class="preview-toolbar">
          <button :class="{ active: !editMode }" @click="editMode = false">预览</button>
          <button :class="{ active: editMode }" @click="editMode = true">编辑</button>
          <span v-if="editMode" class="edit-hint">点击页面元素</span>
        </div>
        <iframe v-if="previewHtml" :srcdoc="previewHtml"
          sandbox="allow-scripts allow-same-origin" class="preview-frame"></iframe>
        <div v-else class="preview-empty">需要 index.html 或 App.vue 文件才能预览</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { FolderChecked, Download } from '@element-plus/icons-vue'
import FileTree from '@/components/project/FileTree.vue'
import CodeViewer from '@/components/CodeViewer.vue'
import { generateProject, getProjectTree, getProjectFile, downloadProjectZip, saveProject } from '@/api/project'

const inputText = ref('')
const streamText = ref('')
const generating = ref(false)
const files = ref([])            // 文件树列表 [{path, language, size}]
const activeFile = ref('')       // 当前选中文件路径
const fileContent = ref('')      // 当前文件内容
const projectId = ref(null)      // 后端项目 ID
const projectType = ref('frontend')
const editMode = ref(false)
let abortController = null

const isFrontend = computed(() => projectType.value === 'frontend')
const currentPath = computed(() => activeFile.value)
const currentContent = computed(() => fileContent.value)
const currentLanguage = computed(() => {
  const f = files.value.find(f => f.path === activeFile.value)
  return f?.language || 'text'
})

// 转换为 FileTree 需要的格式
const treeFiles = computed(() => files.value.map(f => ({ path: f.path, language: f.language })))

const examplePrompts = [
  '创建一个 Vue3 数据表格组件，支持分页、排序和搜索',
  '创建一个 Spring Boot REST API，包含用户增删改查',
  '创建一个 Python Flask 博客系统，支持文章发布和评论',
  '创建一个 HTML 商城首页，包含导航栏、轮播图和商品卡片'
]

// 预览 HTML（从实际文件读取拼接）
const previewHtml = ref('')

async function buildPreview() {
  if (!isFrontend.value || !projectId.value) { previewHtml.value = ''; return }
  try {
    // 尝试读取 index.html 或 App.vue
    const htmlFile = files.value.find(f => f.path === 'index.html' || f.path.endsWith('.html'))
    if (htmlFile) {
      const res = await getProjectFile(projectId.value, htmlFile.path)
      previewHtml.value = res.data.content
      return
    }
    const appVue = files.value.find(f => f.path.endsWith('App.vue'))
    if (appVue) {
      const res = await getProjectFile(projectId.value, appVue.path)
      previewHtml.value = buildVuePreview(res.data.content)
      return
    }
    previewHtml.value = ''
  } catch { previewHtml.value = '' }
}

function buildVuePreview(appCode) {
  const tplM = appCode.match(/<template>([\s\S]*?)<\/template>/i)
  const template = tplM ? tplM[1].trim() : appCode
  const esc = template.replace(/`/g, '\\`').replace(/\$/g, '\\$')
  return `<!DOCTYPE html><html><head><meta charset="UTF-8">
<script src="https://unpkg.com/vue@3/dist/vue.global.prod.js"><\/script>
<script src="https://unpkg.com/element-plus/dist/index.full.min.js"><\/script>
<link rel="stylesheet" href="https://unpkg.com/element-plus/dist/index.css">
<style>*{box-sizing:border-box}body{font-family:system-ui,sans-serif;padding:16px;background:#fff}</style></head>
<body><div id="app">${template}</div>
<script>var {createApp}=Vue;var ElementPlus=window.ElementPlus;
try{createApp({template:\`${esc}\`}).use(ElementPlus).mount('#app')}catch(e){document.body.innerHTML='<pre>'+e.message+'</pre>'}
<\/script></body></html>`
}

// 选择文件 → 从后端读取内容
async function selectFile(path) {
  activeFile.value = path
  if (!projectId.value) return
  try {
    const res = await getProjectFile(projectId.value, path)
    fileContent.value = res.data.content
  } catch { fileContent.value = '// 读取失败' }
}

// 生成项目
async function handleGenerate() {
  if (!inputText.value.trim() || generating.value) return
  const prompt = inputText.value.trim()
  inputText.value = ''
  generating.value = true; streamText.value = ''
  files.value = []; activeFile.value = ''; fileContent.value = ''
  abortController = new AbortController()

  try {
    await generateProject(
      { prompt, type: 'ENGINEERING', language: 'text' },
      {
        signal: abortController.signal,
        onToken: (t) => { streamText.value += t },
        onDone: async (data) => {
          let j
          try { j = JSON.parse(data) } catch { j = {} }
          projectId.value = j.conversationId || Date.now()
          projectType.value = j.projectType || 'frontend'
          streamText.value = ''; generating.value = false
          // 从后端获取真实文件树
          try {
            const treeRes = await getProjectTree(projectId.value)
            files.value = treeRes.data || []
            if (files.value.length > 0) {
              selectFile(files.value[0].path)
              await buildPreview()
            }
          } catch { ElMessage.error('获取文件树失败') }
        },
        onError: (err) => {
          ElMessage.error(err.message || '生成失败')
          generating.value = false; streamText.value = ''
        }
      }
    )
  } catch (err) {
    ElMessage.error(err.message || '网络错误')
    generating.value = false; streamText.value = ''
  }
}

// 保存
async function handleSave() {
  if (!projectId.value) return
  try {
    await saveProject(projectId.value, prompt?.value?.substring(0, 50) || '未命名项目', projectType.value)
    ElMessage.success('已保存到「我的应用」')
  } catch { ElMessage.error('保存失败') }
}

// 下载
function handleDownload() {
  if (!projectId.value) return
  downloadProjectZip(projectId.value)
}

onBeforeUnmount(() => { abortController?.abort() })
</script>

<style scoped>
.project-create { display: flex; flex-direction: column; height: calc(100vh - 60px); background: #0d1117; color: #c9d1d9; --accent: #7c8aff; --bg-sidebar: #0a0d13; --bg-card: #141821; --border: rgba(255,255,255,0.07); --text: #c9d1d9; --text-dim: #6b7280; }
.proj-header { display: flex; align-items: center; justify-content: space-between; padding: 0 20px; height: 48px; background: var(--bg-sidebar); border-bottom: 1px solid var(--border); flex-shrink: 0; }
.header-left { display: flex; align-items: center; gap: 10px; }
.logo-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--accent); }
.logo-text { font-size: 15px; font-weight: 600; color: #e5e7eb; }
.header-right { display: flex; gap: 8px; }
.btn-hdr { display: flex; align-items: center; gap: 4px; padding: 6px 14px; background: var(--bg-card); color: var(--text); border: 1px solid var(--border); border-radius: 6px; font-size: 13px; cursor: pointer; transition: all .15s; }
.btn-hdr:hover:not(:disabled) { background: rgba(124,138,255,0.1); border-color: var(--accent); }
.btn-hdr:disabled { opacity: 0.4; cursor: default; }
.proj-main { display: flex; flex: 1; min-height: 0; }
.panel-tree { width: 240px; flex-shrink: 0; }
.panel-center { flex: 1; display: flex; flex-direction: column; min-width: 0; border-right: 1px solid var(--border); }
.panel-preview { width: 420px; flex-shrink: 0; display: flex; flex-direction: column; background: #fff; }
.welcome-panel { display: flex; flex-direction: column; align-items: center; justify-content: center; flex: 1; padding: 40px; text-align: center; }
.welcome-icon { font-size: 48px; margin-bottom: 16px; }
.welcome-panel h2 { font-size: 22px; color: #e5e7eb; margin: 0 0 8px; }
.welcome-panel p { font-size: 14px; color: var(--text-dim); margin: 0 0 20px; }
.quick-prompts { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; max-width: 600px; margin-bottom: 24px; }
.quick-prompt { padding: 8px 16px; background: var(--bg-card); color: var(--text); border: 1px solid var(--border); border-radius: 20px; font-size: 13px; cursor: pointer; transition: all .2s; }
.quick-prompt:hover { background: rgba(124,138,255,0.1); border-color: rgba(124,138,255,0.25); color: var(--accent); }
.input-area { width: 100%; max-width: 600px; display: flex; gap: 10px; }
.proj-input { flex: 1; padding: 12px 16px; background: var(--bg-card); color: var(--text); border: 1px solid var(--border); border-radius: 10px; font-size: 14px; resize: none; outline: none; font-family: inherit; }
.proj-input:focus { border-color: rgba(124,138,255,0.4); }
.btn-generate { padding: 10px 24px; background: linear-gradient(135deg, var(--accent), #7c3aed); color: #fff; border: none; border-radius: 10px; font-size: 14px; cursor: pointer; white-space: nowrap; transition: all .2s; }
.btn-generate:hover:not(:disabled) { transform: scale(1.02); }
.btn-generate:disabled { opacity: 0.4; cursor: default; }
.generating-panel { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px; }
.gen-status { display: flex; align-items: center; gap: 12px; font-size: 16px; color: #e5e7eb; }
.gen-spinner { width: 20px; height: 20px; border: 2px solid var(--border); border-top-color: var(--accent); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.gen-log { margin-top: 16px; padding: 12px; background: var(--bg-card); border-radius: 8px; font-size: 12px; color: var(--text-dim); max-width: 500px; max-height: 120px; overflow: hidden; font-family: monospace; white-space: pre-wrap; }
.code-panel { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.code-panel-header { padding: 10px 16px; background: var(--bg-card); border-bottom: 1px solid var(--border); flex-shrink: 0; }
.file-path { font-size: 13px; color: var(--text); font-family: monospace; }
.no-file-selected { flex: 1; display: flex; align-items: center; justify-content: center; color: var(--text-dim); font-size: 14px; }
.preview-toolbar { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #f5f5f5; border-bottom: 1px solid #e5e7eb; flex-shrink: 0; }
.preview-toolbar button { padding: 4px 14px; border: none; background: transparent; color: #9ca3af; font-size: 12px; cursor: pointer; border-bottom: 2px solid transparent; transition: all .15s; }
.preview-toolbar button:hover { color: #374151; }
.preview-toolbar button.active { color: #5b6af0; border-bottom-color: #5b6af0; font-weight: 500; }
.edit-hint { font-size: 11px; color: #92400e; margin-left: auto; }
.preview-frame { flex: 1; width: 100%; border: none; background: #fff; }
.preview-empty { flex: 1; display: flex; align-items: center; justify-content: center; color: #999; font-size: 14px; padding: 20px; }
</style>
