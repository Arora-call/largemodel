<template>
  <div class="workspace">
    <!-- ===== 顶部工具栏 ===== -->
    <header class="ws-header">
      <div class="header-left">
        <h2 class="logo">CodeForge <span class="beta">Beta</span></h2>
      </div>
      <!-- 模式选择 -->
      <div class="mode-tabs">
        <button v-for="m in modes" :key="m.key"
          :class="['mode-btn', { active: currentMode === m.key }]"
          @click="switchMode(m.key)">
          <i :class="m.icon"></i> {{ m.label }}
        </button>
      </div>
      <!-- 模型选择 -->
      <div class="header-right">
        <!-- 知识库 -->
        <el-popover
          v-model:visible="kbPopoverVisible"
          placement="bottom"
          :width="320"
          trigger="click"
        >
          <template #reference>
            <el-button size="small"
              :type="selectedKbDocs.length > 0 || autoSearchKb ? 'warning' : 'default'"
              @click="loadKnowledgeDocs">
              📚 知识库
              <span v-if="selectedKbDocs.length" class="kb-badge">{{ selectedKbDocs.length }}</span>
            </el-button>
          </template>
          <div class="kb-popover">
            <div class="kb-pop-header">
              <el-switch v-model="autoSearchKb" size="small" active-text="自动检索" />
              <span class="kb-hint" v-if="autoSearchKb">根据问题自动匹配文档</span>
            </div>
            <el-divider style="margin:8px 0" />
            <div class="kb-pop-list" v-loading="kbLoading">
              <div v-if="!knowledgeDocs.length" class="kb-empty">暂无知识库文档</div>
              <el-checkbox-group v-model="selectedKbDocs" :disabled="autoSearchKb">
                <div v-for="doc in knowledgeDocs" :key="doc.id"
                  class="kb-doc-item">
                  <el-checkbox :value="doc.id" :label="doc.id">
                    <span class="kb-doc-title">{{ doc.title || doc.fileName }}</span>
                  </el-checkbox>
                  <span class="kb-doc-type">{{ doc.docType }}</span>
                </div>
              </el-checkbox-group>
            </div>
          </div>
        </el-popover>

        <el-select v-model="selectedModelId" size="small" placeholder="默认模型default"
          style="width: 180px" @change="persistModel">
          <el-option :value="null" label="默认模型" />
          <el-option v-for="m in availableModels" :key="m.id"
            :value="m.id" :label="m.name" />
        </el-select>
        <el-button size="small" @click="handleExport" :disabled="!currentConv">
          <i class="fa fa-download"></i> 导出
        </el-button>
        <el-button size="small" @click="handleSaveAsApp" :disabled="!messages.length">
          <i class="fa fa-save"></i> 保存到应用
        </el-button>
      </div>
    </header>

    <!-- ===== 主体三栏布局 ===== -->
    <div class="ws-body">
      <!-- 左侧：对话/项目列表 -->
      <aside class="ws-sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-actions">
          <el-button type="primary" size="small" @click="newConversation" style="width:100%">
            <i class="fa fa-plus"></i> 新建{{ modeLabel }}
          </el-button>
        </div>
        <div class="conv-list">
          <div v-for="c in conversations" :key="c.id"
            :class="['conv-item', { active: currentConv?.id === c.id }]"
            @click="switchConversation(c)">
            <span class="conv-title">{{ c.title || '新对话' }}</span>
            <span class="conv-time">{{ formatTime(c.updatedAt) }}</span>
            <el-button text size="small" @click.stop="deleteConversation(c)"
              class="conv-del"><i class="fa fa-trash"></i></el-button>
          </div>
          <el-empty v-if="!conversations.length" description="暂无对话" :image-size="60" />
        </div>
        <div class="sidebar-footer">
          <el-button text size="small" @click="sidebarCollapsed = !sidebarCollapsed">
            <i :class="sidebarCollapsed ? 'fa fa-angle-right' : 'fa fa-angle-left'"></i>
          </el-button>
        </div>
      </aside>

      <!-- 中间：AI 对话区 -->
      <main class="ws-chat">
        <!-- 欢迎页 -->
        <div v-if="!messages.length && !generating" class="welcome">
          <h1>{{ modeLabel }}模式</h1>
          <p class="desc">{{ modeDesc }}</p>
          <div class="quick-prompts">
            <div v-for="p in quickPrompts" :key="p" class="quick-item"
              @click="inputText = p; handleGenerate()">{{ p }}</div>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="messages" ref="msgContainer">
          <div v-for="(msg, i) in messages" :key="i"
            :class="['msg', msg.role === 'user' ? 'msg-user' : 'msg-ai']">
            <div class="msg-avatar">
              <i :class="msg.role === 'user' ? 'fa fa-user' : 'fa fa-robot'"></i>
            </div>
            <div class="msg-body">
              <!-- 用户消息 -->
              <div v-if="msg.role === 'user'" class="msg-text">{{ msg.content }}</div>
              <!-- AI 文本 -->
              <div v-if="msg.role === 'ai' && msg.text" class="msg-text markdown"
                v-html="renderMarkdown(msg.text)"></div>
              <!-- AI 文件列表（可折叠文件树） -->
              <div v-if="msg.role === 'ai' && msg.files?.length" class="msg-files">
                <div class="files-toggle" @click="toggleFiles(i)">
                  <i :class="expandedFiles[i] ? 'fa fa-angle-down' : 'fa fa-angle-right'"></i>
                  📁 项目文件 ({{ msg.files.length }})
                </div>
                <div v-if="expandedFiles[i]" class="files-tree">
                  <div v-for="item in asFileTree(msg.files)" :key="item.path"
                    :class="['tree-item', { 'is-dir': item.isDir, 'is-file': !item.isDir }]"
                    :style="{ paddingLeft: (item.depth * 20 + 12) + 'px' }"
                    @click="!item.isDir && item.content && previewFile(item)">
                    <span class="tree-name">{{ item.name }}</span>
                    <span v-if="!item.isDir && item.size" class="tree-size">{{ formatSize(item.size) }}</span>
                  </div>
                </div>
              </div>
              <!-- 中断续写 -->
              <div v-if="msg.interrupted" class="interrupted-hint">
                <el-button size="small" @click="handleContinue(i)">继续生成</el-button>
              </div>
            </div>
          </div>

          <!-- 流式生成中 -->
          <div v-if="generating" class="msg msg-ai">
            <div class="msg-avatar"><i class="fa fa-spinner fa-pulse"></i></div>
            <div class="msg-body">
              <div v-if="streamText" class="streaming-text">{{ streamText }}<span class="cursor-blink">|</span></div>
              <div v-else class="streaming-wait">等待 AI 响应<span class="wait-pulse">...</span></div>
            </div>
          </div>
        </div>

        <!-- 底部输入区 -->
        <div class="input-bar">
          <el-input v-model="inputText" type="textarea" :rows="2"
            placeholder="描述你想要生成的代码..." :disabled="generating"
            @keydown.enter.exact.prevent="handleGenerate()" />
          <el-button v-if="!generating" type="primary" @click="handleGenerate"
            :disabled="!inputText.trim()">
            <i class="fa fa-paper-plane"></i> 发送
          </el-button>
          <el-button v-else type="danger" @click="stopGenerating">
            <i class="fa fa-stop"></i> 停止
          </el-button>
        </div>
      </main>

      <!-- 拖拽调节预览宽度 -->
      <div v-if="showPreview && (previewContent || deployedUrl)" class="resize-handle" @mousedown="startResize">
        <div class="resize-grip"><span></span><span></span><span></span></div>
      </div>

      <!-- 右侧：预览面板 -->
      <aside v-if="showPreview && (previewContent || deployedUrl)" class="ws-preview" :style="{ width: previewWidth + 'px' }">
        <div class="preview-header">
          <span class="preview-label">
            <span v-if="deployedUrl" class="deploy-badge" title="Nginx 部署预览">🚀</span>
            <span v-else>📦</span>
            {{ previewFileName }}
          </span>
          <div class="preview-actions">
            <el-button
              size="small"
              :type="pickerActive ? 'success' : 'default'"
              @click="togglePicker"
              title="选取页面元素以精准定位修改">
              🎯 {{ pickerActive ? '选取中...' : '选取元素' }}
            </el-button>
            <el-button v-if="!deployedUrl && currentMode !== 'SINGLE_FILE'"
              size="small" type="warning" :loading="isDeploying"
              @click="handleDeploy" title="部署到 Nginx 预览">
              🚀 部署预览
            </el-button>
            <el-button v-if="deployedUrl" size="small" text type="success"
              @click="deployedUrl = ''" title="切换回内联预览">
              🔄 内联预览
            </el-button>
            <el-button size="small" text @click="showPreview = false"><i class="fa fa-times"></i></el-button>
          </div>
        </div>
        <!-- 已选取的元素气泡 -->
        <div v-if="pickedElements.length" class="picked-elements-bar">
          <span class="picked-label">已选元素:</span>
          <span v-for="(el, i) in pickedElements" :key="el.filePath + '|' + el.selector + '|' + i" class="picked-tag">
            <code>{{ el.filePath ? el.filePath.split('/').pop() + ' → ' : '' }}{{ el.selector }}</code>
            <button class="picked-tag-close" @click="removePickedElement(i)" title="取消选中">×</button>
          </span>
        </div>

        <div class="preview-body" :class="{ 'no-pointer': isResizing }">
          <!-- Nginx 部署预览 -->
          <iframe v-if="deployedUrl"
            ref="htmlPreview" class="preview-iframe"
            :key="'deploy-' + previewVersion"
            :src="deployedUrl" @load="onPreviewLoad"></iframe>
          <!-- 部署失败提示 -->
          <div v-else-if="previewContent === 'deploy_failed'"
            class="deploy-fallback">
            <div class="fallback-icon">⚠️</div>
            <p v-if="previewError" class="fallback-msg">{{ previewError }}</p>
            <p v-else>Nginx 部署未就绪，请检查 Nginx 是否启动</p>
          </div>
          <!-- 内联预览：srcdoc -->
          <iframe v-else-if="previewHtml && currentMode !== 'VUE_PROJECT'"
            ref="htmlPreview" class="preview-iframe"
            :key="'preview-' + previewVersion"
            :srcdoc="previewHtml" @load="onPreviewLoad"></iframe>
          <!-- Vue3 项目：Sandpack（即时预览 / Nginx 部署前的 fallback） -->
          <SandboxPreview v-else-if="currentMode === 'VUE_PROJECT'"
            ref="sandboxRef" :key="sandpackKey"
            :files="previewFilesMap"
            :editMode="pickerActive"
            @element-selected="handleSandboxElementPicked"
            @error="previewError = $event" />
        </div>
        <div v-if="deployedUrl" class="deploy-url-bar">
          <span class="deploy-key">{{ deployedUrl }}</span>
          <el-button size="small" text @click="copyDeployUrl"><i class="fa fa-copy"></i></el-button>
        </div>
        <div v-if="previewError" class="preview-error">{{ previewError }}</div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { unifiedGenerateStream, modifyCodeStream, deployConversation } from '@/api/ai'
