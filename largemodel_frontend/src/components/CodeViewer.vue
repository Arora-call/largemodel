<template>
  <div class="code-viewer" :style="{ height: height, maxHeight: maxHeight || 'none' }">
    <div ref="editorContainer" class="editor-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import * as monaco from 'monaco-editor'
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'

// 配置 Monaco 环境以使用 Vite worker imports
self.MonacoEnvironment = {
  getWorker(_, label) {
    if (label === 'css' || label === 'scss' || label === 'less') return new cssWorker()
    if (label === 'html' || label === 'handlebars' || label === 'razor') return new htmlWorker()
    if (label === 'json') return new jsonWorker()
    if (label === 'typescript' || label === 'javascript') return new tsWorker()
    return new editorWorker()
  }
}

const props = defineProps({
  code: { type: String, default: '' },
  language: { type: String, default: 'text' },
  height: { type: String, default: '400px' },
  maxHeight: { type: String, default: undefined }
})

// 语言标识映射：项目语言 → Monaco 语言 ID
const LANGUAGE_MAP = {
  vue: 'html',
  java: 'java',
  python: 'python',
  html: 'html',
  javascript: 'javascript',
  typescript: 'typescript',
  js: 'javascript',
  ts: 'typescript',
  c: 'cpp',
  cpp: 'cpp',
  css: 'css',
  json: 'json',
  xml: 'xml',
  sql: 'sql',
  yaml: 'yaml',
  text: 'plaintext'
}

const editorContainer = ref(null)
let editor = null

onMounted(() => {
  if (editorContainer.value) {
    const lang = LANGUAGE_MAP[props.language?.toLowerCase()] || 'plaintext'
    editor = monaco.editor.create(editorContainer.value, {
      value: props.code || '',
      language: lang,
      readOnly: true,
      domReadOnly: true,
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      lineNumbers: 'on',
      automaticLayout: true,
      theme: 'vs-dark',
      fontSize: 13,
      fontFamily: "'Cascadia Code', 'JetBrains Mono', 'Fira Code', 'Consolas', 'Courier New', monospace",
      lineHeight: 22,
      padding: { top: 12, bottom: 12 },
      folding: true,
      renderLineHighlight: 'none',
      overviewRulerLanes: 0,
      hideCursorInOverviewRuler: true,
      occurrencesHighlight: 'off',
      selectionHighlight: false,
      matchBrackets: 'never',
      renderWhitespace: 'none',
      contextmenu: false,
      glyphMargin: false
    })
  }
})

// 监听代码内容变化
watch(() => props.code, (newCode) => {
  if (editor) {
    const currentValue = editor.getValue()
    if (currentValue !== newCode) {
      editor.setValue(newCode || '')
    }
  }
})

// 监听语言变化
watch(() => props.language, (newLang) => {
  if (editor) {
    const model = editor.getModel()
    if (model) {
      const lang = LANGUAGE_MAP[newLang?.toLowerCase()] || 'plaintext'
      monaco.editor.setModelLanguage(model, lang)
    }
  }
})

onBeforeUnmount(() => {
  if (editor) {
    editor.dispose()
    editor = null
  }
})
</script>

<style scoped>
.code-viewer {
  border-radius: 8px;
  overflow: hidden;
  min-height: 80px;
}

.editor-container {
  width: 100%;
  height: 100%;
}
</style>
