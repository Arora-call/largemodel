<template>
  <div class="ds-app">
    <!-- ====== 左侧边栏（可折叠） ====== -->
    <aside class="ds-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-inner">
        <div class="sidebar-top">
          <button class="btn-new-chat" @click="startNewChat">
            <span>+</span> 新建对话
          </button>
        </div>

        <div class="chat-history">
          <div class="history-label">对话历史</div>
          <div v-if="conversations.length === 0" class="history-empty">暂无对话记录</div>
          <div
            v-for="conv in conversations"
            :key="conv.id"
            class="history-item"
            :class="{ active: conv.id === activeConvId }"
            @click="switchConv(conv.id)"
          >
            <span class="history-title">{{ conv.title || '新对话' }}</span>
            <span class="history-time">{{ conv.messages.length }} 条消息</span>
          </div>
        </div>

        <div class="sidebar-bottom">
          <button class="btn-icon" title="清空记录" @click="clearHistory"><span>&#8635;</span></button>
        </div>
      </div>
    </aside>

    <!-- 折叠按钮：独立于侧边栏，始终可见 -->
    <button class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? '展开侧栏' : '收起侧栏'">
      {{ sidebarCollapsed ? '▶' : '◀' }}
    </button>

    <!-- ====== 主内容区（自动填充剩余宽度） ====== -->
    <main class="ds-main">
      <!-- 顶栏 -->
      <header class="ds-header">
        <div class="header-left">
          <span class="logo-dot"></span>
          <span class="logo-text">CodeForge</span>
          <span class="logo-beta">Beta</span>
        </div>
        <div class="header-center">
          <div class="model-badge">
            <span class="badge-dot"></span>
            {{ modelName }}
          </div>
        </div>
        <div class="header-right">
          <button class="btn-tool" title="复制全部" @click="copyCode(currentConv.codeOutput)"><span>&#128203;</span></button>
          <button class="btn-tool" title="导出" @click="saveAsApplication"><span>&#128190;</span></button>
        </div>
      </header>

      <!-- 对话内容 -->
      <div class="ds-chat" ref="chatArea">
        <div v-if="currentConv.messages.length === 0" class="chat-welcome">
          <div class="welcome-icon">
            <span class="welcome-glow"></span>
          </div>
          <h2>有什么可以帮忙？</h2>
          <p>描述你想要生成的代码应用，我会为你实时生成</p>
          <div class="quick-prompts">
            <button
              v-for="p in examplePrompts"
              :key="p"
              class="quick-prompt"
              @click="inputText = p"
            >{{ p }}</button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-for="(msg, idx) in currentConv.messages" :key="idx" :data-msg-idx="idx" :class="['msg-row', msg.role === 'USER' ? 'msg-user' : 'msg-ai']">
          <div class="msg-avatar">
            <span v-if="msg.role === 'USER'" class="avatar-user">U</span>
            <span v-else class="avatar-ai">AI</span>
          </div>
          <div class="msg-body">
            <!-- 用户消息 -->
            <div v-if="msg.role === 'USER'" class="bubble-user">{{ msg.content }}</div>
            <!-- AI 文字说明（Markdown 渲染） -->
            <div v-if="msg.role === 'AI' && msg.text" class="ai-explain" v-html="renderMd(msg.text)"></div>
            <!-- AI 代码块 + 预览区（左右分栏） -->
            <div v-if="msg.role === 'AI' && msg.code" class="code-row" :class="{ 'has-preview': previewing[idx] && !isBackendCode }">
              <div class="code-section">
                <div class="code-header">
                  <span class="code-dot"></span>
                  <span class="code-lang-badge">{{ msg.language || 'code' }}</span>
                  <div class="code-actions">
                    <button v-if="!isBackendCode" @click="togglePreview(idx)" :title="previewing[idx] ? '收起预览' : '预览'">{{ previewing[idx] ? '✕ 收起预览' : '👁 预览' }}</button>
                    <button @click="copyCode(msg.code)" title="复制">&#128203;</button>
                    <button @click="toggleFold(idx)" title="折叠">{{ folded[idx] ? '&#9654;' : '&#9660;' }}</button>
                  </div>
                </div>
                <CodeViewer
                  v-if="!folded[idx]"
                  :code="msg.code"
                  :language="msg.language"
                  height="400px"
                  max-height="400px"
                />
              </div>
              <div v-if="previewing[idx] && !isBackendCode" class="preview-panel">
                <div class="preview-header">
                  <div class="preview-tabs">
                    <button :class="{active:editSubMode!=='edit'}" @click="editSubMode='preview'">预览</button>
                    <button :class="{active:editSubMode==='edit'}" @click="editSubMode='edit'">编辑</button>
                  </div>
                  <button @click="previewing[idx] = false; editSubMode='preview'" title="关闭">&times;</button>
                </div>
                <div v-if="editSubMode==='edit'" class="edit-info-bar">
                  <span v-if="selectedElement">已选: <b>{{selectedElement.tag}}</b> {{selectedElement.text?.substring(0,30)}}</span>
                  <span v-else>点击页面元素开始编辑</span>
                  <button v-if="selectedElement" class="btn-clear" @click="selectedElement=null;modifyPrompt=''">取消</button>
                </div>
                <iframe :srcdoc="buildPreviewHtml(msg.code, editSubMode==='edit')" sandbox="allow-scripts allow-same-origin" class="preview-iframe"></iframe>
                <div v-if="editSubMode==='edit' && selectedElement" class="edit-modify-bar">
                  <input v-model="modifyPrompt" placeholder="描述修改要求..." @keydown.enter="handleModify" :disabled="modifying" />
                  <button @click="handleModify" :disabled="modifying || !modifyPrompt.trim()">修改</button>
                </div>
              </div>
            </div>
            <!-- 中断续写 -->
            <div v-if="msg.role === 'AI' && msg.interrupted" class="continue-row">
              <button class="btn-continue" @click="handleContinue(idx)">继续生成 →</button>
            </div>
          </div>
        </div>

        <!-- 生成中 -->
        <div v-if="generating" class="msg-row msg-ai">
          <div class="msg-avatar"><span class="avatar-ai">AI</span></div>
          <div class="msg-body">
            <!-- 实时拆分文本说明 + 代码块 -->
            <div v-if="streamExplain" class="ai-explain">{{ streamExplain }}</div>
            <div v-if="streamCode" class="code-section">
              <div class="code-header">
                <span class="code-dot"></span>
                <span class="code-lang-badge">{{ streamLang || 'code' }}</span>
              </div>
              <CodeViewer
                v-if="streamCode"
                :code="streamCode"
                :language="streamLang"
                height="360px"
                max-height="360px"
              />
            </div>
            <div v-if="!streamCode && streamText" class="streaming-text">{{ streamText }}<span class="cursor-blink">|</span></div>
            <div v-if="!streamText" class="streaming-text">思考中...<span class="cursor-blink">|</span></div>
          </div>
        </div>

        <!-- 编辑中 -->
        <div v-if="modifying" class="msg-row msg-ai">
          <div class="msg-avatar"><span class="avatar-ai">AI</span></div>
          <div class="msg-body">
            <div class="streaming-text">修改中...{{ streamText }}<span class="cursor-blink">|</span></div>
          </div>
        </div>
      </div>

      <!-- 右侧快速导航 -->
      <div v-if="navItems.length > 1" class="chat-nav">
        <div
          v-for="(item, i) in navItems"
          :key="item.idx"
          class="nav-dot"
          :class="{ active: i === activeNav }"
          :title="item.preview"
          @click="scrollToMsg(item.idx)"
        >{{ i + 1 }}</div>
      </div>

      <!-- 底部输入区 -->
      <div class="ds-input-bar">
        <div class="input-wrapper">
          <div class="input-actions-left">
            <button class="btn-input-tool" title="附加文件"><span>&#128206;</span></button>
            <button class="btn-input-tool" title="插入代码片段"><span>&lt;/&gt;</span></button>
          </div>
          <textarea
            ref="inputEl"
            v-model="inputText"
            class="ds-textarea"
            :rows="1"
            placeholder="描述你想要生成的代码应用..."
            :disabled="generating || modifying"
            @input="autoResize"
            @keydown.enter="handleEnter"
          ></textarea>
          <div class="input-actions-right">
            <span class="input-hint">Enter 发送 · Shift+Enter 换行</span>
            <button
              v-if="!generating && !modifying"
              class="btn-send"
              :disabled="!inputText.trim()"
              @click="handleGenerate"
            >
              <span>&#8593;</span>
            </button>
            <button v-else class="btn-stop" @click="stopGenerating">
              <span>&#9632;</span>
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, watch, onBeforeUnmount, onMounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateCodeStream, modifyCodeStream } from '@/api/ai'
import { saveApplication, listConversations, getMessages, clearConversations } from '@/api/app'
import CodeViewer from '@/components/CodeViewer.vue'
import { renderMarkdown, copyToClipboard } from '@/utils/markdown'

