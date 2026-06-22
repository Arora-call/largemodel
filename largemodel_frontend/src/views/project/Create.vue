<template>
  <div class="project-create">
    <!-- 顶栏 -->
    <header class="proj-header">
      <div class="header-left">
        <span class="logo-dot"></span>
        <span class="logo-text">项目创建</span>
      </div>
      <div class="header-right">
        <div class="model-selector" style="position:relative;margin-right:8px">
          <span class="model-btn" @click="showModelSelect = !showModelSelect">
            🤖 {{ selectedModelName }} ▾
          </span>
          <div v-if="showModelSelect" class="model-dropdown">
            <div v-if="availableModels.length === 0" class="model-item muted">加载中...</div>
            <div v-for="m in availableModels" :key="m.id" class="model-item"
                 :class="{ active: selectedModelId === m.id }" @click="selectProjectModel(m)">
              {{ m.name }}<el-tag v-if="m.isDefault" size="small" type="success" effect="plain" style="margin-left:4px;font-size:9px">默认</el-tag>
              <span style="font-size:10px;color:var(--text-dim);display:block">{{ m.provider }}</span>
            </div>
          </div>
        </div>
        <button class="btn-hdr btn-new" @click="startNewProject">+ 新建项目</button>
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
      <!-- 左：项目列表 + 文件树 -->
      <div class="panel-left">
        <div class="project-list">
          <div class="list-label">项目列表</div>
          <div v-if="projects.length === 0" class="list-empty">暂无项目</div>
          <div
            v-for="p in projects"
            :key="p.id"
            class="project-item"
            :class="{ active: p.id === projectId }"
            @click="switchProject(p.id)"
          >
            <span class="project-item-name">{{ p.title || '未命名项目' }}</span>
            <span class="project-item-count">{{ p.fileCount || 0 }} 文件</span>
            <button class="project-item-del" title="删除项目" @click.stop="handleDeleteProject(p)">×</button>
          </div>
        </div>
        <div class="panel-tree">
          <FileTree :files="treeFiles" :active-file="activeFile" @select="selectFile" />
        </div>
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

      <!-- 拖拽调节预览宽度 -->
      <div v-if="showPreview && files.length" class="resize-handle" @mousedown="startResize">
        <div class="resize-grip"><span></span><span></span><span></span></div>
      </div>

      <!-- 右：预览面板 -->
      <div v-if="showPreview && files.length" class="panel-preview" :style="{ width: previewWidth + 'px' }">
        <div class="preview-toolbar">
          <span class="preview-label">📦 项目预览</span>
          <span v-if="isBackendPreview" class="backend-tag">后端项目 · 静态预览</span>
          <button class="btn-edit-mode" :class="{ active: editMode }" @click="toggleEditMode">
            {{ editMode ? '✕ 退出编辑' : '✏️ 编辑' }}
          </button>
          <button v-if="sandpackError" class="preview-retry" @click="retryPreview">重试</button>
        </div>

        <!-- 后端项目提示条（非编辑模式） -->
        <div v-if="isBackendPreview && !editMode" class="backend-banner">
          <span class="backend-banner-icon">🐍</span>
          <span class="backend-banner-text">
            后端项目（Flask/Django 模板）— 仅展示静态 HTML 预览，Jinja2 动态内容已替换为占位符。
            运行 <code>pip install -r requirements.txt && python app.py</code> 查看完整效果。
          </span>
        </div>

        <!-- 错误提示栏 -->
        <div v-if="previewError && !editMode" class="preview-error-banner">
          <span class="error-msg">⚠️ {{ (previewError.message || '').substring(0, 200) }}</span>
          <button class="btn-fix-error" @click="handleFixError">🤖 AI 修复</button>
        </div>

        <!-- 编辑模式：元素信息栏 -->
        <div v-if="editMode" class="edit-info-bar">
          <span v-if="selectedElement">
            已选: <b>{{ selectedElement.tag }}</b>
            <template v-if="selectedElement.id"> #{{ selectedElement.id }}</template>
            <template v-if="selectedElement.cls"> .{{ selectedElement.cls }}</template>
            <span class="el-text-preview"> — {{ (selectedElement.text || '').substring(0, 40) }}</span>
          </span>
          <span v-else-if="isBackendPreview">🖱️ 点击静态预览中的元素 → AI 会修改模板源文件（.html）</span>
          <span v-else>🖱️ 点击预览中的元素开始编辑</span>
          <button v-if="selectedElement" class="btn-clear-sel" @click="clearSelection">取消选择</button>
        </div>

        <div v-show="previewReady" class="preview-container" :class="{ 'no-pointer': isResizing }">
          <!-- 后端项目：简单 iframe 静态预览 -->
          <iframe
            v-if="isBackendPreview && backendPreviewHtml"
            :key="'backend-' + sandpackKey"
            :srcdoc="backendPreviewHtml"
            sandbox="allow-scripts"
            referrerpolicy="no-referrer"
            class="preview-iframe"
          ></iframe>
          <!-- 前端项目：Sandpack 预览 -->
          <SandboxPreview
            v-else-if="sandpackVisible"
            :key="sandpackKey"
            :files="displayFiles"
            :readOnly="false"
            :showEditor="true"
            :editMode="editMode"
            height="100%"
            @element-selected="onElementSelected"
            @preview-error="onPreviewError"
          />
        </div>
        <div v-if="!previewReady" class="preview-empty">正在加载项目文件...</div>
        <div v-if="sandpackError && !sandpackVisible" class="preview-error">
          <p>预览加载失败</p>
          <button @click="retryPreview">点击重试</button>
        </div>

        <!-- 编辑模式：AI 修改输入栏 -->
        <div v-if="editMode" class="edit-modify-bar">
          <input
            v-model="modifyPrompt"
            class="modify-input"
            :placeholder="selectedElement ? '描述修改要求，如：把按钮改成蓝色...' : '描述修改要求，或点击预览中的元素...'"
            :disabled="modifying"
            @keydown.enter="handleModify"
          />
          <button class="btn-modify" :disabled="modifying || !modifyPrompt.trim()" @click="handleModify">
            {{ modifying ? '⏳ 修改中...' : '➤ 修改' }}
          </button>
          <button v-if="modifying" class="btn-stop-modify" @click="stopModifying" title="取消">✕</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderChecked, Download } from '@element-plus/icons-vue'