import { listConversations, getMessages, deleteConversation as delConv, exportConversation, saveApplication, getApplication } from '@/api/app'
import { listEnabledModels } from '@/api/admin'
import { listDocuments } from '@/api/knowledge'
import markdownit from 'markdown-it'
import SandboxPreview from '@/components/SandboxPreview.vue'

const md = markdownit({ html: true, breaks: true, linkify: true })
const route = useRoute()

// ─── 模式配置 ───

const modes = [
  { key: 'SINGLE_FILE', label: '单文件', icon: 'fa fa-file-code-o',
    desc: '生成单个 HTML 文件，CSS/JS 内联' },
  { key: 'MULTI_FILE', label: '多文件', icon: 'fa fa-files-o',
    desc: '生成分离的 HTML/CSS/JS 三个文件' },
  { key: 'VUE_PROJECT', label: 'Vue3项目', icon: 'fa fa-cubes',
    desc: '生成完整的 Vue3 + Vite 工程项目' },
]
const quickPromptsMap = {
  SINGLE_FILE: ['创建一个个人作品集页面', '做一个待办事项列表', '生成一个登录页面'],
  MULTI_FILE: ['创建一个企业官网首页', '做一个图片画廊展示页', '生成一个天气预报页面'],
  VUE_PROJECT: ['创建一个电商商品列表页', '做一个博客系统', '生成一个后台管理 dashboard'],
}

const currentMode = ref('SINGLE_FILE')
const modeLabel = computed(() => modes.find(m => m.key === currentMode.value)?.label || '')
const modeDesc = computed(() => modes.find(m => m.key === currentMode.value)?.desc || '')
const quickPrompts = computed(() => quickPromptsMap[currentMode.value] || [])

// ─── 模型 ───

const availableModels = ref([])
const selectedModelId = ref(null)

// ─── 对话 ───

const conversations = ref([])
const currentConv = ref(null)
const messages = ref([])
const sidebarCollapsed = ref(false)

// ─── 生成状态 ───

const inputText = ref('')
const streamText = ref('')
const generating = ref(false)
let abortController = null

// ─── 知识库 ───
const knowledgeDocs = ref([])
const selectedKbDocs = ref([])
const autoSearchKb = ref(false)
const kbPopoverVisible = ref(false)
const kbLoading = ref(false)

async function loadKnowledgeDocs() {
  kbLoading.value = true
  try {
    const res = await listDocuments({ page: 0, size: 50 })
    knowledgeDocs.value = res.data?.content || []
  } catch { knowledgeDocs.value = [] }
  finally { kbLoading.value = false }
}

function toggleKbDoc(docId) {
  const idx = selectedKbDocs.value.indexOf(docId)
  if (idx >= 0) selectedKbDocs.value.splice(idx, 1)
  else selectedKbDocs.value.push(docId)
}

// ─── 预览 ───

const showPreview = ref(false)
const previewContent = ref('')
const previewFileName = ref('')
const previewHtml = ref('')
const previewFilesMap = ref({})
const previewWidth = ref(420)
const previewError = ref(null)
const htmlPreview = ref(null)
const sandboxRef = ref(null)
const sandpackKey = ref(0)
const isResizing = ref(false)
const expandedFiles = reactive({})
const deployedUrl = ref('')     // Nginx 部署后的访问 URL
const isDeploying = ref(false)   // 部署中状态

// ─── 元素选取 ───
const pickerActive = ref(false)
const pickedElements = ref([])   // [{tag, id, classes, text, selector}]
const previewVersion = ref(0)    // 强制 iframe 刷新

