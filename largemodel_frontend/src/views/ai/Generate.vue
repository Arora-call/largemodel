<template>
  <div class="ds-app">
    <!-- ====== 左侧边栏 ====== -->
    <aside class="ds-sidebar">
      <div class="sidebar-top">
        <button class="btn-new-chat" @click="startNewChat">
          <span>+</span> 新建对话
        </button>
        <div class="model-switch">
          <select v-model="genType" class="ds-select">
            <option value="NATIVE">代码生成</option>
            <option value="ENGINEERING">工程项目</option>
          </select>
        </div>
      </div>

      <div class="chat-history">
        <div class="history-label">对话历史</div>
        <div v-if="messages.length === 0" class="history-empty">暂无对话记录</div>
        <div
          v-for="(msg, i) in messageGroups"
          :key="i"
          class="history-item"
          :class="{ active: i === activeGroup }"
          @click="scrollToGroup(i)"
        >
          <span class="history-title">{{ msg.preview || '新对话' }}</span>
          <span class="history-time">{{ msg.time }}</span>
        </div>
      </div>

      <div class="sidebar-bottom">
        <button class="btn-icon" title="收藏"><span>&#9733;</span></button>
        <button class="btn-icon" title="清空记录" @click="clearHistory"><span>&#8635;</span></button>
      </div>
    </aside>

    <!-- ====== 中间主区域 ====== -->
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
          <button class="btn-tool" title="复制全部" @click="copyCode(codeOutput)"><span>&#128203;</span></button>
          <button class="btn-tool" title="导出" @click="saveAsApplication"><span>&#128190;</span></button>
          <button class="btn-tool" title="切换暗色模式"><span>&#9790;</span></button>
        </div>
      </header>

      <!-- 对话内容 -->
      <div class="ds-chat" ref="chatArea">
        <div v-if="messages.length === 0" class="chat-welcome">
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
        <div v-for="(msg, idx) in messages" :key="idx" :class="['msg-row', msg.role === 'USER' ? 'msg-user' : 'msg-ai']">
          <div class="msg-avatar">
            <span v-if="msg.role === 'USER'" class="avatar-user">U</span>
            <span v-else class="avatar-ai">AI</span>
          </div>
          <div class="msg-body">
            <!-- 用户消息 -->
            <div v-if="msg.role === 'USER'" class="bubble-user">{{ msg.content }}</div>
            <!-- AI 文字说明 -->
            <div v-if="msg.role === 'AI' && msg.text" class="ai-explain">{{ msg.text }}</div>
            <!-- AI 代码块 -->
            <div v-if="msg.role === 'AI' && msg.code" class="code-section">
              <div class="code-header">
                <span class="code-dot"></span>
                <span class="code-lang-badge">{{ msg.language || 'code' }}</span>
                <div class="code-actions">
                  <button @click="copyCode(msg.code)" title="复制">&#128203;</button>
                  <button @click="toggleFold(idx)" title="折叠">{{ folded[idx] ? '&#9654;' : '&#9660;' }}</button>
                </div>
              </div>
              <pre v-show="!folded[idx]" class="code-content"><code v-html="highlightCode(msg.code, msg.language)"></code></pre>
            </div>
          </div>
        </div>

        <!-- 生成中 -->
        <div v-if="generating" class="msg-row msg-ai">
          <div class="msg-avatar"><span class="avatar-ai">AI</span></div>
          <div class="msg-body">
            <div class="streaming-text">
              {{ streamText || '思考中...' }}<span class="cursor-blink">|</span>
            </div>
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

    <!-- ====== 右侧参数面板 ====== -->
    <aside v-if="showParams" class="ds-params">
      <div class="params-header">
        <span>生成参数</span>
        <button @click="showParams = false">&times;</button>
      </div>
      <div class="param-group">
        <label>Temperature <span class="param-val">{{ temperature }}</span></label>
        <input type="range" min="0" max="2" step="0.1" v-model="temperature" class="ds-slider" />
      </div>
      <div class="param-group">
        <label>Top P <span class="param-val">{{ topP }}</span></label>
        <input type="range" min="0" max="1" step="0.05" v-model="topP" class="ds-slider" />
      </div>
      <div class="param-group">
        <label>最大长度 <span class="param-val">{{ maxTokens }}</span></label>
        <input type="range" min="256" max="8192" step="256" v-model="maxTokens" class="ds-slider" />
      </div>
      <div class="param-group">
        <label>上下文窗口</label>
        <select v-model="contextWindow" class="ds-select">
          <option :value="4">4K</option>
          <option :value="8">8K</option>
          <option :value="16">16K</option>
          <option :value="32">32K</option>
        </select>
      </div>
      <div class="param-group">
        <label class="param-toggle">
          <input type="checkbox" v-model="formatCode" /> 代码格式化
        </label>
      </div>
      <div class="param-group">
        <label class="param-toggle">
          <input type="checkbox" v-model="addComments" /> 注释优化
        </label>
      </div>
    </aside>

    <!-- 参数面板切换按钮 -->
    <button v-if="!showParams" class="params-toggle" @click="showParams = true" title="参数设置">&#9881;</button>

    <!-- 预览 / 编辑浮窗 -->
    <Teleport to="body">
      <div v-if="showOverlay" class="overlay" @click.self="showOverlay = false">
        <div class="overlay-panel">
          <div class="overlay-header">
            <el-radio-group v-model="previewMode" size="small">
              <el-radio-button value="code">代码</el-radio-button>
              <el-radio-button value="preview" :disabled="!codeOutput || isBackendCode">预览</el-radio-button>
              <el-radio-button value="edit" :disabled="!codeOutput || isBackendCode">编辑</el-radio-button>
            </el-radio-group>
            <button @click="showOverlay = false">&times;</button>
          </div>
          <!-- 代码视图 -->
          <div v-show="previewMode === 'code'" class="overlay-code">
            <pre><code>{{ codeOutput || '' }}</code></pre>
          </div>
          <!-- 预览/编辑视图 -->
          <div v-show="previewMode === 'preview' || previewMode === 'edit'" class="overlay-preview">
            <div v-if="previewMode === 'edit'" class="edit-overlay-bar">
              <span v-if="selectedElement">已选中: <strong>{{ selectedElement.tag }}</strong> {{ selectedElement.text?.substring(0, 30) }}</span>
              <span v-else>点击页面元素选择要修改的区域</span>
              <button v-if="selectedElement" class="btn-clear-sel" @click="clearSelection">取消</button>
            </div>
            <iframe :srcdoc="previewHtml" sandbox="allow-scripts allow-same-origin" class="preview-iframe"></iframe>
            <div v-if="previewMode === 'edit' && selectedElement" class="modify-bar">
              <input
                v-model="modifyPrompt"
                class="modify-input"
                placeholder="描述你希望对这个元素做什么修改..."
                :disabled="modifying"
                @keydown.enter="handleModify"
              />
              <button class="btn-modify-send" :disabled="modifying" @click="handleModify">修改</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, watch, onBeforeUnmount, onMounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateCodeStream, modifyCodeStream } from '@/api/ai'