// Markdown 渲染（缓存结果避免重复计算）
const mdCache = new Map()
function renderMd(text) {
  if (!text) return ''
  const key = text.substring(0, 200)
  if (!mdCache.has(key)) mdCache.set(key, renderMarkdown(text))
  return mdCache.get(key)
}

// ====== 状态 ======
const genType = ref('NATIVE')
const inputText = ref('')
const streamText = ref('')
const generating = ref(false)
const modifying = ref(false)
const chatArea = ref(null)
const inputEl = ref(null)
const modifyPrompt = ref('')
const selectedElement = ref(null)
const sidebarCollapsed = ref(false)
const previewSubMode = ref('preview')
const editSubMode = ref('preview')  // preview | edit
const modelName = ref('glm-4.5-air')
let abortController = null
let modifyAbort = null

// 消息存储 + UI 状态
const conversations = reactive([])
const activeConvId = ref(null)
const folded = reactive({})
const previewing = reactive({})
const segFolded = reactive({})
const segPreviewing = reactive({})
const activeGroup = ref(-1)
const activeNav = ref(-1)

// 导航：每个用户问题作为一个锚点
const navItems = computed(() => {
  const items = []
  currentConv.value?.messages.forEach((m, i) => {
    if (m.role === 'USER') items.push({ idx: i, preview: m.content?.substring(0, 25) || '' })
  })
  return items
})

function scrollToMsg(idx) {
  const el = document.querySelector(`[data-msg-idx="${idx}"]`)
  if (el) { el.scrollIntoView({ behavior: 'smooth', block: 'start' }); activeNav.value = navItems.value.findIndex(n => n.idx === idx) }
}
const currentConv = computed(() => conversations.find(c => c.id === activeConvId.value) || conversations[0])

// 初始化第一个对话
function ensureConv() {
  if (conversations.length === 0 || !conversations.find(c => c.id === activeConvId.value)) {
    const newConv = reactive({ id: Date.now(), title: '新对话', messages: [], codeOutput: '' })
    conversations.unshift(newConv)
    activeConvId.value = newConv.id
  }
}
ensureConv()

const examplePrompts = [
  '生成一个用户登录注册的 Spring Boot Controller',
  '帮我写一个 Vue3 数据表格组件，支持分页和排序',
  '生成一个文件上传下载工具类',
  '用 HTML 生成一个类似淘宝的商店页面'
]