import FileTree from '@/components/project/FileTree.vue'
import CodeViewer from '@/components/CodeViewer.vue'
import SandboxPreview from '@/components/SandboxPreview.vue'
import { generateProject, getProjectTree, getProjectFile, downloadProjectZip, saveProject, modifyProjectStream, saveProjectFiles } from '@/api/project'
import { listConversations, deleteProject, getApplication } from '@/api/app'
import { preparePreviewFiles } from '@/utils/jinja2Compat'
import { listEnabledModels } from '@/api/admin'

const route = useRoute()
const inputText = ref('')
const streamText = ref('')
const generating = ref(false)
const files = ref([])            // 文件树列表 [{path, language, size}]
const activeFile = ref('')       // 当前选中文件路径
const fileContent = ref('')      // 当前文件内容
const projectId = ref(null)      // 后端项目 ID
const projectType = ref('frontend')
const projectTitle = ref('')     // 项目标题（用于保存）
// 多项目支持
const projects = ref([])

// 模型选择
const availableModels = ref([])
const selectedModelId = ref(null)
const selectedModelName = ref('默认模型')
const showModelSelect = ref(false)

const PROJECT_MODEL_KEY = 'codeforge_project_model'

async function loadProjectModels() {
  try {
    const res = await listEnabledModels()
    availableModels.value = res.data || []
    const saved = localStorage.getItem(PROJECT_MODEL_KEY)
    if (saved) {
      const m = availableModels.value.find(x => x.id === Number(saved))
      if (m) { selectedModelId.value = m.id; selectedModelName.value = m.name; return }
    }
    const def = availableModels.value.find(m => m.isDefault)
    if (def) { selectedModelId.value = def.id; selectedModelName.value = def.name }
    else if (availableModels.value.length > 0) {
      selectedModelId.value = availableModels.value[0].id
      selectedModelName.value = availableModels.value[0].name
    }
  } catch { /* ignore */ }
}
function selectProjectModel(m) {
  selectedModelId.value = m.id
  selectedModelName.value = m.name
  showModelSelect.value = false
  localStorage.setItem(PROJECT_MODEL_KEY, String(m.id))
}

const editMode = ref(false)
let abortController = null

// 预览状态
const previewFiles = ref({})
const previewReady = ref(false)
const sandpackVisible = ref(false)
const sandpackError = ref(false)
const sandpackKey = ref(0)
const previewWidth = ref(420)
const isResizing = ref(false)

// 编辑模式状态
const selectedElement = ref(null)
const modifyPrompt = ref('')
const modifying = ref(false)
const previewError = ref(null)
const editAbort = ref(null)

const isFrontend = computed(() => projectType.value === 'frontend')

// editMode 切换时强制后端 iframe 重新挂载（注入/移除选择器）
watch(editMode, () => { sandpackKey.value++ })