// ====== 状态 ======
const genType = ref('NATIVE')
const inputText = ref('')
const streamText = ref('')
const codeOutput = ref('')
const generating = ref(false)
const modifying = ref(false)
const chatArea = ref(null)
const inputEl = ref(null)
const previewMode = ref('code')
const modifyPrompt = ref('')
const selectedElement = ref(null)
const showOverlay = ref(false)
const showParams = ref(false)
const modelName = ref('glm-4.5-air')
let abortController = null

// 参数调节
const temperature = ref(0.7)
const topP = ref(0.9)
const maxTokens = ref(4096)
const contextWindow = ref(8)
const formatCode = ref(true)
const addComments = ref(true)

// 消息存储
const messages = reactive([])
const folded = reactive({})
const activeGroup = ref(-1)

const examplePrompts = [
  '生成一个用户登录注册的 Spring Boot Controller',
  '帮我写一个 Vue3 数据表格组件，支持分页和排序',
  '生成一个文件上传下载工具类',
  '用 HTML 生成一个类似淘宝的商店页面'
]

// ====== 计算属性 ======
const messageGroups = computed(() => {
  const groups = []
  for (let i = messages.length - 1; i >= 0; i--) {
    if (messages[i].role === 'USER') {
      groups.push({
        preview: messages[i].content?.substring(0, 30) || '',
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
        idx: i
      })
    }
  }
  return groups.slice(0, 20)
})