/** 切换元素选取模式 */
function togglePicker() {
  pickerActive.value = !pickerActive.value
  if (pickerActive.value) {
    pickedElements.value = []
    // srcdoc iframe: 直接注入脚本
    injectPickerScript()
    // Nginx iframe / Sandbox: 发送 postMessage 激活内置选取器
    sendPickerMessage('cf-picker-activate')
  } else {
    removePickerFromIframe()
    sendPickerMessage('cf-picker-deactivate')
  }
}

/** 向所有预览 iframe 发送激活/停用消息 */
function sendPickerMessage(type) {
  // Nginx deployed iframe
  const iframe = htmlPreview.value
  if (iframe && iframe.contentWindow) {
    try { iframe.contentWindow.postMessage({ type }, '*') } catch (e) { /* cross-origin, message may still work */ }
  }
}

/** 向预览 iframe 注入元素选取脚本 */
function injectPickerScript() {
  const iframe = htmlPreview.value
  if (!iframe) return
  try {
    const doc = iframe.contentDocument || iframe.contentWindow?.document
    if (!doc) return
    // 移除旧脚本（如果存在）
    const old = doc.getElementById('cf-picker-script')
    if (old) old.remove()
    const oldOverlay = doc.getElementById('cf-picker-overlay')
    if (oldOverlay) oldOverlay.remove()
    const oldStyle = doc.getElementById('cf-picker-style')
    if (oldStyle) oldStyle.remove()

    // 注入样式
    const style = doc.createElement('style')
    style.id = 'cf-picker-style'
    style.textContent = `
      .cf-hover-highlight { outline: 2px solid #7c8aff !important; outline-offset: 2px; }
      #cf-picker-overlay { position: fixed; top:0;left:0;width:100%;height:100%;z-index:999999;pointer-events:none; }
    `
    doc.head.appendChild(style)

    // 注入脚本
    const script = doc.createElement('script')
    script.id = 'cf-picker-script'
    script.textContent = `
      (function() {
        var hovered = null
        document.addEventListener('mouseover', function(e) {
          if (hovered) hovered.classList.remove('cf-hover-highlight')
          var el = e.target
          if (!el || el === document.body || el === document.documentElement) return
          el.classList.add('cf-hover-highlight')
          hovered = el
          e.stopPropagation()
        }, true)
        document.addEventListener('mouseout', function(e) {
          if (hovered) { hovered.classList.remove('cf-hover-highlight'); hovered = null }
        }, true)
        document.addEventListener('click', function(e) {
          e.preventDefault()
          e.stopPropagation()
          var el = e.target
          if (!el || el === document.body || el === document.documentElement) return
          // 先移除高亮 class 再读取 className
          el.classList.remove('cf-hover-highlight')
          hovered = null
          var tag = el.tagName.toLowerCase()
          var id = el.id || ''
          // 过滤掉我们注入的 class
          var rawClasses = (el.className && typeof el.className === 'string') ? String(el.className).trim() : ''
          var classes = rawClasses.replace(/\\bcf-hover-highlight\\b/g, '').trim()
          // 取第一个直接文本节点的内容，压缩空白
          var text = ''
          for (var i = 0; i < el.childNodes.length; i++) {
            if (el.childNodes[i].nodeType === 3) { text += el.childNodes[i].textContent; break }
          }
          text = text.replace(/\\s+/g, ' ').trim().substring(0, 60)
          if (!text) {
            // 回退：用 innerText（只取可见文字）截取
            text = (el.innerText || '').replace(/\\s+/g, ' ').trim().substring(0, 60)
          }
          // 构建 CSS 选择器（去掉 cf-hover-highlight）
          var selector = tag
          if (id) selector = '#' + id
          else if (classes) selector = tag + '.' + classes.split(/\\s+/).join('.')
          window.parent.postMessage({
            type: 'cf-pick-element',
            data: { tag: tag, id: id, classes: classes, text: text, selector: selector, filePath: 'index.html' }
          }, '*')
        }, true)
      })()
    `
    doc.body.appendChild(script)
  } catch (e) {
    // 跨域 iframe 无法注入（deployedUrl 跨域场景）
    console.warn('元素选取脚本注入失败（可能跨域）:', e.message)
  }
}

/** 移除 iframe 中的选取脚本 */
function removePickerFromIframe() {
  const iframe = htmlPreview.value
  if (!iframe) return
  try {
    const doc = iframe.contentDocument || iframe.contentWindow?.document
    if (!doc) return
    ;['cf-picker-script', 'cf-picker-overlay', 'cf-picker-style'].forEach(id => {
      const el = doc.getElementById(id)
      if (el) el.remove()
    })
    // 清除所有高亮
    doc.querySelectorAll('.cf-hover-highlight').forEach(el => el.classList.remove('cf-hover-highlight'))
  } catch (e) { /* ignore */ }
}

/** 从预览 iframe 收到消息时处理 */
function handlePreviewMessage(e) {
  // 自己的 injectPickerScript 发出的消息
  if (e.data?.type === 'cf-pick-element' && e.data.data) {
    const d = e.data.data
    if (!pickedElements.value.find(p => p.selector === d.selector)) {
      pickedElements.value.push(d)
    }
  }
  // SandboxPreview CDN 模式发出的消息
  if (e.data?.type === 'element-selected' && e.data.payload) {
    handleSandboxElementPicked(e.data.payload)
  }
}

/** 处理 SandboxPreview（Vue CDN 模式）的元素选取 */
function handleSandboxElementPicked(payload) {
  const tag = payload.tag || 'div'
  const id = payload.id || ''
  const cls = (payload.cls || '').toString().trim()
  // 构建选择器
  let selector = tag
  if (id) selector = '#' + id
  else if (cls) selector = tag + '.' + cls.split(/\s+/).filter(Boolean).join('.')
  // 压缩文本
  const text = (payload.text || '').replace(/\s+/g, ' ').trim().substring(0, 60)
  const info = { tag, id, classes: cls, text, selector }
  // 文件路径：Vue 项目的 CDN 模式下所有组件合并，默认 App.vue
  const appVue = Object.keys(previewFilesMap.value).find(p => /App\.vue$/i.test(p))
  info.filePath = appVue || 'src/App.vue'
  if (!pickedElements.value.find(p => p.selector === selector)) {
    pickedElements.value.push(info)
  }
}

/** 移除单个已选元素 */
function removePickedElement(index) {
  pickedElements.value.splice(index, 1)
}

/** 构建元素信息文本（用于 AI prompt） */
function buildPickedElementsInfo() {
  if (!pickedElements.value.length) return ''
  return pickedElements.value.map((el, i) =>
    `[元素${i + 1}] 文件: ${el.filePath || 'index.html'} | <${el.tag}${el.id ? ' id="' + el.id + '"' : ''}${el.classes ? ' class="' + el.classes + '"' : ''}> 文本: "${el.text}" (选择器: ${el.selector})`
  ).join('\n')
}

// ─── 初始化 ───