/** 是否展示预览面板（前端项目 或 包含 HTML 模板的后端项目） */
const showPreview = computed(() => {
  if (projectType.value === 'frontend') return true
  // 后端项目：检查是否有 HTML 模板文件
  if (projectType.value === 'backend') {
    return files.value.some(f => /\.html?$/i.test(f.path))
  }
  return false
})

/** 是否为后端项目的静态预览（Jinja2 模板转换） */
const isBackendPreview = computed(() => projectType.value === 'backend' && showPreview.value)

/** 用于沙箱预览的文件（后端项目会预处理 Jinja2 模板） */
const displayFiles = computed(() => {
  if (projectType.value === 'backend') {
    const { files: processed } = preparePreviewFiles(previewFiles.value, 'backend')
    return processed
  }
  return previewFiles.value
})

/** 后端项目预览入口文件路径 */
const backendEntryPath = computed(() => {
  if (projectType.value !== 'backend') return ''
  const { entryPath } = preparePreviewFiles(previewFiles.value, 'backend')
  return entryPath
})

/** 注入元素选择器脚本到 HTML（用于后端项目编辑模式） */
function injectPickerScript(html) {
  const PICKER = `<script>
(function() {
  var ov = document.createElement("div");
  ov.id = "__picker";
  ov.style.cssText = "position:fixed;pointer-events:none;border:2px solid #f56c6c;background:rgba(245,108,108,.08);z-index:99999;display:none;border-radius:2px;transition:all .1s";
  document.body.appendChild(ov);
  document.addEventListener("mouseover", function(e) {
    var t = e.target;
    if (t === ov || t.id === "__picker" || t.id === "__err" || t.closest("#__err")) return;
    var r = t.getBoundingClientRect();
    ov.style.display = "block";
    ov.style.top = r.top + "px";
    ov.style.left = r.left + "px";
    ov.style.width = r.width + "px";
    ov.style.height = r.height + "px";
  }, true);
  document.addEventListener("mouseout", function(e) {
    if (e.target === ov || e.target.id === "__picker") return;
    ov.style.display = "none";
  }, true);
  document.addEventListener("click", function(e) {
    e.preventDefault();
    e.stopPropagation();
    var t = e.target;
    if (t === ov || t.id === "__picker" || t.id === "__err" || t.closest("#__err")) return;
    ov.style.borderColor = "#67c23a";
    ov.style.background = "rgba(103,194,58,.1)";
    var info = {
      tag: t.tagName ? t.tagName.toLowerCase() : "",
      id: t.id || "",
      cls: (t.className || "").toString(),
      text: (t.textContent || "").substring(0, 80),
      html: (t.outerHTML || "").substring(0, 500)
    };
    window.parent.postMessage({ type: "element-selected", payload: info }, "*");
  }, true);
})();
<\\/script>`
  return html.replace(/<\/body>/i, PICKER + '\n</body>')
}

/** 后端项目 iframe srcdoc（入口文件的静态 HTML） */
const backendPreviewHtml = computed(() => {
  if (projectType.value !== 'backend') return ''
  const { files: processed, entryPath } = preparePreviewFiles(previewFiles.value, 'backend')
  let html = processed[entryPath] || ''
  if (editMode.value && html) {
    html = injectPickerScript(html)
  }
  return html
})

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

/** 批量加载所有项目文件用于预览 */
async function loadAllFiles() {
  if (!projectId.value || !files.value.length) {
    previewFiles.value = {}
    previewReady.value = false
    return
  }
  // 只有前端项目或包含 HTML 的后端项目才加载预览
  if (projectType.value !== 'frontend' && !files.value.some(f => /\.html?$/i.test(f.path))) {
    previewFiles.value = {}
    previewReady.value = false
    return
  }
  previewReady.value = false
  const results = await Promise.allSettled(
    files.value.map(f => getProjectFile(projectId.value, f.path))
  )
  const map = {}
  files.value.forEach((f, i) => {
    const result = results[i]
    if (result.status === 'fulfilled' && result.value?.data?.content) {
      map[f.path] = result.value.data.content
    }
  })
  previewFiles.value = map
  previewReady.value = true
  await nextTick()
  sandpackVisible.value = true
  sandpackError.value = false
}

function retryPreview() {
  sandpackError.value = false
  sandpackVisible.value = false
  sandpackKey.value++
  nextTick(() => { sandpackVisible.value = true })
}