// ====== 计算属性 ======
const isBackendCode = computed(() => {
  const code = currentConv.value?.codeOutput || ''
  if (!code) return false
  if (/├──|└──|│   ├──|## .*项目|文件结构/.test(code)) return true
  return !/<template\b|<html\b/i.test(code)
})

/** 为指定代码构建预览 HTML（不再是全局 computed） */
function buildPreviewHtml(code, editMode = false) {
  if (!code) return '<html><body style="color:#999;display:flex;align-items:center;justify-content:center;height:100vh;background:#1a1a2e;font-family:sans-serif">暂无代码</body></html>'

  if (/<!DOCTYPE\s+html|<html[\s>]/i.test(code) && !/<template>/i.test(code)) {
    const nav = '<script>document.addEventListener("click",function(e){var a=e.target.closest("a[href]");if(a){var h=a.getAttribute("href");if(!h||h==="#"||h.startsWith("#"))e.preventDefault()}},!0)<\/script>'
    return code.replace(/<\/body>/i, nav + '</body>').replace(/<\/html>/i, nav + '</html>')
  }

  const tplM = code.match(/<template>([\s\S]*?)<\/template>/i)
  const scrM = code.match(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/i)
  const styM = code.match(/<style[^>]*?>([\s\S]*?)<\/style>/i)
  const template = (tplM ? tplM[1].trim() : '') || code
  const isSetup = scrM ? /\bsetup\b/.test(scrM[0]) : false
  const isExport = scrM ? /export\s+default\s*[{\[]/m.test(scrM[1]) : false
  const style = styM ? styM[1].trim() : ''
  let scr = scrM ? scrM[1].replace(/import\s+.*?from\s+['"].*?['"]\s*;?/g,'').replace(/defineProps\s*\(.*?\)/gs,'/*p*/').replace(/defineEmits\s*\(.*?\)/gs,'/*e*/').replace(/withDefaults\s*\(/g,'(').trim() : ''
  const esc = (template||'').replace(/\\/g,'\\\\').replace(/`/g,'\\`').replace(/<\/script>/gi,'<\\/script>').replace(/\$/g,'\\$')

  let boot = ''
  if (scrM && isExport) {
    boot = `try{${scr.replace(/export\s+default\s+/,'var __c__=')};var cmp=typeof __c__!=='undefined'?__c__:{template:\`${esc}\`};createApp(cmp).use(ElementPlus).mount('#app')}catch(e){showErr(e)}`
  } else if (scrM && isSetup) {
    const names = []; const re = /(?:const|let|var)\s+(\w+)\s*=\s*(?:ref|reactive|computed)/g; let m; while((m=re.exec(scr))!==null) { if(!names.includes(m[1])) names.push(m[1]) }
    const fre = /function\s+(\w+)\s*\(/g; while((m=fre.exec(scr))!==null) { if(!names.includes(m[1])) names.push(m[1]) }
    const ret = names.length>0?`return {${names.join(',')}}`:'return {}'
    boot = `try{var cmp={template:\`${esc}\`,setup(){${scr};${ret}}};createApp(cmp).use(ElementPlus).mount('#app')}catch(e){showErr(e)}`
  } else if (scrM) {
    boot = `try{${scr};var cmp=(typeof appComponent!=='undefined')?appComponent:{template:\`${esc}\`};createApp(cmp).use(ElementPlus).mount('#app')}catch(e){showErr(e)}`
  } else {
    boot = `try{createApp({template:\`${esc}\`}).use(ElementPlus).mount('#app')}catch(e){showErr(e)}`
  }

  return `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8">
<script src="https://unpkg.com/vue@3/dist/vue.global.prod.js"><\/script>
<script src="https://unpkg.com/element-plus/dist/index.full.min.js"><\/script>
<link rel="stylesheet" href="https://unpkg.com/element-plus/dist/index.css">
<style>[v-cloak]{display:none}*{box-sizing:border-box;margin:0;padding:0}body{font-family:system-ui,sans-serif;padding:16px;background:#fff}#err{position:fixed;top:0;left:0;right:0;background:#f56c6c;color:#fff;padding:12px 16px;font-size:13px;white-space:pre-wrap;display:none;z-index:999}${style}</style></head>
<body><div id="err"></div><div id="app" v-cloak>${template}</div>
<script>document.addEventListener('click',function(e){var a=e.target.closest('a[href]');if(a){var h=a.getAttribute('href');if(!h||h==='#'||h.startsWith('#'))e.preventDefault()}},!0);
var {createApp,ref,reactive,computed,onMounted,watch,nextTick,defineComponent}=Vue;var ElementPlus=window.ElementPlus;
function showErr(e){var el=document.getElementById('err');el.style.display='block';el.textContent='渲染错误: '+(e.message||String(e))}
var __EDIT=${editMode};
if(__EDIT){(function(){
  var ov=document.createElement('div');ov.id='__picker';ov.style.cssText='position:fixed;pointer-events:none;border:2px solid #f56c6c;background:rgba(245,108,108,.08);z-index:99999;display:none;border-radius:2px';
  document.body.appendChild(ov);
  document.addEventListener('mouseover',function(e){var t=e.target;if(t===ov||t.id==='__picker')return;var r=t.getBoundingClientRect();ov.style.display='block';ov.style.top=r.top+'px';ov.style.left=r.left+'px';ov.style.width=r.width+'px';ov.style.height=r.height+'px'});
  document.addEventListener('mouseout',function(){ov.style.display='none'});
  document.addEventListener('click',function(e){e.preventDefault();e.stopPropagation();var t=e.target;ov.style.borderColor='#67c23a';ov.style.background='rgba(103,194,58,.1)';
    var info={tag:t.tagName.toLowerCase(),id:t.id||'',cls:(t.className||'').toString(),text:(t.textContent||'').substring(0,80),html:t.outerHTML.substring(0,500)};
    window.parent.postMessage({type:'element-selected',payload:info},'*'); },!0);
})();}
${boot}
<\/script></body></html>`
}

const editMode = computed(() => previewSubMode.value === 'edit')

// 流式输出实时拆分文本 + 代码
const streamExplain = computed(() => {
  const t = streamText.value || ''
  if (!t) return ''
  const m = t.match(/```/)
  return m ? t.substring(0, m.index).trim() : ''
})
const streamLang = computed(() => {
  const t = streamText.value || ''
  if (!t) return 'text'
  const m = t.match(/```(\w*)/)
  return m?.[1] || 'text'
})
const streamCode = computed(() => {
  const t = streamText.value
  const m = t.match(/```\w*[\s\S]*?[\r\n]([\s\S]*)/)
  if (!m) return ''
  const code = m[1].replace(/```[\s\S]*$/, '') // 去掉尾部可能出现的闭合 ```
  return code
})

const previewHtml = computed(() => buildPreviewHtml(currentConv.value?.codeOutput || ''))

// ====== 路由守卫 ======
onBeforeRouteLeave((to, from, next) => {
  if (generating.value) {
    ElMessageBox.confirm('代码正在生成中，离开将丢失当前进度', '提示', {
      confirmButtonText: '确定离开', cancelButtonText: '继续等待', type: 'warning'
    }).then(() => { stopGenerating(); next() }).catch(() => next(false))
  } else { next() }
})


// ====== 函数 ======
function autoResize() { if (inputEl.value) { inputEl.value.style.height = 'auto'; inputEl.value.style.height = inputEl.value.scrollHeight + 'px' } }
let lastSendTime = 0
function handleEnter(e) {
  if (e.shiftKey) return
  e.preventDefault()
  // 防抖：500ms 内不允许重复发送
  const now = Date.now()
  if (now - lastSendTime < 500) return
  lastSendTime = now
  handleGenerate()
}
let _scrollRAF = null
function scrollToBottom() {
  if (_scrollRAF) return
  _scrollRAF = requestAnimationFrame(() => {
    if (chatArea.value) chatArea.value.scrollTop = chatArea.value.scrollHeight
    _scrollRAF = null
  })
}
// ESC 终止生成/修改
function handleKeydown(e) {
  if (e.key === 'Escape' && (generating.value || modifying.value)) {
    if (generating.value) stopGenerating()
    if (modifying.value) { modifyAbort?.abort(); modifying.value = false }
  }
}
onMounted(() => document.addEventListener('keydown', handleKeydown))
onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (abortController) { abortController.abort(); abortController = null }
  if (modifyAbort) { modifyAbort.abort(); modifyAbort = null }
})
function toggleFold(idx) { folded[idx] = !folded[idx] }
function toggleSegFold(idx, si) { segFolded[idx + '-' + si] = !segFolded[idx + '-' + si] }
function toggleSegPreview(idx, si) { segPreviewing[idx + '-' + si] = !segPreviewing[idx + '-' + si] }

/** 获取消息的分段列表（兼容旧格式 msg.text/msg.code） */
function getSegments(msg) {
  if (msg.segments && msg.segments.length) return msg.segments
  // 从旧格式 text + code 构建分段
  const segs = []
  if (msg.text) segs.push({ type: 'text', content: msg.text })
  if (msg.code) segs.push({ type: 'code', content: msg.code, language: msg.language || 'text', filename: '' })
  // 兜底：如果 text/code 都为空但有原始 content，显示为文本
  if (segs.length === 0 && msg.content && typeof msg.content === 'string' && msg.content.trim()) {
    // 尝试从 raw content 解析分段
    const parsed = parseAiResponse(msg.content)
    if (parsed.segments && parsed.segments.length) return parsed.segments
    // 实在不行，直接显示原始内容
    segs.push({ type: 'text', content: msg.content.trim() })
  }
  return segs
}

function startNewChat() {
  const newConv = reactive({ id: Date.now(), title: '新对话', messages: [], codeOutput: '' })
  conversations.unshift(newConv)
  activeConvId.value = newConv.id
  inputText.value = ''; streamText.value = ''
  scrollToBottom()
  nextTick(() => inputEl.value?.focus())
}

/** 从后端加载对话列表（始终同步，去重合并） */
async function loadConversationsFromBackend() {
  try {
    const res = await listConversations({ type: 'NATIVE' })
    if (res.data && res.data.length > 0) {
      // 移除空的本地占位对话（无 backendId 且无消息的临时对话）
      for (let i = conversations.length - 1; i >= 0; i--) {
        const c = conversations[i]
        if (typeof c.id === 'number' && c.id > Date.now() - 5000 && c.messages.length === 0 && !c.backendId) {
          conversations.splice(i, 1)
        }
      }
      // 将已有 backendId 的本地对话的 ID 更新为后端 ID，避免重复
      res.data.forEach(bc => {
        const local = conversations.find(c => c.backendId === bc.id && c.id !== bc.id)
        if (local) {
          if (activeConvId.value === local.id) activeConvId.value = bc.id
          local.id = bc.id
          if (bc.title && bc.title !== '新对话') local.title = bc.title
        }
      })
      // 合并后端对话（按 ID 去重）
      const existingIds = new Set(conversations.map(c => c.id))
      res.data.forEach(c => {
        if (!existingIds.has(c.id)) {
          conversations.push(reactive({ id: c.id, title: c.title || '新对话', messages: [], codeOutput: '' }))
        }
      })
      if (!activeConvId.value || !conversations.find(c => c.id === activeConvId.value)) {
        activeConvId.value = conversations[0]?.id || null
      }
      // 自动加载当前对话的消息
      if (activeConvId.value && typeof activeConvId.value !== 'string') {
        switchConv(activeConvId.value)
      }
    }
  } catch { /* ignore */ }
}

onMounted(loadConversationsFromBackend)
async function switchConv(id) {
  activeConvId.value = id
  // 切换对话时清空旧对话的折叠/预览状态
  Object.keys(folded).forEach(k => delete folded[k])
  Object.keys(previewing).forEach(k => delete previewing[k])
  selectedElement.value = null; modifyPrompt.value = ''
  scrollToBottom()
  nextTick(() => inputEl.value?.focus())
  // 从后端加载该对话的消息
  try {
    const res = await getMessages(id)
    const conv = conversations.find(c => c.id === id)
    if (conv && res.data) {
      conv.messages.length = 0
      res.data.forEach(m => {
        if (m.role === 'AI') {
          const parsed = parseAiResponse(m.content || '')
          conv.messages.push({ role: 'AI', content: m.content, code: parsed.code, text: parsed.text, language: parsed.language })
        } else {
          conv.messages.push({ role: 'USER', content: m.content, code: '', text: '', language: '' })
        }
      })
      // 从消息中提取最后一个AI代码作为codeOutput
      const lastAi = [...conv.messages].reverse().find(m => m.role === 'AI')
      if (lastAi) {
        const parsed = parseAiResponse(lastAi.content || '')
        conv.codeOutput = parsed.code || ''
      }
    }
  } catch { /* ignore */ }
}
async function clearHistory() {
  conversations.length = 0; streamText.value = '';
  Object.keys(previewing).forEach(k => delete previewing[k]);
  ensureConv();
  try { await clearConversations(); ElMessage.success('已清空') }
  catch { ElMessage.success('已清空（本地）') }
}
function stopGenerating() {
  if (abortController) abortController.abort()
  if (streamText.value) {
    const conv = currentConv.value
    conv.messages.push({ role: 'AI', content: streamText.value, code: '', text: '', language: '', interrupted: true })
    conv.codeOutput = streamText.value
  }
  generating.value = false; modifying.value = false; streamText.value = ''
}

async function handleContinue(msgIdx) {
  const conv = currentConv.value
  if (!conv || generating.value) return

  // 构建上下文：包含之前的所有用户消息 + 被中断的AI代码
  let context = ''
  for (const m of conv.messages) {
    if (m.role === 'USER') context += '用户: ' + m.content + '\n'
    else if (m.role === 'AI') {
      const code = m.code || m.content || ''
      context += 'AI(已生成): ' + code.substring(0, 2000) + '\n'
    }
  }
  const prompt = '之前的对话:\n' + context + '\n请继续完成上面被中断的代码，保持风格一致，直接续写。'

  // 删除被中断的消息
  if (msgIdx >= 0 && conv.messages[msgIdx]?.interrupted) {
    conv.messages.splice(msgIdx, 1)
  }
  generating.value = true; streamText.value = ''
  abortController = new AbortController()
  try {
    await generateCodeStream(
      { prompt, type: genType.value, language: 'text', conversationId: conv.backendId || null },
      {
        signal: abortController.signal,
        onToken: (t) => { streamText.value += t; scrollToBottom() },
        onDone: (raw) => handleStreamDone(raw, conv, false),
        onError: (err) => { ElMessage.error(err.message || '续写失败'); streamText.value = ''; generating.value = false }
      }
    )
  } catch (err) { ElMessage.error(err.message || '网络错误'); generating.value = false; streamText.value = '' }
}
function togglePreview(idx) { previewing[idx] = !previewing[idx] }
async function copyCode(text) {
  const ok = await copyToClipboard(text)
  if (ok) ElMessage.success('已复制')
  else ElMessage({ message: '复制失败，请手动选中代码复制', type: 'warning', duration: 5000 })
}
function clearSelection() { selectedElement.value = null; modifyPrompt.value = '' }

function detectPromptLang(prompt) {
  const p = prompt.toLowerCase()
  if (/html|网页|页面|前端|界面|css|div|table|表单/.test(p) && !/vue|react|组件/.test(p)) return 'html'
  if (/vue|vue3|vuejs|\.vue|组件/.test(p)) return 'vue'
  if (/python|py|爬虫|flask|django/.test(p)) return 'python'
  if (/java|spring|springboot|后端|controller|service|接口|maven/.test(p)) return 'java'
  if (/javascript|js|nodejs|react|angular|typescript|ts/.test(p)) return 'javascript'
  return 'java'
}

function parseAiResponse(raw) {
  // 提取所有代码块，识别 // File: xxx 文件头
  const blockRe = /```(\w*)\s*[\r\n]*([\s\S]*?)```/g
  const blocks = []
  let m
  while ((m = blockRe.exec(raw)) !== null) {
    let code = m[2].trim()
    if (!code) continue
    // 提取 // File: 声明的文件名
    let fileName = ''
    const flMatch = code.match(/^\s*\/\/\s*File:\s*(.+)/m)
    if (flMatch) { fileName = flMatch[1].trim(); code = code.replace(/^\s*\/\/\s*File:[^\n]*\n?/, '').trim() }
    if (!code) continue
    blocks.push({ lang: m[1] || detectLang(code), code, fileName })
  }
  if (blocks.length > 0) {
    const code = blocks.map(b => (b.fileName ? `// ===== ${b.fileName} =====\n` : '') + b.code).join('\n\n')
    const text = raw.replace(/```[\s\S]*?```/g, '').trim()
    return { text, code, language: blocks[0].lang }
  }
  return { text: '', code: raw.trim(), language: detectLang(raw) }
}

function detectLang(code) {
  if (/<template|<script|<style/i.test(code)) return 'vue'
  if (/public\s+class|@RestController|@Service|@Autowired|import\s+java/i.test(code)) return 'java'
  if (/def\s+\w+\s*\(|import\s+\w+|class\s+\w+:|if\s+__name__/i.test(code) && !/public class/i.test(code)) return 'python'
  if (/<!DOCTYPE html|<html|<div|<span/i.test(code) && !/<template/i.test(code)) return 'html'
  if (/console\.log|const\s+\w+\s*=\s*(require|function|\(.*=>)|export\s+(default|const)/i.test(code)) return 'javascript'
  if (/#include|int\s+main|printf|scanf/i.test(code)) return 'c'
  return 'text'
}


// ====== 生成 ======
async function handleGenerate() {
  if (!inputText.value.trim() || generating.value) return
  const rawPrompt = inputText.value.trim()
  inputText.value = ''
  const conv = currentConv.value
  // 有历史对话时，把之前的 AI 生成代码作为"当前代码"上下文带过去
  let fullPrompt = rawPrompt
  const lastAi = [...conv.messages].reverse().find(m => m.role === 'AI' && m.code)
  if (lastAi) {
    fullPrompt = '当前代码:\n```\n' + lastAi.code.substring(0, 3000) + '\n```\n\n用户要求: ' + rawPrompt + '\n\n请在现有代码基础上修改，不要重写。'
  }
  conv.messages.push({ role: 'USER', content: rawPrompt })
  if (conv.messages.length === 2) conv.title = rawPrompt.substring(0, 30)
  scrollToBottom()
  generating.value = true; streamText.value = ''
  abortController = new AbortController()

  try {
    await generateCodeStream(
      { prompt: fullPrompt, originalPrompt: rawPrompt, type: genType.value, language: detectPromptLang(rawPrompt), conversationId: conv.backendId || null },
      {
        signal: abortController.signal,
        onToken: (t) => { streamText.value += t; scrollToBottom() },
        onDone: (raw) => handleStreamDone(raw, conv, false),
        onError: (err) => { ElMessage.error(err.message || '生成失败'); streamText.value = ''; generating.value = false }
      }
    )
  } catch (err) { ElMessage.error(err.message || '网络错误'); generating.value = false; streamText.value = '' }
}

function handleStreamDone(data, conv, autoSave) {
  let j; try { j = JSON.parse(data) } catch { j = { code: data } }
  conv.codeOutput = j.code || ''
  conv.messages.push({ role: 'AI', content: j.code || data, code: j.code || '', text: j.text || '', language: j.language || '' })
  if (j.conversationId) conv.backendId = j.conversationId
  streamText.value = ''; generating.value = false; scrollToBottom()
  if (autoSave && j.code) saveApplication({ name: conv.title || '未命名', sourceCode: j.code, type: genType.value, language: j.language || '', description: '' }).then(() => {
    if (genType.value === 'ENGINEERING') ElMessage.success('工程项目已保存，可在「我的应用」查看项目结构')
  }).catch(() => {})
}

// ====== iframe 消息 ======
function onIframeMessage(e) {
  if (e.data?.type === 'element-selected') selectedElement.value = e.data.payload
}
onMounted(() => window.addEventListener('message', onIframeMessage))
onBeforeUnmount(() => window.removeEventListener('message', onIframeMessage))

async function handleModify() {
  if (!modifyPrompt.value.trim() || modifying.value || !selectedElement.value) return
  const el = selectedElement.value
  const info = `<${el.tag}${el.id?' id='+el.id:''}${el.cls?' class='+el.cls:''}> 文本:"${el.text}"`
  const prompt = modifyPrompt.value.trim()
  modifyPrompt.value = ''; modifying.value = true
  modifyAbort = new AbortController()
  try {
    await modifyCodeStream(
      { currentCode: currentConv.value.codeOutput, elementInfo: info, modifyPrompt: prompt },
      { signal: modifyAbort.signal,
        onToken: (t) => { /* modify doesn't stream to UI for now */ },
        onDone: (data) => {
          let code = data; try { code = JSON.parse(data).code || data } catch {}
          currentConv.value.codeOutput = code; modifying.value = false; selectedElement.value = null
          ElMessage.success('修改完成')
        },
        onError: (err) => { ElMessage.error(err.message || '修改失败'); modifying.value = false }
      }
    )
  } catch (err) { ElMessage.error(err.message || '网络错误'); modifying.value = false }
}

async function saveAsApplication() {
  const code = currentConv.value?.codeOutput
  if (!code) { ElMessage.warning('没有可保存的代码'); return }
  try {
    await saveApplication({
      name: currentConv.value?.title || '未命名应用',
      sourceCode: code,
      type: genType.value,
      language: detectLang(code),
      description: ''
    })
    ElMessage.success('应用已保存')
  } catch { ElMessage.error('保存失败') }
}
function scrollToGroup(i) { activeGroup.value = i; scrollToBottom() }
</script>

<style>
/* ====== Reset & Global ====== */
.ds-app { display: flex; height: 100vh; background: #0d1117; color: #c9d1d9; font-family: -apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif; overflow: hidden; --accent: #7c8aff; --accent2: #5b6af0; --bg-sidebar: #0a0d13; --bg-main: #0d1117; --bg-card: #141821; --bg-code: #1a1e2a; --border: rgba(255,255,255,0.07); --text: #c9d1d9; --text-dim: #6b7280; --radius: 10px; }
*, *::before, *::after { box-sizing: border-box; }
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 3px; }
::-webkit-scrollbar-track { background: transparent; }

/* ====== 左侧边栏（可折叠） ====== */
.ds-sidebar { width: 260px; min-width: 260px; background: var(--bg-sidebar); display: flex; flex-direction: column; border-right: 1px solid var(--border); transition: all .25s ease; overflow: hidden; flex-shrink: 0; position: relative; }
.ds-sidebar.collapsed { width: 0; min-width: 0; border-right: none; }
.sidebar-toggle { width: 24px; height: 24px; background: var(--bg-card); color: var(--text-dim); border: 1px solid var(--border); border-radius: 50%; cursor: pointer; font-size: 10px; display: flex; align-items: center; justify-content: center; z-index: 20; flex-shrink: 0; margin: auto 0; padding: 0; line-height: 1; transition: all .15s; }
.sidebar-toggle:hover { background: rgba(129,140,248,0.15); color: var(--accent); border-color: rgba(129,140,248,0.3); }
.sidebar-inner { display: flex; flex-direction: column; height: 100%; width: 260px; overflow: hidden; }
.sidebar-top { padding: 16px; border-bottom: 1px solid var(--border); }
.btn-new-chat { width: 100%; padding: 10px 16px; background: linear-gradient(135deg, rgba(124,138,255,0.1), rgba(124,138,255,0.04)); color: var(--text); border: 1px solid rgba(124,138,255,0.15); border-radius: var(--radius); font-size: 14px; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: all .2s; }
.btn-new-chat:hover { background: linear-gradient(135deg, rgba(124,138,255,0.18), rgba(124,138,255,0.08)); border-color: rgba(124,138,255,0.3); }
.btn-new-chat span { font-size: 18px; color: var(--accent); }
.model-switch { margin-top: 12px; }
.ds-select { width: 100%; padding: 8px 12px; background: var(--bg-card); color: var(--text); border: 1px solid var(--border); border-radius: 6px; font-size: 13px; outline: none; cursor: pointer; }
.chat-history { flex: 1; overflow-y: auto; padding: 8px; }
.history-label { font-size: 11px; color: var(--text-dim); text-transform: uppercase; letter-spacing: 1px; padding: 8px 8px 4px; }
.history-empty { padding: 16px 8px; color: var(--text-dim); font-size: 13px; text-align: center; }
.history-item { padding: 10px 12px; border-radius: 8px; cursor: pointer; transition: all .2s; margin-bottom: 2px; }
.history-item:hover { background: rgba(124,138,255,0.06); }
.history-item.active { background: rgba(124,138,255,0.12); border-left: 2px solid var(--accent); }
.history-title { display: block; font-size: 13px; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.history-time { display: block; font-size: 11px; color: var(--text-dim); margin-top: 2px; }
.sidebar-bottom { padding: 12px 16px; border-top: 1px solid var(--border); display: flex; gap: 8px; }
.btn-icon { width: 36px; height: 36px; background: transparent; color: var(--text-dim); border: 1px solid var(--border); border-radius: 6px; cursor: pointer; font-size: 14px; display: flex; align-items: center; justify-content: center; transition: all .2s; }
.btn-icon:hover { background: rgba(255,255,255,0.04); color: var(--text); }

/* ====== 中间主区域 ====== */
.ds-main { flex: 1; display: flex; flex-direction: column; min-width: 0; width: 0; }
.ds-header { height: 52px; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; border-bottom: 1px solid var(--border); background: var(--bg-sidebar); flex-shrink: 0; }
.header-left { display: flex; align-items: center; gap: 10px; }
.logo-dot { width: 10px; height: 10px; border-radius: 50%; background: linear-gradient(135deg, var(--accent), #a78bfa); }
.logo-text { font-size: 16px; font-weight: 600; color: #e5e7eb; letter-spacing: -0.5px; }
.logo-beta { font-size: 10px; padding: 2px 6px; border-radius: 4px; background: rgba(129,140,248,0.15); color: var(--accent); }
.header-center { display: flex; align-items: center; }
.model-badge { display: flex; align-items: center; gap: 6px; padding: 6px 14px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 20px; font-size: 13px; color: var(--text); }
.badge-dot { width: 6px; height: 6px; border-radius: 50%; background: #34d399; }
.header-right { display: flex; gap: 4px; }
.btn-tool { width: 32px; height: 32px; background: transparent; color: var(--text-dim); border: none; border-radius: 6px; cursor: pointer; font-size: 14px; display: flex; align-items: center; justify-content: center; transition: all .15s; }
.btn-tool:hover { background: rgba(255,255,255,0.06); color: var(--text); }

/* ====== 对话区 ====== */
.ds-chat { flex: 1; overflow-y: auto; padding: 20px clamp(16px, 3%, 48px) 20px clamp(16px, 3%, 32px); min-height: 0; position: relative; }
.chat-welcome { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 50vh; text-align: center; max-width: 720px; margin: 0 auto; }
.welcome-icon { width: 72px; height: 72px; border-radius: 50%; background: rgba(129,140,248,0.08); display: flex; align-items: center; justify-content: center; margin-bottom: 24px; position: relative; }
.welcome-glow { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, var(--accent), #a78bfa); opacity: 0.6; filter: blur(20px); }
.chat-welcome h2 { font-size: 24px; font-weight: 500; color: #e5e7eb; margin: 0 0 8px; }
.chat-welcome p { font-size: 14px; color: var(--text-dim); margin: 0 0 24px; }
.quick-prompts { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; max-width: 600px; }
.quick-prompt { padding: 8px 16px; background: var(--bg-card); color: var(--text); border: 1px solid var(--border); border-radius: 20px; font-size: 13px; cursor: pointer; transition: all .2s; }
.quick-prompt:hover { background: rgba(129,140,248,0.10); border-color: rgba(129,140,248,0.25); color: var(--accent); }

/* ====== 消息 ====== */
.msg-row { display: flex; gap: 12px; margin-bottom: 28px; max-width: 100%; }
.msg-user { justify-content: flex-end; margin-left: auto; }
.msg-avatar { width: 32px; height: 32px; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; flex-shrink: 0; }
.avatar-user { background: linear-gradient(135deg, #6366f1, #818cf8); color: #fff; width: 32px; height: 32px; border-radius: 6px; display: flex; align-items: center; justify-content: center; }
.avatar-ai { background: rgba(129,140,248,0.12); color: var(--accent); width: 32px; height: 32px; border-radius: 6px; display: flex; align-items: center; justify-content: center; }
.msg-body { flex: 1; min-width: 0; }
.bubble-user { background: linear-gradient(135deg, rgba(124,138,255,0.15), rgba(124,138,255,0.06)); color: #e5e7eb; padding: 10px 18px; border-radius: 14px 14px 6px 14px; display: inline-block; max-width: 85%; font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
.ai-explain { font-size: 14px; color: var(--text); line-height: 1.7; margin-bottom: 12px; word-break: break-word; }
.ai-explain :deep(h1),.ai-explain :deep(h2),.ai-explain :deep(h3) { margin: 12px 0 8px; font-weight: 600; color: #e5e7eb; }
.ai-explain :deep(h1) { font-size: 18px; }
.ai-explain :deep(h2) { font-size: 16px; }
.ai-explain :deep(h3) { font-size: 14px; }
.ai-explain :deep(strong) { color: #e5e7eb; font-weight: 600; }
.ai-explain :deep(li) { margin: 2px 0 2px 16px; }
.ai-explain :deep(code) { background: rgba(124,138,255,0.12); color: var(--accent); padding: 1px 6px; border-radius: 4px; font-size: 13px; font-family: 'Cascadia Code','JetBrains Mono',monospace; }
.ai-explain :deep(.md-code) { background: var(--bg-code); border: 1px solid var(--border); border-radius: 8px; padding: 12px; margin: 8px 0; overflow: auto; font-size: 12px; line-height: 1.5; }
.ai-explain :deep(.md-code code) { background: transparent; padding: 0; color: #c9d1d9; }
.streaming-text { font-size: 14px; color: var(--text-dim); line-height: 1.7; }

/* ====== 代码+预览 左右分栏 ====== */
.code-row { display: flex; gap: 0; margin-top: 4px; border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; min-height: 200px; }
.code-row .code-section { flex: 1; min-width: 0; border: none; border-radius: 0; margin-top: 0; }
.code-row.has-preview .code-section { flex: 0 0 50%; }
.code-row .preview-panel { width: 0; overflow: hidden; transition: width .25s ease; background: #fff; display: flex; flex-direction: column; border-left: 1px solid var(--border); }
.code-row.has-preview .preview-panel { width: 50%; }
.preview-header { display: flex; align-items: center; justify-content: space-between; padding: 6px 12px; background: #f5f5f5; border-bottom: 1px solid #e5e7eb; font-size: 12px; color: #6b7280; flex-shrink: 0; }
.preview-header button { background: transparent; border: none; font-size: 16px; cursor: pointer; color: #9ca3af; padding: 0 4px; }
.preview-panel .preview-iframe { flex: 1; width: 100%; min-height: 200px; border: none; }
.preview-tabs { display:flex; gap:0 }
.preview-tabs button { padding:4px 14px; border:none; background:transparent; color:#9ca3af; font-size:12px; cursor:pointer; border-bottom:2px solid transparent; transition:all .15s }
.preview-tabs button:hover { color:#374151 }
.preview-tabs button.active { color:#5b6af0; border-bottom-color:#5b6af0; font-weight:500 }
.edit-info-bar { display:flex; align-items:center; gap:8px; padding:5px 12px; background:#fef3c7; color:#92400e; font-size:12px; flex-shrink:0; border-bottom:1px solid #fcd34d }
.edit-info-bar .btn-clear { margin-left:auto; background:transparent; color:#92400e; border:1px solid #fcd34d; border-radius:4px; padding:2px 8px; font-size:11px; cursor:pointer }
.edit-modify-bar { display:flex; gap:8px; padding:8px 12px; background:#fafafa; border-top:1px solid #e5e7eb; flex-shrink:0 }
.edit-modify-bar input { flex:1; padding:6px 10px; border:1px solid #d1d5db; border-radius:6px; font-size:13px; outline:none }
.edit-modify-bar input:focus { border-color:#5b6af0; box-shadow:0 0 0 2px rgba(91,106,240,.1) }
.edit-modify-bar button { padding:6px 16px; background:#5b6af0; color:#fff; border:none; border-radius:6px; font-size:13px; cursor:pointer }
.edit-modify-bar button:disabled { opacity:.5; cursor:default }

/* standalone code (no preview) */
.code-section { background: var(--bg-code); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; margin-top: 6px; box-shadow: 0 2px 8px rgba(0,0,0,0.15); }
.code-header { display: flex; align-items: center; padding: 8px 14px; border-bottom: 1px solid var(--border); gap: 8px; }
.code-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--accent); opacity: 0.6; }
.code-lang-badge { font-size: 12px; color: var(--text-dim); text-transform: uppercase; letter-spacing: 0.5px; }
.code-actions { margin-left: auto; display: flex; gap: 4px; }
.code-actions button { background: transparent; color: var(--text-dim); border: none; padding: 4px 8px; border-radius: 4px; cursor: pointer; font-size: 12px; transition: all .15s; white-space: nowrap; }
.code-actions button:hover { background: rgba(255,255,255,0.04); color: var(--text); }

/* ====== 输入区 ====== */
.ds-input-bar { padding: 16px clamp(16px, 3%, 32px); border-top: 1px solid var(--border); background: var(--bg-sidebar); flex-shrink: 0; }
.input-wrapper { display: flex; align-items: flex-end; gap: 10px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 12px 18px; transition: border-color .25s, box-shadow .25s; max-width: 100%; }
.input-wrapper:focus-within { border-color: rgba(124,138,255,0.4); box-shadow: 0 0 0 3px rgba(124,138,255,0.06); }
.input-actions-left, .input-actions-right { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.btn-input-tool { width: 30px; height: 30px; background: transparent; color: var(--text-dim); border: none; border-radius: 6px; cursor: pointer; font-size: 14px; display: flex; align-items: center; justify-content: center; transition: all .15s; }
.btn-input-tool:hover { background: rgba(255,255,255,0.04); color: var(--text); }
.ds-textarea { flex: 1; background: transparent; border: none; color: var(--text); font-size: 14px; line-height: 1.5; resize: none; outline: none; font-family: inherit; min-height: 24px; max-height: 200px; }
.ds-textarea::placeholder { color: var(--text-dim); }
.input-hint { font-size: 11px; color: var(--text-dim); margin-right: 8px; }
.btn-send { width: 36px; height: 36px; background: linear-gradient(135deg, var(--accent2), #7c3aed); color: #fff; border: none; border-radius: 8px; cursor: pointer; font-size: 16px; display: flex; align-items: center; justify-content: center; transition: all .2s; flex-shrink: 0; box-shadow: 0 2px 8px rgba(124,138,255,0.2); }
.btn-send:hover { transform: scale(1.04); box-shadow: 0 4px 12px rgba(124,138,255,0.3); }
.btn-send:disabled { opacity: 0.3; cursor: default; transform: none; box-shadow: none; }
.btn-stop { width: 34px; height: 34px; background: rgba(239,68,68,0.2); color: #ef4444; border: 1px solid rgba(239,68,68,0.3); border-radius: 6px; cursor: pointer; font-size: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.cursor-blink { animation: blink 1s infinite; color: var(--accent); } @keyframes blink { 50% { opacity: 0; } }
.continue-row { display: flex; align-items: center; gap: 10px; margin-top: 8px; padding: 8px 0; }
.btn-continue { padding: 6px 16px; background: rgba(129,140,248,0.12); color: var(--accent); border: 1px solid rgba(129,140,248,0.25); border-radius: 6px; font-size: 13px; cursor: pointer; transition: all .15s; }
.btn-continue:hover { background: rgba(129,140,248,0.20); border-color: var(--accent); }
.continue-hint { font-size: 12px; color: var(--text-dim); }

/* ====== 快速导航 ====== */
.chat-nav { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); display: flex; flex-direction: column; gap: 6px; z-index: 50; }
.nav-dot { width: 26px; height: 26px; border-radius: 50%; background: var(--bg-card); color: var(--text-dim); border: 1px solid var(--border); font-size: 11px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all .15s; user-select: none; }
.nav-dot:hover { background: rgba(129,140,248,0.12); color: var(--accent); border-color: rgba(129,140,248,0.25); }
.nav-dot.active { background: var(--accent2); color: #fff; border-color: var(--accent2); }
</style>