onMounted(async () => {
  window.addEventListener('message', handlePreviewMessage)
  await loadModels()
  loadKnowledgeDocs()
  const savedMode = localStorage.getItem('codegen-mode')
  if (savedMode && modes.find(m => m.key === savedMode)) {
    currentMode.value = savedMode
  }

  // 从「我的应用」跳转：加载关联对话
  const appId = route.query.appId
  if (appId) {
    try {
      const appRes = await getApplication(Number(appId))
      const app = appRes?.data
      if (app) {
        // 恢复对话模式和模型
        if (app.type && modes.find(m => m.key === app.type)) {
          currentMode.value = app.type
        }
        await loadConversations()
        // 查找关联对话并加载
        if (app.conversationId) {
          const conv = conversations.value.find(c => c.id === app.conversationId)
          if (conv) {
            await switchConversation(conv)
          } else {
            // 对话可能在不同 type 下列表，直接通过 ID 获取消息
            currentConv.value = { id: app.conversationId, title: app.name, type: app.type }
            try {
              const msgRes = await getMessages(app.conversationId)
              const msgs = msgRes?.data || []
              messages.value = msgs.map(m => parseAiMessage(m))
              const lastAi = [...messages.value].reverse().find(m => m.role === 'ai')
              if (lastAi?.files?.length) {
                if (app.type === 'VUE_PROJECT') {
                  buildPreviewFilesMap(lastAi.files)
                  showPreview.value = true
                  previewFileName.value = lastAi.files.find(f => f.path === 'package.json')?.path || 'Vue 项目'
                  await nextTick()
                  sandpackKey.value++
                } else if (app.type === 'MULTI_FILE') {
                  showPreview.value = true
                  previewFileName.value = '多文件项目'
                  autoDeployMultiFile(app.conversationId)
                } else if (lastAi.files[0]?.content) {
                  setPreview(lastAi.files[0].content, lastAi.files[0].path || 'index.html')
                }
              }
            } catch (e) { console.warn('加载应用对话失败', e) }
          }
        }
      }
    } catch (e) { console.warn('加载应用失败', e) }
  } else {
    await loadConversations()
  }
})

// ─── 模型管理 ───

async function loadModels() {
  try {
    const res = await listEnabledModels()
    availableModels.value = res?.data || []
    const saved = localStorage.getItem('codegen-modelId')
    selectedModelId.value = saved ? Number(saved) : null
  } catch (e) { /* ignore */ }
}

function persistModel() {
  localStorage.setItem('codegen-modelId', selectedModelId.value || '')
}

// ─── 模式切换 ───

function switchMode(mode) {
  currentMode.value = mode
  localStorage.setItem('codegen-mode', mode)
  newConversation()
}

// ─── 对话管理 ───

async function loadConversations() {
  try {
    const res = await listConversations({ type: currentMode.value })
    conversations.value = res?.data || []
  } catch (e) { /* ignore */ }
}

function newConversation() {
  currentConv.value = null
  messages.value = []
  streamText.value = ''
  previewContent.value = ''
  previewHtml.value = ''
  previewFilesMap.value = {}
  showPreview.value = false
  deployedUrl.value = ''
  inputText.value = ''
  pickerActive.value = false
  pickedElements.value = []
}

async function switchConversation(c) {
  if (generating.value) return
  currentConv.value = c
  messages.value = []
  previewContent.value = ''
  showPreview.value = false
  deployedUrl.value = ''
  try {
    const res = await getMessages(c.id)
    const msgs = res?.data || []
    messages.value = msgs.map(m => parseAiMessage(m))
    const lastAi = [...messages.value].reverse().find(m => m.role === 'ai')

    // 模式推断：优先从对话 type 字段，否则从当前模式
    const convType = c.type || currentMode.value

    if (lastAi?.files?.length) {
      // 重建预览
      if (convType === 'VUE_PROJECT') {
        // Vue 项目：Sandpack 即时预览 + 尝试 Nginx 部署
        buildPreviewFilesMap(lastAi.files)
        showPreview.value = true
        previewFileName.value = lastAi.files.find(f => f.path === 'package.json')?.path || 'Vue 项目'
        await nextTick()
        sandpackKey.value++
        autoDeployMultiFile(c.id)
      } else if (convType === 'MULTI_FILE') {
        // 多文件：自动部署到 Nginx
        showPreview.value = true
        previewFileName.value = '多文件项目'
        autoDeployMultiFile(c.id)
      } else {
        // 单文件：用 iframe 内联预览
        const mainFile = findMainFile(lastAi.files)
        setPreview(mainFile?.content || lastAi.code, mainFile?.path || 'index.html')
      }
    } else if (lastAi?.code) {
      setPreview(lastAi.code, 'index.html')
    }

    // 自动展开最后一个有文件的消息
    for (let i = messages.value.length - 1; i >= 0; i--) {
      if (messages.value[i].role === 'ai' && messages.value[i].files?.length) {
        expandedFiles[i] = true
        break
      }
    }
  } catch (e) {
    console.error('加载对话失败，详细错误:', e)
    ElMessage.error('加载对话失败: ' + (e.message || '未知错误'))
  }
}

async function deleteConversation(c) {
  try {
    await ElMessageBox.confirm('确定删除此对话？（已保存到应用的项目不受影响）', '提示', { type: 'warning' })
    await delConv(c.id)
    conversations.value = conversations.value.filter(x => x.id !== c.id)
    if (currentConv.value?.id === c.id) newConversation()
  } catch (e) { /* cancel */ }
}

// ─── 核心：发送生成请求 ───

async function handleGenerate() {
  if (!inputText.value.trim() || generating.value) return
  const prompt = inputText.value.trim()
  inputText.value = ''

  // 如果有选取的元素，走「文件修改」流程
  const elementsInfo = buildPickedElementsInfo()
  const isModify = !!elementsInfo

  // 对话中显示简洁消息（不重复元素详情，AI 会单独收到）
  const displayMsg = isModify
    ? `🎯 ${prompt}\n(已选中 ${pickedElements.value.length} 个元素，精准修改)`
    : prompt
  messages.value.push({ role: 'user', content: displayMsg })
  streamText.value = ''
  generating.value = true
  previewError.value = null

  abortController = new AbortController()

  // 选取了元素 → 调用文件修改 API
  if (isModify) {
    const currentFiles = collectCurrentFiles()
    try {
      await modifyCodeStream({
        files: currentFiles,
        elementInfo: elementsInfo,
        modifyPrompt: prompt,
        conversationId: currentConv.value?.id || null,
        type: currentMode.value
      }, {
        onToken: (t) => { streamText.value += t.replace(/\\n/g, '\n').replace(/\\t/g, '\t') },
        onDone: (raw) => { handleModifyDone(raw) },
        onError: (err) => {
          ElMessage.error('修改失败: ' + err.message)
          generating.value = false
        },
        signal: abortController.signal
      })
    } catch (e) {
      if (e.name !== 'AbortError') {
        ElMessage.error('请求异常: ' + e.message)
      }
      generating.value = false
    }
    return
  }

  // 正常生成
  try {
    await unifiedGenerateStream({
      prompt,
      type: currentMode.value,
      conversationId: currentConv.value?.id || null,
      modelId: selectedModelId.value || null,
      knowledgeDocIds: selectedKbDocs.value.length > 0 ? [...selectedKbDocs.value] : null,
      autoSearchKnowledge: autoSearchKb.value
    }, {
      onToken: (t) => { streamText.value += t.replace(/\\n/g, '\n').replace(/\\t/g, '\t') },
      onToolCall: (data) => {
        try {
          const tc = typeof data === 'string' ? JSON.parse(data) : data
          if (tc.tool === 'writeFile') {
            ElMessage.success(`文件已创建: ${tc.path}`)
          }
        } catch (e) { /* ignore */ }
      },
      onDone: (raw) => { handleStreamDone(raw) },
      onError: (err) => {
        ElMessage.error('生成失败: ' + err.message)
        generating.value = false
      },
      signal: abortController.signal
    })
  } catch (e) {
    if (e.name !== 'AbortError') {
      ElMessage.error('请求异常: ' + e.message)
    }
    generating.value = false
  }
}

