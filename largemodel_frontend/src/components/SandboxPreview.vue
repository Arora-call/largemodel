<template>
  <div class="sandbox-preview" :style="{ height }">
    <!-- CDN 模式：使用 iframe 绕过 Sandpack 的 Vite 原生二进制问题 -->
    <div v-if="hasFiles && shouldUseCDN && cdnHtml" class="cdn-iframe-container">
      <iframe
        :key="'cdn-' + sandpackKey"
        :srcdoc="cdnHtml"
        sandbox="allow-scripts allow-same-origin"
        class="preview-iframe"
      ></iframe>
    </div>
    <!-- Sandpack 模式 -->
    <div v-else-if="hasFiles && !shouldUseCDN" class="sandbox-container">
      <Sandpack
        :key="sandpackKey"
        :template="detectedTemplate"
        :files="normalizedFiles"
        :customSetup="mergedSetup"
        theme="dark"
        :options="sandpackOptions"
      />
    </div>
    <!-- 空状态 -->
    <div v-else class="sandbox-empty">
      <div class="empty-icon">📦</div>
      <p>暂无文件可预览</p>
      <span class="empty-hint">AI 生成代码后将在此处展示运行效果</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { Sandpack } from 'sandpack-vue3'
import { buildVueCDNHtml } from '@/utils/sandboxCDN'