/** 拖拽调节预览宽度 */
function startResize(e) {
  e.preventDefault()
  const handle = e.currentTarget
  handle.classList.add('resizing')
  isResizing.value = true  // 屏蔽 iframe 鼠标事件
  const startX = e.clientX
  const startW = previewWidth.value
  const onMove = (ev) => {
    const dx = ev.clientX - startX
    previewWidth.value = Math.max(240, Math.min(900, startW - dx))
  }
  const onUp = () => {
    handle.classList.remove('resizing')
    isResizing.value = false
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

/** 监听 sandpack 全局错误 */
function onSandpackError(e) {
  if (e.message && e.message.includes('shell')) {
    sandpackError.value = true
    sandpackVisible.value = false
  }
}

// ===== 编辑模式函数 =====

function toggleEditMode() {
  editMode.value = !editMode.value
  if (!editMode.value) {
    selectedElement.value = null
    modifyPrompt.value = ''
    previewError.value = null
  }
}

function onElementSelected(payload) {
  selectedElement.value = payload
  previewError.value = null
}

function onPreviewError(payload) {
  previewError.value = payload
}

function clearSelection() {
  selectedElement.value = null
  modifyPrompt.value = ''
}

function handleFixError() {
  editMode.value = true
  if (previewError.value) {
    modifyPrompt.value = '修复这个错误: ' + (previewError.value.message || '')
  }
}

function stopModifying() {
  if (editAbort.value) {
    editAbort.value.abort()
    editAbort.value = null
  }
  modifying.value = false
}

async function handleModify() {
  if (!modifyPrompt.value.trim() || modifying.value) return

  const el = selectedElement.value
  const elementInfo = el
    ? `<${el.tag}${el.id ? ' id=' + el.id : ''}${el.cls ? ' class=' + el.cls : ''}> 文本:"${el.text}"`
    : ''

  const promptText = modifyPrompt.value.trim()
  modifyPrompt.value = ''
  modifying.value = true
  previewError.value = null
  editAbort.value = new AbortController()

  try {
    await modifyProjectStream(
      projectId.value,
      { files: previewFiles.value, elementInfo, modifyPrompt: promptText, conversationId: projectId.value, targetPath: activeFile.value },
      {
        signal: editAbort.value.signal,
        onToken: () => {},
        onDone: async (result) => {
          if (result.files && Object.keys(result.files).length > 0) {
            // 检测项目优势文件夹前缀（用于修正 AI 返回的路径）
            const prefixCount = {}
            for (const f of files.value) {
              const slash = f.path.indexOf('/')
              if (slash > 0) {
                const prefix = f.path.substring(0, slash + 1)
                prefixCount[prefix] = (prefixCount[prefix] || 0) + 1
              }
            }
            let dominantPrefix = ''
            const threshold = files.value.length * 2 / 3
            for (const [p, c] of Object.entries(prefixCount)) {
              if (c > threshold) { dominantPrefix = p; break }
            }

            const diskMap = {}  // 收集要保存到磁盘的 actualPath → code 映射
            for (const [path, code] of Object.entries(result.files)) {
              if (!path) continue
              // 找到匹配的文件路径（可能带项目目录前缀）
              const match = files.value.find(f => f.path === path || f.path.endsWith('/' + path) || f.path.endsWith(path))
              let actualPath
              if (match) {
                actualPath = match.path
              } else if (dominantPrefix && !path.startsWith(dominantPrefix)) {
                // 新文件或未匹配文件 → 补上优势文件夹前缀
                actualPath = dominantPrefix + path
              } else {
                actualPath = path
              }
              previewFiles.value[actualPath] = code
              diskMap[actualPath] = code
            }
            // 写回磁盘
            try {
              await saveProjectFiles(projectId.value, diskMap)
            } catch (e) {
              console.error('保存修改到磁盘失败:', e)
              ElMessage.error('文件保存失败: ' + (e.response?.data?.message || e.message || '未知错误'))
            }
            sandpackKey.value++
            sandpackVisible.value = true
            if (activeFile.value && result.files[activeFile.value]) {
              fileContent.value = result.files[activeFile.value]
            }
            ElMessage.success('修改完成')
          } else if (result.code) {
            // 单文件修改
            if (activeFile.value) {
              previewFiles.value[activeFile.value] = result.code
              fileContent.value = result.code
              // 写回磁盘
              try {
                await saveProjectFiles(projectId.value, { [activeFile.value]: result.code })
              } catch (e) {
                console.error('保存修改到磁盘失败:', e)
                ElMessage.error('文件保存失败: ' + (e.response?.data?.message || e.message || '未知错误'))
              }
            }
            sandpackKey.value++
            sandpackVisible.value = true
            ElMessage.success('修改完成')
          } else {
            ElMessage.warning('AI 未返回可识别的文件修改')
          }
          modifying.value = false
          selectedElement.value = null
          editAbort.value = null
        },
        onError: (err) => {
          ElMessage.error(err.message || '修改失败')
          modifying.value = false
          editAbort.value = null
        }
      }
    )
  } catch (err) {
    if (err.name !== 'AbortError') {
      ElMessage.error(err.message || '网络错误')
    }
    modifying.value = false
    editAbort.value = null
  }
}

// Escape 取消修改
function onKeydown(e) {
  if (e.key === 'Escape' && modifying.value) { stopModifying() }
}

// ===== 项目管理 =====

/** 从后端加载项目列表 */
async function loadProjectsFromBackend() {
  try {
    const res = await listConversations({ type: 'ENGINEERING' })
    if (res.data && res.data.length > 0) {
      res.data.forEach(c => {
        if (!projects.value.find(p => p.id === c.id)) {
          projects.value.push({ id: c.id, title: c.title || '未命名项目', fileCount: 0 })
        }
      })
    }
  } catch { /* 后端不可用则跳过 */ }
}

/** 从文件路径后缀推断语言 */
function detectLangFromPath(path) {
  const ext = path.split('.').pop()?.toLowerCase()
  const map = { vue: 'vue', js: 'javascript', ts: 'typescript', html: 'html', htm: 'html',
    css: 'css', scss: 'scss', json: 'json', py: 'python', java: 'java', xml: 'xml',
    md: 'markdown', yml: 'yaml', yaml: 'yaml', sql: 'sql', go: 'go', rs: 'rust' }
  return map[ext] || 'text'
}

/** 从文件扩展名检测项目类型 */
function detectProjectType(fileList) {
  const exts = fileList.map(f => {
    const parts = f.path.split('.')
    return parts.length > 1 ? parts.pop().toLowerCase() : ''
  })
  const backendExts = ['py', 'java', 'go', 'rb', 'php', 'cs', 'rs', 'cpp', 'c', 'kt', 'swift', 'scala']
  const frontendExts = ['vue', 'jsx', 'tsx', 'js', 'ts', 'html', 'htm', 'css', 'scss', 'less']
  const hasBackend = exts.some(e => backendExts.includes(e))
  const hasFrontend = exts.some(e => frontendExts.includes(e))
  // 后端优先判定：有后端语言且无前端框架文件 → backend
  if (hasBackend && !exts.includes('vue') && !exts.includes('jsx') && !exts.includes('tsx')) {
    return 'backend'
  }
  return 'frontend'
}

/** 切换项目 */
async function switchProject(id) {
  if (id === projectId.value) return
  projectId.value = id
  // 清理编辑状态
  editMode.value = false; selectedElement.value = null; modifyPrompt.value = ''
  previewFiles.value = {}; previewReady.value = false; sandpackVisible.value = false
  try {
    const treeRes = await getProjectTree(id)
    files.value = treeRes.data || []
    // 从文件列表检测项目类型
    if (files.value.length > 0) {
      projectType.value = detectProjectType(files.value)
      selectFile(files.value[0].path)
      await loadAllFiles()
    }
    // 更新文件数
    const p = projects.value.find(p => p.id === id)
    if (p) p.fileCount = files.value.length
  } catch { ElMessage.error('加载项目失败') }
}

/** 删除项目 */
async function handleDeleteProject(p) {
  try {
    await ElMessageBox.confirm(`确定删除项目「${p.title || '未命名'}」吗？磁盘文件和对话记录将被清除。`, '删除确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }
  try {
    await deleteProject(p.id)
    projects.value = projects.value.filter(x => x.id !== p.id)
    if (projectId.value === p.id) startNewProject()
    ElMessage.success('项目已删除')
  } catch { ElMessage.error('删除失败') }
}

/** 新建项目 */
function startNewProject() {
  projectId.value = null; files.value = []; activeFile.value = ''; fileContent.value = ''
  previewFiles.value = {}; previewReady.value = false; sandpackVisible.value = false
  editMode.value = false; selectedElement.value = null; modifyPrompt.value = ''
  inputText.value = ''; streamText.value = ''
  projectType.value = 'frontend'
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
  const userPrompt = inputText.value.trim()
  projectTitle.value = userPrompt.substring(0, 50)
  inputText.value = ''
  generating.value = true; streamText.value = ''
  files.value = []; activeFile.value = ''; fileContent.value = ''
  abortController = new AbortController()

  try {
    await generateProject(
      { prompt: userPrompt, type: 'ENGINEERING', language: 'text', modelId: selectedModelId.value },
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
              await loadAllFiles()
            }
            // 添加到项目列表
            if (!projects.value.find(p => p.id === projectId.value)) {
              projects.value.unshift({
                id: projectId.value,
                title: projectTitle.value,
                fileCount: files.value.length
              })
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
    const { value: customName } = await ElMessageBox.prompt(
      '请输入项目名称',
      '保存到我的应用',
      {
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        inputValue: projectTitle.value || '',
        inputPlaceholder: '例如：博客系统、商城首页',
        inputValidator: (v) => v && v.trim() ? true : '名称不能为空'
      }
    )
    if (customName) {
      projectTitle.value = customName.trim()
      await saveProject(projectId.value, projectTitle.value, projectType.value)
      ElMessage.success(`「${projectTitle.value}」已保存到我的应用`)
    }
  } catch { /* 用户取消 */ }
}

// 下载
async function handleDownload() {
  if (!projectId.value) return
  try {
    await downloadProjectZip(projectId.value)
  } catch (e) {
    ElMessage.error('下载失败: ' + (e.message || '无法连接'))
  }
}

onMounted(async () => {
  await loadProjectModels()
  await loadProjectsFromBackend()
  // 从"我的应用"跳转过来 → 自动加载对应项目
  const pid = route.query.projectId
  if (pid) {
    const id = Number(pid)
    if (!isNaN(id) && projects.value.find(p => p.id === id)) {
      await switchProject(id)
    } else if (!isNaN(id)) {
      projectId.value = id
      let loaded = false
      // 尝试从磁盘加载
      try {
        const treeRes = await getProjectTree(id)
        if (treeRes.data && treeRes.data.length > 0) {
          files.value = treeRes.data
          loaded = true
        }
      } catch { /* 磁盘文件可能已被清除 */ }

      // 磁盘为空 → 从"我的应用"数据库重建
      if (!loaded) {
        try {
          const appRes = await getApplication(id)
          const app = appRes.data
          if (app && app.sourceCode) {
            // 解析 sourceCode 中的 // ===== path ===== 标记重建文件
            const rebuiltFiles = []
            const parts = app.sourceCode.split(/\/\/\s*={3,}\s*(.+?)\s*={3,}\s*/)
            for (let i = 1; i < parts.length; i += 2) {
              const path = parts[i].trim()
              const code = (parts[i + 1] || '').trim()
              if (path && code) {
                rebuiltFiles.push({ path, language: detectLangFromPath(path), content: code, size: code.length })
              }
            }
            if (rebuiltFiles.length > 0) {
              // 写回磁盘
              const diskMap = {}
              rebuiltFiles.forEach(f => { diskMap[f.path] = f.content })
              try { await saveProjectFiles(id, diskMap) } catch {}
              files.value = rebuiltFiles
              loaded = true
            }
          }
          if (app && app.name) {
            projectTitle.value = app.name
          }
        } catch { /* 应用也不存在 */ }
      }

      if (loaded) {
        projectType.value = detectProjectType(files.value)
        projects.value.unshift({ id, title: projectTitle.value || '从应用导入', fileCount: files.value.length })
        if (files.value.length > 0) {
          selectFile(files.value[0].path)
          await loadAllFiles()
        }
      }
    }
  }
  window.addEventListener('error', onSandpackError)
  document.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => {
  window.removeEventListener('error', onSandpackError)
  document.removeEventListener('keydown', onKeydown)
  abortController?.abort()
  editAbort.value?.abort()
})
</script>

<style scoped>
.project-create { display: flex; flex-direction: column; height: calc(100vh - 60px); background: #0d1117; color: #c9d1d9; --accent: #7c8aff; --bg-sidebar: #0a0d13; --bg-card: #141821; --border: rgba(255,255,255,0.07); --text: #c9d1d9; --text-dim: #6b7280; }
.proj-header { display: flex; align-items: center; justify-content: space-between; padding: 0 20px; height: 48px; background: var(--bg-sidebar); border-bottom: 1px solid var(--border); flex-shrink: 0; }
.header-left { display: flex; align-items: center; gap: 10px; }
.logo-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--accent); }
.logo-text { font-size: 15px; font-weight: 600; color: #e5e7eb; }
.header-right { display: flex; gap: 8px; }
.model-selector { display: flex; align-items: center; }
.model-btn { display: flex; align-items: center; gap: 4px; padding: 6px 12px; background: var(--bg-card); color: var(--text); border: 1px solid var(--border); border-radius: 6px; font-size: 12px; cursor: pointer; white-space: nowrap; transition: all .15s; }
.model-btn:hover { border-color: var(--accent); }
.model-dropdown { position: absolute; top: 36px; right: 0; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius); box-shadow: var(--shadow-lg); z-index: 200; min-width: 220px; padding: 4px 0; }
.model-item { padding: 8px 14px; font-size: 13px; color: var(--text-primary); cursor: pointer; transition: background .15s; }
.model-item:hover { background: var(--bg-hover); }
.model-item.active { background: var(--accent-bg); color: var(--accent); }
.model-item.muted { color: var(--text-dim); cursor: default; }
.btn-hdr { display: flex; align-items: center; gap: 4px; padding: 6px 14px; background: var(--bg-card); color: var(--text); border: 1px solid var(--border); border-radius: 6px; font-size: 13px; cursor: pointer; transition: all .15s; }
.btn-hdr:hover:not(:disabled) { background: rgba(124,138,255,0.1); border-color: var(--accent); }
.btn-hdr:disabled { opacity: 0.4; cursor: default; }
.btn-hdr.btn-new { background: rgba(124,138,255,0.12); border-color: rgba(124,138,255,0.25); color: var(--accent); }
.btn-hdr.btn-new:hover { background: rgba(124,138,255,0.2); }
.proj-main { display: flex; flex: 1; min-height: 0; }
.panel-left { width: 240px; flex-shrink: 0; display: flex; flex-direction: column; overflow: hidden; }
.project-list { border-bottom: 1px solid var(--border); max-height: 200px; overflow-y: auto; flex-shrink: 0; }
.list-label { padding: 8px 14px; font-size: 11px; color: var(--text-dim); text-transform: uppercase; letter-spacing: 1px; }
.list-empty { padding: 12px 14px; font-size: 12px; color: var(--text-dim); text-align: center; }
.project-item { padding: 8px 14px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; transition: background .15s; }
.project-item:hover { background: rgba(124,138,255,0.06); }
.project-item.active { background: rgba(124,138,255,0.12); border-left: 2px solid var(--accent); }
.project-item-name { font-size: 13px; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.project-item-count { font-size: 11px; color: var(--text-dim); flex-shrink: 0; margin-left: 8px; }
.project-item-del { display: none; flex-shrink: 0; width: 20px; height: 20px; border: none; border-radius: 50%; background: transparent; color: var(--text-dim); font-size: 16px; line-height: 18px; cursor: pointer; margin-left: 6px; transition: all .2s; }
.project-item:hover .project-item-del { display: inline-block; }
.project-item-del:hover { background: #e74c3c; color: #fff; }
.panel-tree { flex: 1; overflow: hidden; }
.panel-center { flex: 1; display: flex; flex-direction: column; min-width: 0; border-right: 1px solid var(--border); }
.panel-preview { flex-shrink: 0; display: flex; flex-direction: column; background: #1a1e2a; overflow: hidden; }
.resize-handle {
  width: 10px;
  flex-shrink: 0;
  cursor: col-resize;
  background: transparent;
  transition: background .2s;
  z-index: 10;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
}
.resize-handle:hover {
  background: rgba(124, 138, 255, 0.08);
}
.resize-handle:active,
.resize-handle.resizing {
  background: rgba(124, 138, 255, 0.15);
}
/* 中间竖线指示条 */
.resize-handle::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 2px;
  height: 40px;
  border-radius: 1px;
  background: rgba(255, 255, 255, 0.12);
  transition: background .2s, height .2s;
}
.resize-handle:hover::after {
  background: rgba(124, 138, 255, 0.5);
  height: 56px;
}
.resize-handle:active::after,
.resize-handle.resizing::after {
  background: var(--accent);
  height: 80px;
  box-shadow: 0 0 6px rgba(124, 138, 255, 0.4);
}
/* 三点指示器 */
.resize-grip {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity .2s;
  position: relative;
  z-index: 1;
}
.resize-handle:hover .resize-grip,
.resize-handle.resizing .resize-grip {
  opacity: 1;
}
.resize-grip span {
  width: 2px;
  height: 2px;
  border-radius: 50%;
  background: var(--accent);
}
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
.preview-toolbar { display: flex; align-items: center; gap: 12px; padding: 8px 12px; background: #141821; border-bottom: 1px solid var(--border); flex-shrink: 0; }
.preview-label { font-size: 12px; color: #c9d1d9; font-weight: 500; }
.preview-retry { padding: 2px 10px; background: rgba(239,68,68,0.15); color: #ef4444; border: 1px solid rgba(239,68,68,0.3); border-radius: 4px; font-size: 11px; cursor: pointer; margin-left: auto; }
.preview-container { flex: 1; min-height: 0; }
.preview-container.no-pointer { pointer-events: none; }
.preview-empty { flex: 1; display: flex; align-items: center; justify-content: center; color: #999; font-size: 14px; padding: 20px; background: #1a1e2a; }
.preview-error { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; color: #ef4444; font-size: 14px; }
.preview-error button { padding: 6px 16px; background: var(--accent); color: #fff; border: none; border-radius: 6px; cursor: pointer; }

/* ===== 编辑模式 ===== */
.btn-edit-mode { padding: 3px 10px; background: rgba(124,138,255,0.08); color: var(--accent); border: 1px solid rgba(124,138,255,0.2); border-radius: 4px; font-size: 11px; cursor: pointer; white-space: nowrap; transition: all .15s; }
.btn-edit-mode:hover { background: rgba(124,138,255,0.15); border-color: rgba(124,138,255,0.35); }
.btn-edit-mode.active { background: rgba(239,68,68,0.15); color: #ef4444; border-color: rgba(239,68,68,0.3); }
.preview-error-banner { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: rgba(239,68,68,0.08); border-bottom: 1px solid rgba(239,68,68,0.15); flex-shrink: 0; }
.preview-error-banner .error-msg { font-size: 12px; color: #fca5a5; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.btn-fix-error { padding: 3px 10px; background: rgba(124,138,255,0.15); color: var(--accent); border: 1px solid rgba(124,138,255,0.25); border-radius: 4px; font-size: 11px; cursor: pointer; white-space: nowrap; }
.btn-fix-error:hover { background: rgba(124,138,255,0.25); }
.edit-info-bar { display: flex; align-items: center; gap: 8px; padding: 6px 12px; background: rgba(124,138,255,0.06); border-bottom: 1px solid rgba(124,138,255,0.1); font-size: 12px; color: #c9d1d9; flex-shrink: 0; }
.edit-info-bar b { color: #a5b4fc; }
.edit-info-bar .el-text-preview { color: #6b7280; }
.btn-clear-sel { margin-left: auto; background: transparent; color: #9ca3af; border: 1px solid rgba(255,255,255,0.1); border-radius: 3px; padding: 2px 8px; font-size: 11px; cursor: pointer; }
.btn-clear-sel:hover { background: rgba(239,68,68,0.1); color: #ef4444; }
.edit-modify-bar { display: flex; gap: 8px; padding: 8px 12px; background: #141821; border-top: 1px solid rgba(255,255,255,0.07); flex-shrink: 0; }
.modify-input { flex: 1; padding: 7px 12px; background: #1a1e2a; color: #c9d1d9; border: 1px solid rgba(255,255,255,0.1); border-radius: 6px; font-size: 13px; outline: none; font-family: inherit; }
.modify-input:focus { border-color: rgba(124,138,255,0.4); }
.modify-input:disabled { opacity: 0.5; }
.modify-input::placeholder { color: #4b5563; }
.btn-modify { padding: 7px 16px; background: linear-gradient(135deg, var(--accent), #7c3aed); color: #fff; border: none; border-radius: 6px; font-size: 13px; cursor: pointer; white-space: nowrap; flex-shrink: 0; }
.btn-modify:hover:not(:disabled) { transform: scale(1.02); }
.btn-modify:disabled { opacity: 0.4; cursor: default; }
.btn-stop-modify { width: 32px; height: 32px; background: rgba(239,68,68,0.15); color: #ef4444; border: 1px solid rgba(239,68,68,0.25); border-radius: 6px; cursor: pointer; font-size: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.btn-stop-modify:hover { background: rgba(239,68,68,0.25); }

/* ===== 后端项目预览 ===== */
.backend-tag {
  font-size: 11px;
  padding: 2px 10px;
  background: rgba(251, 191, 36, 0.12);
  color: #fbbf24;
  border: 1px solid rgba(251, 191, 36, 0.2);
  border-radius: 10px;
  white-space: nowrap;
}
.backend-banner {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(251, 191, 36, 0.06);
  border-bottom: 1px solid rgba(251, 191, 36, 0.1);
  flex-shrink: 0;
}
.backend-banner-icon {
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 1px;
}
.backend-banner-text {
  font-size: 11px;
  color: #a3a3a3;
  line-height: 1.5;
}
.backend-banner-text code {
  padding: 1px 6px;
  background: rgba(0,0,0,0.3);
  border-radius: 3px;
  font-size: 10px;
  color: #d4d4d4;
  font-family: 'SF Mono', 'Fira Code', monospace;
}
.preview-iframe {
  flex: 1;
  width: 100%;
  border: none;
  min-height: 0;
  background: #fff;
}
</style>