/** 收集当前预览中的文件列表 */
function collectCurrentFiles() {
  // 1. 从最近一条 AI 消息中提取文件
  const lastAi = [...messages.value].reverse().find(m => m.role === 'ai')
  if (lastAi?.files?.length) {
    return lastAi.files.map(f => ({ path: f.path, language: f.language || detectLangFromPath(f.path), content: f.content }))
  }
  // 2. 从 previewFilesMap 重建（Vue 项目 / Sandpack）
  if (Object.keys(previewFilesMap.value).length) {
    return Object.entries(previewFilesMap.value).map(([path, obj]) => ({
      path, language: detectLangFromPath(path), content: obj.code || ''
    }))
  }
  // 3. 回退：从 previewContent/previewHtml 重建（单文件模式）
  const content = previewContent.value
  const name = previewFileName.value || 'index.html'
  if (content && content.length > 10) {
    return [{ path: name, language: detectLangFromPath(name), content: content }]
  }
  // 4. 最终回退：最后一个 AI 消息的 code 字段
  if (lastAi?.code && lastAi.code.length > 10) {
    return [{ path: 'index.html', language: 'html', content: lastAi.code }]
  }
  return []
}

/** 处理修改完成事件 */
function handleModifyDone(raw) {
  generating.value = false
  try {
    const data = typeof raw === 'string' ? JSON.parse(raw) : raw
    const files = data.files || []

    const aiMsg = {
      role: 'ai',
      text: data.text || '修改完成',
      files: files,
      code: data.code || '',
      language: data.language || 'html'
    }
    messages.value.push(aiMsg)

    // 更新预览
    if (files.length) {
      if (currentMode.value === 'VUE_PROJECT') {
        buildPreviewFilesMap(files)
        sandpackKey.value++
        showPreview.value = true
        previewFileName.value = files.find(f => f.path === 'package.json')?.path || 'Vue 项目'
        previewVersion.value++
        if (currentConv.value?.id) {
          autoDeployMultiFile(currentConv.value.id).then(() => { previewVersion.value++ })
        }
      } else if (currentMode.value === 'MULTI_FILE') {
        if (currentConv.value?.id) {
          autoDeployMultiFile(currentConv.value.id).then(() => { previewVersion.value++ })
        }
      } else {
        const mainFile = findMainFile(files)
        setPreview(mainFile?.content || '', mainFile?.path || 'index.html')
        previewVersion.value++
      }
    }

    removePickerFromIframe()
    pickedElements.value = []
    pickerActive.value = false
    previewVersion.value++
    ElMessage.success('修改完成')
    nextTick(() => scrollToBottom())
  } catch (e) {
    console.error('解析修改完成事件失败:', e)
  }
}

function detectLangFromPath(path) {
  if (!path) return 'text'
  if (path.endsWith('.vue')) return 'vue'
  if (path.endsWith('.html')) return 'html'
  if (path.endsWith('.css')) return 'css'
  if (path.endsWith('.js')) return 'javascript'
  return 'text'
}

function handleStreamDone(raw) {
  generating.value = false
  try {
    const data = typeof raw === 'string' ? JSON.parse(raw) : raw
    const convId = data.conversationId

    // 合并代码：从 files 重建，使用语言对应的注释标记
    const files = data.files || []
    const code = files.length
      ? files.map(f => commentForFile(f.path) + '\n' + (f.content || '')).join('\n\n')
      : (data.code || '')

    const aiMsg = {
      role: 'ai',
      text: data.text || '',
      code: code,
      files: files,
      language: data.language || 'html',
    }
    messages.value.push(aiMsg)

    if (convId && !currentConv.value) {
      loadConversations().then(() => {
        currentConv.value = conversations.value.find(c => c.id === convId)
      })
    }

    // 预览：单文件 srcdoc，Vue/多文件走 Nginx 部署
    if (currentMode.value === 'VUE_PROJECT') {
      // Vue 项目：先显示 Sandpack 作为即时预览，同时异步部署 Nginx
      if (files.length) {
        buildPreviewFilesMap(files)
        sandpackKey.value++
        showPreview.value = true
        previewFileName.value = files.find(f => f.path === 'package.json')?.path || 'Vue 项目'
      }
      if (convId) autoDeployMultiFile(convId)
    } else if (currentMode.value === 'MULTI_FILE') {
      if (convId) autoDeployMultiFile(convId)
    } else if (files.length) {
      const mainFile = findMainFile(files)
      setPreview(mainFile?.content || code, mainFile?.path || 'index.html')
    } else if (code && !isFilePath(code)) {
      setPreview(code, 'index.html')
    }

    nextTick(() => scrollToBottom())
  } catch (e) {
    console.error('解析完成事件失败:', e)
  }
}

/** 多文件模式：生成完成后自动部署到 Nginx */
async function autoDeployMultiFile(convId) {
  try {
    const res = await deployConversation(convId)
    if (res?.code === 200 && res?.data) {
      deployedUrl.value = res.data.url
      previewContent.value = res.data.url
      previewFileName.value = '多文件项目'
      showPreview.value = true
      previewError.value = null
    } else {
      throw new Error(res?.message || '部署失败')
    }
  } catch (e) {
    console.warn('自动部署失败（Nginx 可能未启动）:', e.message)
    // fallback: 显示文件列表提示
    previewContent.value = 'deploy_failed'
    previewFileName.value = '多文件项目'
    showPreview.value = true
    previewError.value = '⚠️ Nginx 部署未就绪，多文件项目无法预览。请启动 Nginx 后点击「🚀 部署预览」按钮。'
  }
}

/** 判断内容是否像文件路径而非代码 */
function isFilePath(str) {
  if (!str) return false
  const s = str.trim()
  // Windows 绝对路径: C:\... 或 F:\...
  if (/^[A-Z]:[\\/]/.test(s)) return true
  // 太短的内容（如纯路径）不是代码
  if (s.length < 20 && /[\\/]/.test(s) && !s.includes('<') && !s.includes('{')) return true
  return false
}

function stopGenerating() {
  if (abortController) {
    abortController.abort()
    abortController = null
    if (streamText.value) {
      messages.value.push({ role: 'ai', text: streamText.value, code: streamText.value, files: [], interrupted: true })
    }
    streamText.value = ''
    generating.value = false
  }
}

async function handleContinue(msgIdx) {
  const msg = messages.value[msgIdx]
  if (!msg?.interrupted) return
  delete msg.interrupted
  inputText.value = '请继续'
  await handleGenerate()
}

// ─── 保存到我的应用 ───

async function handleSaveAsApp() {
  if (!messages.value.length) return
  const lastAi = [...messages.value].reverse().find(m => m.role === 'ai')
  if (!lastAi?.code && !lastAi?.files?.length) { ElMessage.warning('没有可保存的代码'); return }
  try {
    const { value: name } = await ElMessageBox.prompt('应用名称', '保存到我的应用', {
      inputValue: currentConv.value?.title || '未命名应用',
      confirmButtonText: '保存',
    })
    if (!name) return
    await saveApplication({
      name: name.trim(),
      type: currentMode.value,
      language: lastAi.language || 'html',
      description: lastAi.text?.substring(0, 200) || '',
      conversationId: currentConv.value?.id || null,
    })
    ElMessage.success('已保存到我的应用')
  } catch (e) { /* cancel */ }
}