const isBackendCode = computed(() => {
  const code = codeOutput.value
  if (!code) return false
  if (/├──|└──|│   ├──|## .*项目|文件结构/.test(code)) return true
  return !/<template\b|<html\b/i.test(code)
})

const previewHtml = computed(() => {
  const code = codeOutput.value
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
${boot}
<\/script></body></html>`
})

const editMode = computed(() => previewMode.value === 'edit')

// ====== 路由守卫 ======
onBeforeRouteLeave((to, from, next) => {
  if (generating.value) {
    ElMessageBox.confirm('代码正在生成中，离开将丢失当前进度', '提示', {
      confirmButtonText: '确定离开', cancelButtonText: '继续等待', type: 'warning'
    }).then(() => { stopGenerating(); next() }).catch(() => next(false))
  } else { next() }
})
onBeforeUnmount(() => { if (abortController) abortController.abort() })

// ====== 函数 ======
function autoResize() { if (inputEl.value) { inputEl.value.style.height = 'auto'; inputEl.value.style.height = inputEl.value.scrollHeight + 'px' } }
function handleEnter(e) { if (!e.shiftKey) { e.preventDefault(); handleGenerate() } }
function scrollToBottom() { nextTick(() => { if (chatArea.value) chatArea.value.scrollTop = chatArea.value.scrollHeight }) }
function toggleFold(idx) { folded[idx] = !folded[idx] }
function startNewChat() { inputText.value = ''; scrollToBottom() }
function clearHistory() { messages.length = 0; codeOutput.value = ''; streamText.value = ''; showOverlay.value = false; ElMessage.success('已清空') }
function showPreview() { showOverlay.value = true }
function stopGenerating() { if (abortController) abortController.abort(); generating.value = false; modifying.value = false; streamText.value = '' }
function copyCode(text) { navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制')).catch(() => ElMessage.error('复制失败')) }
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
  const fm = raw.match(/```(\w*)\s*[\r\n]*([\s\S]*?)```/)
  if (fm) {
    const lang = fm[1] || detectLang(fm[2])
    const before = raw.substring(0, fm.index).trim()
    const after = raw.substring(fm.index + fm[0].length).trim()
    return { text: [before, after].filter(Boolean).join('\n'), code: fm[2].trim(), language: lang }
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

// 简单语法高亮
function highlightCode(code, lang) {
  let html = code.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
  if (lang === 'java' || lang === 'javascript' || lang === 'vue' || lang === 'python' || lang === 'c') {
    html = html
      .replace(/\b(public|class|static|void|int|String|boolean|return|if|else|for|while|new|import|package|const|let|var|function|export|default|def|from|None|True|False|import|print)\b/g, '<span class="tk-kw">$1</span>')
      .replace(/(&quot;.*?&quot;|&#39;.*?&#39;|".*?"|'.*?'|`.*?`)/g, '<span class="tk-str">$1</span>')
      .replace(/(\/\/.*)/g, '<span class="tk-cm">$1</span>')
      .replace(/\b(\d+\.?\d*)\b/g, '<span class="tk-num">$1</span>')
      .replace(/\b(@\w+)\b/g, '<span class="tk-at">$1</span>')
  }
  return html
}

// ====== 生成 ======
async function handleGenerate() {
  if (!inputText.value.trim() || generating.value) return
  const prompt = inputText.value.trim()
  inputText.value = ''
  messages.push({ role: 'USER', content: prompt })
  scrollToBottom()
  generating.value = true; streamText.value = ''
  abortController = new AbortController()

  try {
    await generateCodeStream(
      { prompt, type: genType.value, language: detectPromptLang(prompt) },
      {
        signal: abortController.signal,
        onToken: (t) => { streamText.value += t; scrollToBottom() },
        onDone: (raw) => {
          const p = parseAiResponse(raw)
          codeOutput.value = p.code
          messages.push({ role: 'AI', content: raw, code: p.code, text: p.text, language: p.language })
          streamText.value = ''; generating.value = false
          showOverlay.value = true
          activeGroup.value = messageGroups.value.length - 1
          scrollToBottom()
        },
        onError: (err) => {
          ElMessage.error(err.message || '生成失败')
          messages.push({ role: 'AI', content: '[生成失败] ' + err.message, code: '', text: '' })
          streamText.value = ''; generating.value = false; scrollToBottom()
        }
      }
    )
  } catch (err) { ElMessage.error(err.message || '网络错误'); generating.value = false; streamText.value = '' }
}

// ====== 修改 ======
async function handleModify() {
  if (!modifyPrompt.value.trim() || modifying.value || !selectedElement.value) return
  const el = selectedElement.value
  const info = `<${el.tag}${el.id?' id="'+el.id+'"':''}${el.cls?' class="'+el.cls+'"':''}> 文本:"${el.text}"`
  const prompt = modifyPrompt.value.trim()
  modifyPrompt.value = ''; modifying.value = true; streamText.value = ''
  abortController = new AbortController()

  try {
    await modifyCodeStream(
      { currentCode: codeOutput.value, elementInfo: info, modifyPrompt: prompt },
      {
        signal: abortController.signal,
        onToken: (t) => { streamText.value += t; scrollToBottom() },
        onDone: (newCode) => {
          codeOutput.value = newCode; streamText.value = ''; modifying.value = false
          selectedElement.value = null; ElMessage.success('修改完成')
        },
        onError: (err) => { ElMessage.error(err.message || '修改失败'); modifying.value = false; streamText.value = '' }
      }
    )
  } catch (err) { ElMessage.error(err.message || '网络错误'); modifying.value = false; streamText.value = '' }
}

// ====== iframe 消息 ======
function onIframeMessage(e) { if (e.data?.type === 'element-selected') selectedElement.value = e.data.payload }
onMounted(() => window.addEventListener('message', onIframeMessage))
onBeforeUnmount(() => window.removeEventListener('message', onIframeMessage))

function saveAsApplication() { ElMessage.info('保存功能将在应用管理模块完成') }
function scrollToGroup(i) { activeGroup.value = i; scrollToBottom() }
</script>

<style>
/* ====== Reset & Global ====== */
.ds-app { display: flex; height: 100vh; background: #0f0f1a; color: #d1d5db; font-family: -apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif; overflow: hidden; --accent: #818cf8; --accent2: #6366f1; --bg-sidebar: #09090f; --bg-main: #0f0f1a; --bg-card: #161625; --bg-code: #1a1a2e; --border: rgba(255,255,255,0.06); --text: #d1d5db; --text-dim: #6b7280; --radius: 8px; }
*, *::before, *::after { box-sizing: border-box; }
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 3px; }
::-webkit-scrollbar-track { background: transparent; }

/* ====== 左侧边栏 ====== */
.ds-sidebar { width: 260px; min-width: 260px; background: var(--bg-sidebar); display: flex; flex-direction: column; border-right: 1px solid var(--border); }
.sidebar-top { padding: 16px; border-bottom: 1px solid var(--border); }
.btn-new-chat { width: 100%; padding: 10px 16px; background: transparent; color: var(--text); border: 1px solid var(--border); border-radius: var(--radius); font-size: 14px; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: all .2s; }
.btn-new-chat:hover { background: rgba(255,255,255,0.04); border-color: rgba(255,255,255,0.12); }
.btn-new-chat span { font-size: 18px; color: var(--accent); }
.model-switch { margin-top: 12px; }
.ds-select { width: 100%; padding: 8px 12px; background: var(--bg-card); color: var(--text); border: 1px solid var(--border); border-radius: 6px; font-size: 13px; outline: none; cursor: pointer; }
.chat-history { flex: 1; overflow-y: auto; padding: 8px; }
.history-label { font-size: 11px; color: var(--text-dim); text-transform: uppercase; letter-spacing: 1px; padding: 8px 8px 4px; }
.history-empty { padding: 16px 8px; color: var(--text-dim); font-size: 13px; text-align: center; }
.history-item { padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all .15s; margin-bottom: 2px; }
.history-item:hover { background: rgba(255,255,255,0.04); }
.history-item.active { background: rgba(129,140,248,0.10); }
.history-title { display: block; font-size: 13px; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.history-time { display: block; font-size: 11px; color: var(--text-dim); margin-top: 2px; }
.sidebar-bottom { padding: 12px 16px; border-top: 1px solid var(--border); display: flex; gap: 8px; }
.btn-icon { width: 36px; height: 36px; background: transparent; color: var(--text-dim); border: 1px solid var(--border); border-radius: 6px; cursor: pointer; font-size: 14px; display: flex; align-items: center; justify-content: center; transition: all .2s; }
.btn-icon:hover { background: rgba(255,255,255,0.04); color: var(--text); }

/* ====== 中间主区域 ====== */
.ds-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
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
.ds-chat { flex: 1; overflow-y: auto; padding: 24px; }
.chat-welcome { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 60vh; text-align: center; }
.welcome-icon { width: 72px; height: 72px; border-radius: 50%; background: rgba(129,140,248,0.08); display: flex; align-items: center; justify-content: center; margin-bottom: 24px; position: relative; }
.welcome-glow { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, var(--accent), #a78bfa); opacity: 0.6; filter: blur(20px); }
.chat-welcome h2 { font-size: 24px; font-weight: 500; color: #e5e7eb; margin: 0 0 8px; }
.chat-welcome p { font-size: 14px; color: var(--text-dim); margin: 0 0 24px; }
.quick-prompts { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; max-width: 600px; }
.quick-prompt { padding: 8px 16px; background: var(--bg-card); color: var(--text); border: 1px solid var(--border); border-radius: 20px; font-size: 13px; cursor: pointer; transition: all .2s; }
.quick-prompt:hover { background: rgba(129,140,248,0.10); border-color: rgba(129,140,248,0.25); color: var(--accent); }

/* ====== 消息 ====== */
.msg-row { display: flex; gap: 12px; margin-bottom: 28px; max-width: 900px; }
.msg-user { justify-content: flex-end; margin-left: auto; }
.msg-avatar { width: 32px; height: 32px; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; flex-shrink: 0; }
.avatar-user { background: linear-gradient(135deg, #6366f1, #818cf8); color: #fff; width: 32px; height: 32px; border-radius: 6px; display: flex; align-items: center; justify-content: center; }
.avatar-ai { background: rgba(129,140,248,0.12); color: var(--accent); width: 32px; height: 32px; border-radius: 6px; display: flex; align-items: center; justify-content: center; }
.msg-body { flex: 1; min-width: 0; }
.bubble-user { background: rgba(129,140,248,0.10); color: #e5e7eb; padding: 10px 16px; border-radius: 12px 12px 4px 12px; display: inline-block; max-width: 85%; font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
.ai-explain { font-size: 14px; color: var(--text); line-height: 1.7; margin-bottom: 12px; }
.streaming-text { font-size: 14px; color: var(--text-dim); line-height: 1.7; }

/* ====== 代码块 (DeepSeek 标志性) ====== */
.code-section { background: var(--bg-code); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; margin-top: 8px; }
.code-header { display: flex; align-items: center; padding: 8px 14px; border-bottom: 1px solid var(--border); gap: 8px; }
.code-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--accent); opacity: 0.6; }
.code-lang-badge { font-size: 12px; color: var(--text-dim); text-transform: uppercase; letter-spacing: 0.5px; }
.code-actions { margin-left: auto; display: flex; gap: 4px; }
.code-actions button { background: transparent; color: var(--text-dim); border: none; padding: 4px 8px; border-radius: 4px; cursor: pointer; font-size: 13px; transition: all .15s; }
.code-actions button:hover { background: rgba(255,255,255,0.04); color: var(--text); }
.code-content { padding: 16px; margin: 0; overflow-x: auto; font-family: 'JetBrains Mono','Fira Code','Consolas',monospace; font-size: 13px; line-height: 1.65; color: #c9d1d9; white-space: pre; tab-size: 4; }
.code-content .tk-kw { color: #79c0ff; }
.code-content .tk-str { color: #f0a862; }
.code-content .tk-cm { color: #6a9955; font-style: italic; }
.code-content .tk-num { color: #79c0ff; }
.code-content .tk-at { color: #d2a8ff; }

/* ====== 输入区 ====== */
.ds-input-bar { padding: 16px 24px; border-top: 1px solid var(--border); background: var(--bg-sidebar); flex-shrink: 0; }
.input-wrapper { display: flex; align-items: flex-end; gap: 10px; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 10px 16px; transition: border-color .2s; }
.input-wrapper:focus-within { border-color: rgba(129,140,248,0.35); }
.input-actions-left, .input-actions-right { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.btn-input-tool { width: 30px; height: 30px; background: transparent; color: var(--text-dim); border: none; border-radius: 6px; cursor: pointer; font-size: 14px; display: flex; align-items: center; justify-content: center; transition: all .15s; }
.btn-input-tool:hover { background: rgba(255,255,255,0.04); color: var(--text); }
.ds-textarea { flex: 1; background: transparent; border: none; color: var(--text); font-size: 14px; line-height: 1.5; resize: none; outline: none; font-family: inherit; min-height: 24px; max-height: 200px; }
.ds-textarea::placeholder { color: var(--text-dim); }
.input-hint { font-size: 11px; color: var(--text-dim); margin-right: 8px; }
.btn-send { width: 34px; height: 34px; background: linear-gradient(135deg, var(--accent2), #8b5cf6); color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 16px; display: flex; align-items: center; justify-content: center; transition: all .2s; flex-shrink: 0; }
.btn-send:hover { opacity: 0.9; transform: scale(1.02); }
.btn-send:disabled { opacity: 0.3; cursor: default; transform: none; }
.btn-stop { width: 34px; height: 34px; background: rgba(239,68,68,0.2); color: #ef4444; border: 1px solid rgba(239,68,68,0.3); border-radius: 6px; cursor: pointer; font-size: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.cursor-blink { animation: blink 1s infinite; color: var(--accent); } @keyframes blink { 50% { opacity: 0; } }

/* ====== 右侧参数面板 ====== */
.ds-params { width: 260px; min-width: 260px; background: var(--bg-sidebar); border-left: 1px solid var(--border); padding: 16px; overflow-y: auto; }
.params-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; font-size: 14px; font-weight: 500; }
.params-header button { background: transparent; color: var(--text-dim); border: none; font-size: 18px; cursor: pointer; }
.param-group { margin-bottom: 18px; }
.param-group label { display: block; font-size: 13px; color: var(--text-dim); margin-bottom: 6px; }
.param-val { float: right; color: var(--accent); font-size: 12px; }
.ds-slider { width: 100%; height: 4px; -webkit-appearance: none; background: rgba(255,255,255,0.08); border-radius: 2px; outline: none; }
.ds-slider::-webkit-slider-thumb { -webkit-appearance: none; width: 14px; height: 14px; border-radius: 50%; background: var(--accent); cursor: pointer; }
.param-toggle { display: flex; align-items: center; gap: 8px; font-size: 13px !important; color: var(--text) !important; cursor: pointer; }
.param-toggle input { accent-color: var(--accent); }
.params-toggle { position: fixed; right: 16px; top: 50%; transform: translateY(-50%); width: 36px; height: 36px; background: var(--bg-card); color: var(--text-dim); border: 1px solid var(--border); border-radius: var(--radius); cursor: pointer; font-size: 16px; display: flex; align-items: center; justify-content: center; z-index: 100; }

/* ====== 预览/编辑浮窗 ====== */
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.overlay-panel { width: 90vw; height: 85vh; background: var(--bg-main); border: 1px solid var(--border); border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }
.overlay-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid var(--border); }
.overlay-header button { background: transparent; color: var(--text-dim); border: none; font-size: 20px; cursor: pointer; }
.overlay-code { flex: 1; overflow: auto; padding: 16px; background: var(--bg-code); }
.overlay-code pre { margin: 0; font-family: 'JetBrains Mono',monospace; font-size: 13px; line-height: 1.6; color: #c9d1d9; white-space: pre-wrap; }
.overlay-preview { flex: 1; display: flex; flex-direction: column; background: #fff; }
.edit-overlay-bar { display: flex; align-items: center; gap: 8px; padding: 6px 16px; background: #fef3c7; color: #92400e; font-size: 13px; border-bottom: 1px solid #fcd34d; flex-shrink: 0; }
.btn-clear-sel { margin-left: auto; background: transparent; color: #92400e; border: 1px solid #fcd34d; border-radius: 4px; padding: 2px 10px; font-size: 12px; cursor: pointer; }
.preview-iframe { flex: 1; border: none; }
.modify-bar { display: flex; gap: 8px; padding: 10px 16px; background: #fafafa; border-top: 1px solid #e5e7eb; flex-shrink: 0; }
.modify-input { flex: 1; padding: 8px 12px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 13px; outline: none; }
.modify-input:focus { border-color: var(--accent); }
.btn-modify-send { padding: 8px 16px; background: var(--accent2); color: #fff; border: none; border-radius: 6px; font-size: 13px; cursor: pointer; }
.btn-modify-send:disabled { opacity: 0.5; cursor: default; }
</style>