const props = defineProps({
  /** 文件映射: { 'src/App.vue': 'code...', 'src/main.js': 'code...' } */
  files: {
    type: Object,
    default: () => ({})
  },
  /** 手动指定模板，为空时自动检测 */
  template: {
    type: String,
    default: ''
  },
  /** 额外的依赖设置 */
  customSetup: {
    type: Object,
    default: () => ({})
  },
  /** 是否只读 */
  readOnly: {
    type: Boolean,
    default: true
  },
  /** 是否显示代码编辑器 */
  showEditor: {
    type: Boolean,
    default: true
  },
  /** 容器高度 */
  height: {
    type: String,
    default: '100%'
  },
  /** 是否使用 CDN 模式（Vue 项目推荐开启，绕过 Vite 原生二进制问题） */
  useCDN: {
    type: Boolean,
    default: true
  },
  /** 编辑模式：启用元素选择器 */
  editMode: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['element-selected', 'preview-error'])

/** 文件内容或编辑模式变化时更换 key 强制重新挂载 */
const sandpackKey = ref(0)
watch(
  () => [JSON.stringify(props.files), props.editMode],
  () => { sandpackKey.value++ }
)

/** 监听 CDN iframe 的消息 */
function onIframeMessage(e) {
  if (e.data?.type === 'element-selected') {
    emit('element-selected', e.data.payload)
  } else if (e.data?.type === 'preview-error') {
    emit('preview-error', e.data.payload)
  }
}
onMounted(() => window.addEventListener('message', onIframeMessage))
onBeforeUnmount(() => window.removeEventListener('message', onIframeMessage))

const hasFiles = computed(() => {
  return Object.keys(props.files).length > 0
})

/** 文件扩展名集合 */
const fileExtensions = computed(() => {
  return Object.keys(normalizedFiles.value).map(p => {
    const parts = p.split('.')
    return parts.length > 1 ? parts[parts.length - 1].toLowerCase() : ''
  })
})

// ===================== CDN 模式 =====================

/** 是否应使用 CDN 模式（Vue 项目 + useCDN 开启） */
const shouldUseCDN = computed(() => {
  if (props.template) return false
  if (!props.useCDN) return false
  const exts = fileExtensions.value
  if (!exts.includes('vue')) return false
  const keys = Object.keys(normalizedFiles.value)
  return keys.some(p => /app\.vue$/i.test(p))
})

/** CDN 模式下生成的独立 HTML */
const cdnHtml = computed(() => {
  if (!shouldUseCDN.value) return null
  return buildVueCDNHtml(normalizedFiles.value, { editMode: props.editMode })
})

/**
 * 路径规范化：确保路径以 / 开头，匹配 sandpack 的文件路径格式
 * 支持: 'src/App.vue' → '/src/App.vue'
 *       'App.vue' → '/App.vue' (如果是 Vue SFC 单文件)
 *       '/src/App.vue' → '/src/App.vue' (保持)
 */
function normalizePath(p) {
  let path = p.trim()
  // 移除开头的 ./
  if (path.startsWith('./')) path = path.substring(1)
  // 确保以 / 开头
  if (!path.startsWith('/')) path = '/' + path
  // 处理 Windows 风格反斜杠
  path = path.replace(/\\/g, '/')
  return path
}

/** 规范化后的文件映射 */
const normalizedFiles = computed(() => {
  const result = {}
  for (const [path, content] of Object.entries(props.files)) {
    const normPath = normalizePath(path)
    if (typeof content === 'string') {
      result[normPath] = { code: content }
    } else {
      result[normPath] = content
    }
  }
  return result
})

/**
 * 自动检测合适的模板
 * - 有 .vue 文件 → 'vite-vue'
 * - 有 .html 文件(无 .vue) → 'vanilla'
 * - 默认 → 'vite-vue'
 */
const detectedTemplate = computed(() => {
  if (shouldUseCDN.value && cdnHtml.value) return 'static'
  if (props.template) return props.template
  const exts = fileExtensions.value
  if (exts.includes('vue')) return 'vite-vue'
  if (exts.includes('html')) return 'vanilla'
  return 'vite-vue'
})

/**
 * 扫描所有文件内容，自动检测需要的依赖
 */
function scanDependencies(filesMap) {
  const deps = {}
  const allCode = Object.values(filesMap)
    .map(f => typeof f === 'string' ? f : f.code || '')
    .join('\n')

  // 检测 element-plus
  if (/element-plus|@element-plus|element-plus\/es|elementPlus/i.test(allCode)) {
    deps['element-plus'] = 'latest'
    deps['@element-plus/icons-vue'] = 'latest'
  }
  // 检测 vue-router (非模板自带)
  if (/vue-router|createRouter|VueRouter/i.test(allCode)) {
    deps['vue-router'] = 'latest'
  }
  // 检测 pinia
  if (/pinia|createPinia|defineStore/i.test(allCode)) {
    deps['pinia'] = 'latest'
  }
  // 检测 axios
  if (/axios/i.test(allCode)) {
    deps['axios'] = 'latest'
  }
  // 检测 echarts
  if (/echarts/i.test(allCode)) {
    deps['echarts'] = 'latest'
  }

  return deps
}

/** 合并的 customSetup：用户传入的 + 自动检测的 */
const mergedSetup = computed(() => {
  const autoDeps = scanDependencies(normalizedFiles.value)
  const userSetup = props.customSetup || {}

  return {
    ...userSetup,
    dependencies: {
      ...autoDeps,
      ...(userSetup.dependencies || {})
    }
  }
})

/** Sandpack 选项 */
const sandpackOptions = computed(() => {
  const isCDN = shouldUseCDN.value && cdnHtml.value
  return {
    readOnly: props.readOnly,
    showLineNumbers: true,
    showInlineErrors: true,
    showTabs: true,
    closableTabs: true,
    wrapContent: true,
    resizablePanels: true,
    activeFile: isCDN ? '/index.html' : findActiveFile(),
    editorWidthPercentage: (isCDN || !props.showEditor) ? 0 : 50,
    ...(props.showEditor && !isCDN ? {} : { layout: 'preview' })
  }
})

/** 找到合适的默认激活文件 */
function findActiveFile() {
  const paths = Object.keys(normalizedFiles.value)
  // 优先激活 App.vue
  const appVue = paths.find(p => /app\.vue$/i.test(p))
  if (appVue) return appVue
  // 其次 main.js
  const mainJs = paths.find(p => /main\.(js|ts)$/i.test(p))
  if (mainJs) return mainJs
  // 其次 index.html
  const indexHtml = paths.find(p => /index\.html$/i.test(p))
  if (indexHtml) return indexHtml
  // 兜底：第一个文件
  return paths[0] || ''
}
</script>

<style scoped>
.sandbox-preview {
  width: 100%;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  overflow: hidden;
  background: #1a1e2a;
}

.sandbox-container {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* sandpack-vue3 内部会渲染自己的布局，我们让它铺满 */
.sandbox-container :deep(.sp-wrapper) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.sandbox-container :deep(.sp-layout) {
  flex: 1;
  min-height: 0;
}

/* 调整 sandpack 主题以匹配我们的暗色设计 */
.sandbox-container :deep(.sp-tab-button) {
  background: #141821;
}

/* CDN 模式 iframe 容器 */
.cdn-iframe-container {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.preview-iframe {
  flex: 1;
  width: 100%;
  border: none;
  min-height: 0;
}

.sandbox-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;
  color: #6b7280;
  background: #1a1e2a;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.sandbox-empty p {
  font-size: 15px;
  color: #9ca3af;
  margin: 0 0 8px;
}

.empty-hint {
  font-size: 12px;
  color: #6b7280;
}
</style>