// ─── 部署到 Nginx ───

async function handleDeploy() {
  const convId = currentConv.value?.id
  if (!convId) { ElMessage.warning('请先生成代码后再部署'); return }
  isDeploying.value = true
  try {
    const res = await deployConversation(convId)
    if (res?.code === 200 && res?.data) {
      deployedUrl.value = res.data.url
      ElMessage.success('部署成功！可通过 Nginx 预览多文件项目')
    } else {
      ElMessage.error(res?.message || '部署失败')
    }
  } catch (e) {
    ElMessage.error('部署请求失败: ' + e.message)
  } finally {
    isDeploying.value = false
  }
}

function copyDeployUrl() {
  if (!deployedUrl.value) return
  navigator.clipboard.writeText(deployedUrl.value).then(
    () => ElMessage.success('URL 已复制'),
    () => ElMessage.info('请手动复制: ' + deployedUrl.value)
  )
}

// ─── 文件树（消息内折叠展示） ───

function toggleFiles(msgIdx) {
  expandedFiles[msgIdx] = !expandedFiles[msgIdx]
}

/** 将扁平文件列表按目录分组，返回扁平分组列表（不嵌套） */
function asFileTree(files) {
  if (!files?.length) return []
  // 按路径排序
  const sorted = [...files].sort((a, b) => (a.path || '').localeCompare(b.path || ''))
  const result = []
  let lastDir = ''
  for (const f of sorted) {
    const p = f.path || ''
    const lastSlash = p.lastIndexOf('/')
    const dir = lastSlash >= 0 ? p.substring(0, lastSlash) : ''
    const name = lastSlash >= 0 ? p.substring(lastSlash + 1) : p
    // 遇到新目录时插入目录标题行
    if (dir !== lastDir) {
      lastDir = dir
      if (dir) {
        result.push({ isDir: true, name: '📁 ' + dir + '/', path: dir, depth: 0 })
      }
    }
    result.push({
      isDir: false,
      name: '📄 ' + name,
      path: p,
      depth: dir ? 1 : 0,
      content: f.content,
      size: (f.content || '').length,
    })
  }
  return result
}

// ─── 预览 ───

function findMainFile(files) {
  if (!files?.length) return null
  const priority = ['index.html', 'App.vue', 'src/App.vue']
  for (const p of priority) {
    const found = files.find(f => f.path === p || f.path.endsWith('/' + p))
    if (found) return found
  }
  return files.find(f => f.path.endsWith('.html')) || files.find(f => f.path.endsWith('.vue')) || files[0]
}

function setPreview(code, fileName) {
  previewContent.value = code || ''
  previewFileName.value = fileName || 'preview'
  if (currentMode.value !== 'VUE_PROJECT') {
    previewHtml.value = buildPreviewHtml(code, fileName)
  }
  showPreview.value = true
}

function previewFile(file) { setPreview(file.content, file.path) }

function buildPreviewFilesMap(files) {
  const map = {}
  for (const f of files) { if (f.content) map[f.path] = { code: f.content } }
  previewFilesMap.value = map
}

function buildPreviewHtml(code, fileName) {
  if (!code) return ''
  if (code.trim().startsWith('<!DOCTYPE') || code.trim().startsWith('<html')) return code
  return `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1.0">
<style>body{margin:0;font-family:system-ui,sans-serif;}</style></head><body>${code}</body></html>`
}

// ─── 拖拽 ───

function startResize(e) {
  e.preventDefault()
  const handle = e.currentTarget
  handle.classList.add('resizing')
  isResizing.value = true
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

function onPreviewLoad() {
  if (pickerActive.value) {
    // iframe 内容变化后重新注入选取脚本
    nextTick(() => injectPickerScript())
  }
}

// ─── 导出 ───

async function handleExport() {
  if (!currentConv.value) return
  try {
    const res = await exportConversation(currentConv.value.id)
    const blob = new Blob([res], { type: 'text/markdown' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = `${currentConv.value.title || '对话'}.md`
    a.click(); URL.revokeObjectURL(url)
  } catch (e) { ElMessage.error('导出失败') }
}

// ─── 工具函数 ───

function parseAiMessage(m) {
  try {
    const msg = { role: m.role?.toLowerCase(), content: m.content }
    if (m.role === 'AI' || m.role === 'ai') {
      const raw = m.content || ''
      msg.files = extractFilesFromContent(raw)
      // 文本：优先从 JSON 取 description，回退到去掉代码块和 JSON 的纯文本
      if (msg.files.length) {
        const json = extractJsonObject(raw)
        if (json?.description) {
          msg.text = json.description
        } else {
          const braceIdx = raw.indexOf('{')
          if (braceIdx > 0) {
            msg.text = raw.substring(0, braceIdx).replace(/```[\s\S]*?```/g, '').trim()
          } else {
            msg.text = raw.replace(/```[\s\S]*?```/g, '').replace(/\[FILE\][^\n]*/gi, '').trim()
          }
        }
      } else {
        msg.text = raw.replace(/```[\s\S]*?```/g, '').replace(/\[FILE\][^\n]*/gi, '').trim()
      }
      // code：从 files 重建，使用语言对应的注释标记
      msg.code = msg.files.length
        ? msg.files.map(f => commentForFile(f.path) + '\n' + (f.content || '')).join('\n\n')
        : (extractCode(raw) || raw)
    }
    return msg
  } catch (e) {
    console.error('parseAiMessage 解析失败:', e, m)
    return { role: m.role?.toLowerCase(), content: m.content, text: m.content || '', files: [], code: '' }
  }
}

/** 根据文件扩展名生成对应的注释标记 */
function commentForFile(path) {
  if (!path) return ''
  if (path.endsWith('.html') || path.endsWith('.htm')) return '<!-- file: ' + path + ' -->'
  if (path.endsWith('.css')) return '/* file: ' + path + ' */'
  if (path.endsWith('.js')) return '// file: ' + path
  if (path.endsWith('.vue')) return '<!-- file: ' + path + ' -->'
  if (path.endsWith('.json')) return '// file: ' + path
  return '# file: ' + path
}

/** 从 AI 消息内容中提取文件（支持 JSON 格式、[FILE] 标记、代码块） */
function extractFilesFromContent(raw) {
  if (!raw) return []
  const files = []
  const seen = new Set()

  // 1. 尝试 JSON 格式（优先）: {"htmlCode":"...","cssCode":"..."} 或 {"files":[{...}]}
  try {
    const json = extractJsonObject(raw)
    if (json) {
      // 多文件 JSON: {htmlCode, cssCode, jsCode}
      if (json.htmlCode) addFile(files, seen, 'index.html', 'html', json.htmlCode)
      if (json.cssCode) addFile(files, seen, 'style.css', 'css', json.cssCode)
      if (json.jsCode) addFile(files, seen, 'script.js', 'javascript', json.jsCode)
      // Vue 项目 JSON: {files: [{path, content}]}
      if (Array.isArray(json.files)) {
        for (const fe of json.files) {
          if (fe.path && fe.content) addFile(files, seen, fe.path, detectLang(fe.path), fe.content)
        }
      }
      if (files.length) return files
    }
  } catch (e) { /* fall through */ }

  // 2. [FILE] 标记格式
  const re = /\[FILE\]\s*([^\r\n]+)\s*[\r\n]+```(\w*)\s*[\r\n]*([\s\S]*?)```/gi
  let m
  while ((m = re.exec(raw)) !== null) {
    const path = m[1].trim()
    const lang = m[2] || detectLang(path)
    const code = m[3].trim()
    if (code && !seen.has(path)) addFile(files, seen, path, lang, code)
  }
  if (files.length) return files

  // 2.5 注释标记格式: <!-- file: path --> 或 /* file: path */ 或 // file: path
  const commentRe = /(?:<!--\s*file:\s*(.+?)\s*-->|\/\*\s*file:\s*(.+?)\s*\*\/|\/\/\s*file:\s*(.+)|#\s*file:\s*(.+))\n([\s\S]*?)(?=\n(?:<!--\s*file:|\/\*\s*file:|\/\/\s*file:|#\s*file:)|$)/g
  while ((m = commentRe.exec(raw)) !== null) {
    const path = (m[1] || m[2] || m[3] || m[4] || '').trim()
    const code = (m[5] || '').trim()
    if (path && code && !seen.has(path)) addFile(files, seen, path, detectLang(path), code)
  }
  if (files.length) return files

  // 3. 通用代码块兜底
  const blocks = [...raw.matchAll(/```(\w*)\s*\n?([\s\S]*?)```/g)]
  let idx = 0
  for (const b of blocks) {
    const code = b[2].trim()
    if (!code) continue
    idx++
    const lang = b[1] || 'text'
    const name = lang === 'html' ? 'index.html' : lang === 'css' ? 'style.css' : lang === 'javascript' ? 'script.js' : `file${idx}.txt`
    addFile(files, seen, name, lang, code)
  }
  return files
}

function addFile(files, seen, path, lang, content) {
  if (seen.has(path)) return
  seen.add(path)
  files.push({ path, language: lang, content })
}

/** 从原始文本中提取 JSON 对象（括号深度追踪） */
function extractJsonObject(raw) {
  const start = raw.indexOf('{')
  if (start < 0) return null
  let depth = 0, inStr = false, esc = false
  for (let i = start; i < raw.length; i++) {
    const c = raw[i]
    if (inStr) { if (esc) esc = false; else if (c === '\\') esc = true; else if (c === '"') inStr = false }
    else { if (c === '"') inStr = true; else if (c === '{') depth++; else if (c === '}') { depth--; if (depth === 0) { try { return JSON.parse(raw.substring(start, i + 1)) } catch (e) { return null } } } }
  }
  return null
}

function detectLang(path) {
  if (!path) return 'text'
  if (path.endsWith('.vue')) return 'vue'
  if (path.endsWith('.html')) return 'html'
  if (path.endsWith('.css')) return 'css'
  if (path.endsWith('.js')) return 'javascript'
  return 'text'
}

function extractCode(raw) {
  if (!raw) return ''
  const matches = [...raw.matchAll(/```(\w*)\s*\n?([\s\S]*?)```/g)]
  return matches.map(m => m[2].trim()).filter(Boolean).join('\n\n') || raw
}

function renderMarkdown(text) {
  if (!text) return ''
  return md.render(text)
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function scrollToBottom() {
  const el = document.querySelector('.ws-chat .messages')
  if (el) el.scrollTop = el.scrollHeight
}

watch(() => currentMode.value, () => { newConversation(); loadConversations() })
watch(() => streamText.value, () => { nextTick(() => scrollToBottom()) })
watch(showPreview, (v) => { if (!v) { pickerActive.value = false; pickedElements.value = [] } })
</script>

<style scoped>
.workspace { display:flex; flex-direction:column; height:100vh; background:var(--bg-primary,#0d1117); color:var(--text-primary,#c9d1d9); }

/* ─── Header ─── */
.ws-header { display:flex; align-items:center; padding:0 16px; height:48px; border-bottom:1px solid #21262d; background:var(--bg-card,#141821); }
.ws-header .logo { font-size:18px; font-weight:bold; color:var(--accent,#7c8aff); margin:0; }
.ws-header .beta { font-size:11px; color:var(--text-dim,#6b7280); margin-left:4px; }
.mode-tabs { display:flex; gap:4px; margin-left:24px; }
.mode-btn { padding:4px 14px; border:1px solid #30363d; border-radius:6px; background:transparent; color:var(--text-secondary,#8b949e); cursor:pointer; font-size:13px; transition:all .2s; }
.mode-btn:hover { border-color:var(--accent,#7c8aff); color:var(--text-primary,#c9d1d9); }
.mode-btn.active { background:var(--accent,#7c8aff); border-color:var(--accent,#7c8aff); color:#fff; }
.header-right { margin-left:auto; display:flex; gap:8px; align-items:center; }

/* ─── Body ─── */
.ws-body { display:flex; flex:1; overflow:hidden; }

/* ─── Sidebar ─── */
.ws-sidebar { width:220px; background:var(--bg-card,#141821); border-right:1px solid #21262d; display:flex; flex-direction:column; transition:width .2s; }
.ws-sidebar.collapsed { width:40px; }
.ws-sidebar.collapsed .sidebar-actions,
.ws-sidebar.collapsed .conv-list { display:none; }
.sidebar-actions { padding:12px; }
.conv-list { flex:1; overflow-y:auto; padding:0 8px; }
.conv-item { padding:8px 12px; border-radius:6px; cursor:pointer; margin-bottom:2px; display:flex; align-items:center; gap:8px; transition:background .15s; }
.conv-item:hover { background:#1c2333; }
.conv-item.active { background:rgba(124,138,255,.15); }
.conv-title { flex:1; font-size:13px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.conv-time { font-size:11px; color:var(--text-dim,#6b7280); }
.conv-del { opacity:0; transition:opacity .15s; font-size:12px; }
.conv-item:hover .conv-del { opacity:1; }
.sidebar-footer { padding:8px; border-top:1px solid #21262d; text-align:center; }

/* ─── Chat ─── */
.ws-chat { flex:1; display:flex; flex-direction:column; min-width:0; min-height:0; }
.welcome { flex:1; display:flex; flex-direction:column; align-items:center; justify-content:center; padding:48px; text-align:center; }
.welcome h1 { font-size:28px; margin-bottom:8px; }
.welcome .desc { color:var(--text-secondary,#8b949e); margin-bottom:32px; }
.quick-prompts { display:flex; gap:12px; flex-wrap:wrap; justify-content:center; }
.quick-item { padding:8px 18px; border:1px solid #30363d; border-radius:8px; cursor:pointer; font-size:13px; transition:all .2s; }
.quick-item:hover { border-color:var(--accent,#7c8aff); color:var(--accent,#7c8aff); }
.messages { flex:1; overflow-y:auto; padding:16px 24px; }

.msg { display:flex; gap:12px; margin-bottom:20px; }
.msg-user { flex-direction:row-reverse; }
.msg-avatar { width:32px; height:32px; border-radius:50%; background:#1c2333; display:flex; align-items:center; justify-content:center; font-size:14px; flex-shrink:0; }
.msg-user .msg-avatar { background:var(--accent,#7c8aff); }
.msg-body { max-width:75%; }
.msg-user .msg-body { text-align:right; }
.msg-text { padding:10px 14px; border-radius:10px; background:#1c2333; line-height:1.6; }
.msg-user .msg-text { background:var(--accent,#7c8aff); color:#fff; }
.msg-text :deep(pre) { background:#0d1117; padding:12px; border-radius:6px; overflow-x:auto; }
.msg-text :deep(code) { font-family:'Fira Code',monospace; font-size:13px; }
.msg-files { margin-top:8px; }
.files-toggle { padding:6px 10px; background:#1c2333; border:1px solid #30363d; border-radius:6px; cursor:pointer; font-size:12px; user-select:none; transition:all .15s; display:inline-flex; align-items:center; gap:6px; }
.files-toggle:hover { border-color:var(--accent,#7c8aff); }
.files-tree { margin-top:6px; background:#161b22; border:1px solid #21262d; border-radius:6px; padding:6px 0; }
.tree-item { display:flex; align-items:center; gap:6px; padding:3px 8px; font-size:12px; color:var(--text-secondary,#8b949e); cursor:default; transition:background .1s; }
.tree-item.is-dir { color:var(--text-dim,#6b7280); font-size:11px; padding-top:6px; cursor:default; }
.tree-item.is-file { cursor:pointer; }
.tree-item.is-file:hover { background:rgba(124,138,255,.08); color:var(--text-primary,#c9d1d9); }
.tree-arrow { font-size:11px; width:16px; text-align:center; flex-shrink:0; }
.tree-name { flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.tree-size { font-size:10px; color:var(--text-dim,#6b7280); }
.streaming-text { color:var(--text-secondary,#8b949e); font-size:13px; line-height:1.6; white-space:pre-wrap; word-break:break-word; font-family:'Fira Code',monospace; }
.cursor-blink { animation:blink 1s infinite; color:var(--accent,#7c8aff); }
@keyframes blink { 0%,50% { opacity:1; } 51%,100% { opacity:0; } }
.streaming-wait { color:var(--text-dim,#6b7280); font-size:13px; font-style:italic; }
.wait-pulse { animation:waitPulse 1.5s infinite; }
@keyframes waitPulse { 0%,100% { opacity:0.2; } 50% { opacity:1; } }

.input-bar { padding:12px 24px; border-top:1px solid #21262d; display:flex; gap:10px; background:var(--bg-card,#141821); }

/* ─── Preview ─── */
.ws-preview { flex-shrink:0; display:flex; flex-direction:column; background:#1a1e2a; overflow:hidden; }

/* ─── Resize Handle ─── */
.resize-handle { width:10px; flex-shrink:0; cursor:col-resize; background:transparent; transition:background .2s; z-index:10; position:relative; display:flex; align-items:center; justify-content:center; user-select:none; }
.resize-handle:hover { background:rgba(124,138,255,.08); }
.resize-handle:active, .resize-handle.resizing { background:rgba(124,138,255,.15); }
.resize-handle::after { content:''; position:absolute; left:50%; transform:translateX(-50%); width:2px; height:40px; border-radius:1px; background:rgba(255,255,255,.12); transition:background .2s,height .2s; }
.resize-handle:hover::after { background:rgba(124,138,255,.5); height:56px; }
.resize-handle:active::after, .resize-handle.resizing::after { background:var(--accent,#7c8aff); height:80px; box-shadow:0 0 6px rgba(124,138,255,.4); }
.resize-grip { display:flex; gap:2px; opacity:0; transition:opacity .2s; position:relative; z-index:1; }
.resize-handle:hover .resize-grip, .resize-handle.resizing .resize-grip { opacity:1; }
.resize-grip span { width:2px; height:2px; border-radius:50%; background:var(--accent,#7c8aff); }

.preview-header { padding:8px 12px; border-bottom:1px solid #21262d; font-size:12px; display:flex; align-items:center; justify-content:space-between; flex-shrink:0; }
.preview-body { flex:1; overflow:hidden; }
.preview-body.no-pointer { pointer-events:none; }
.preview-iframe { width:100%; height:100%; border:none; }
.preview-error { padding:8px; color:#f85149; font-size:12px; flex-shrink:0; }
.deploy-fallback { flex:1; display:flex; flex-direction:column; align-items:center; justify-content:center; padding:32px; text-align:center; color:var(--text-dim,#6b7280); gap:12px; }
.deploy-fallback .fallback-icon { font-size:48px; }
.deploy-fallback .fallback-msg { font-size:13px; color:#f0883e; line-height:1.6; max-width:280px; }
.preview-label { display:flex; align-items:center; gap:6px; }
.deploy-badge { font-size:14px; animation:pulse 2s infinite; }
@keyframes pulse { 0%,100% { opacity:1; } 50% { opacity:.5; } }
.deploy-url-bar { display:flex; align-items:center; justify-content:space-between; padding:4px 8px; background:#0d1117; border-top:1px solid #21262d; font-size:11px; color:var(--text-dim,#6b7280); flex-shrink:0; }
.deploy-url-bar .deploy-key { font-family:'Fira Code',monospace; color:var(--accent,#7c8aff); overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }

/* ─── 知识库 Popover ─── */
.kb-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 16px; height: 16px; border-radius: 8px;
  background: var(--warning,#f59e0b); color: #000;
  font-size: 10px; font-weight: 700; margin-left: 4px; padding: 0 4px;
}
.kb-popover { max-height: 360px; display: flex; flex-direction: column; }
.kb-pop-header { display: flex; align-items: center; gap: 12px; }
.kb-hint { font-size: 11px; color: var(--text-dim,#6b7280); }
.kb-pop-list { flex: 1; overflow-y: auto; min-height: 60px; max-height: 280px; }
.kb-empty { text-align: center; padding: 20px; color: var(--text-dim,#6b7280); font-size: 13px; }
.kb-doc-item { display: flex; align-items: center; justify-content: space-between; padding: 4px 0; }
.kb-doc-title { font-size: 13px; color: var(--text-primary); max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: inline-block; }
.kb-doc-type { font-size: 10px; color: var(--text-dim,#6b7280); text-transform: uppercase; background: var(--bg-secondary); padding: 1px 6px; border-radius: 4px; }

/* ─── 元素选取气泡 ─── */
.picked-elements-bar {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding: 6px 10px; border-bottom: 1px solid #21262d;
  background: rgba(124,138,255,.06); flex-shrink: 0;
}
.picked-label { font-size: 11px; color: var(--text-dim,#6b7280); white-space: nowrap; }
.picked-tag {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 4px 2px 8px;
  background: rgba(124,138,255,.15); border: 1px solid rgba(124,138,255,.3);
  border-radius: 12px; font-size: 11px; color: var(--accent,#7c8aff);
  max-width: 200px;
}
.picked-tag code {
  font-family: 'Fira Code',monospace; font-size: 10px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  background: transparent; color: inherit;
}
.picked-tag-close {
  width: 16px; height: 16px; border-radius: 50%; border: none;
  background: rgba(255,255,255,.1); color: var(--text-dim,#6b7280);
  cursor: pointer; font-size: 10px; line-height: 16px; text-align: center; padding: 0;
  flex-shrink: 0; transition: all .15s;
}
.picked-tag-close:hover { background: var(--danger,#f85149); color: #fff; }
</style>
