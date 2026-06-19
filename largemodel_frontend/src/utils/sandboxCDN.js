/**
 * 将 Vue 多文件项目转换为使用 CDN 的独立 HTML 文件。
 * 在 HTML 中内联编译 Vue SFC，只需 Vue 3 CDN（无需 vue3-sfc-loader）。
 * 绕过 Sandpack vite-vue 模板中 Vite/Rollup/esbuild 原生二进制问题。
 */

/**
 * 解析 Vue SFC，提取 template / script / style
 */
function parseSFC(code) {
  const tplM = code.match(/<template>([\s\S]*?)<\/template>/i)
  const scrM = code.match(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/i)
  const styM = code.match(/<style[^>]*?>([\s\S]*?)<\/style>/i)

  return {
    template: tplM ? tplM[1].trim() : '',
    script: scrM ? scrM[1].trim() : '',
    isSetup: scrM ? /\bsetup\b/.test(scrM[0]) : false,
    style: styM ? styM[1].trim() : ''
  }
}

/**
 * 检测 .vue 文件中的组件导入
 * 返回 { localName: componentName }，如 { Calculator: 'Calculator' }
 */
function detectComponentImports(code) {
  const imports = {}
  const re = /import\s+(\w+)\s+from\s*['"](.+?)['"]\s*;?/g
  let m
  while ((m = re.exec(code)) !== null) {
    const [, name, path] = m
    if (path.endsWith('.vue')) {
      const filename = path.split('/').pop().replace(/\.vue$/, '')
      imports[name] = filename
    }
  }
  return imports
}

/**
 * JS 保留字和 Vue API（不需要从 setup 返回）
 */
const SKIP_NAMES = new Set([
  'const', 'let', 'var', 'if', 'else', 'for', 'while', 'do', 'return',
  'function', 'class', 'new', 'typeof', 'instanceof', 'in', 'of',
  'true', 'false', 'null', 'undefined', 'this', 'super', 'import',
  'export', 'default', 'async', 'await', 'yield', 'switch', 'case',
  'break', 'continue', 'try', 'catch', 'finally', 'throw', 'delete',
  'void', 'debugger', 'with', 'require', 'console',
  'ref', 'reactive', 'computed', 'watch', 'watchEffect',
  'onMounted', 'onUnmounted', 'onBeforeMount', 'onBeforeUnmount',
  'nextTick', 'provide', 'inject', 'toRef', 'toRefs', 'isRef', 'unref',
  'defineComponent', 'defineAsyncComponent', 'h', 'markRaw', 'toRaw',
  'Transition', 'TransitionGroup', 'KeepAlive', 'Teleport', 'Suspense'
])

/**
 * 将 <script setup> 代码转为 setup() 函数体
 */
function convertSetupScript(code) {
  let body = code

  // 移除 import 语句
  body = body.replace(/import\s*\{[\s\S]*?\}\s*from\s*['"][\s\S]*?['"]\s*;?/g, '')
  body = body.replace(/import\s+\w+\s+from\s*['"][\s\S]*?['"]\s*;?/g, '')
  body = body.replace(/import\s+['"][\s\S]*?['"]\s*;?/g, '')

  // 移除 defineProps / defineEmits / withDefaults
  body = body.replace(/(?:const|let|var)\s+\w+\s*=\s*defineProps\s*<[\s\S]*?>\s*\([^)]*\)\s*;?/g, '')
  body = body.replace(/(?:const|let|var)\s+\w+\s*=\s*defineEmits\s*<[\s\S]*?>\s*\([^)]*\)\s*;?/g, '')
  body = body.replace(/withDefaults\s*\(/g, '(')

  body = body.trim()
  if (!body) return body

  // 检测需要返回的变量名
  const names = new Set()
  const lines = body.split('\n')

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue

    const funcMatch = trimmed.match(/^(?:async\s+)?function\s+(\w+)/)
    if (funcMatch) {
      if (!SKIP_NAMES.has(funcMatch[1])) names.add(funcMatch[1])
      continue
    }

    if (!/^(?:const|let|var)\b/.test(trimmed)) continue

    const afterKeyword = trimmed.replace(/^(?:const|let|var)\s+/, '')
    const parts = afterKeyword.split(',')

    for (const part of parts) {
      const p = part.trim()
      if (p.startsWith('{')) {
        const innerRe = /\b([a-zA-Z_$][\w$]*)\b/g
        let im
        while ((im = innerRe.exec(p)) !== null) {
          if (!SKIP_NAMES.has(im[1])) names.add(im[1])
        }
        continue
      }
      if (p.startsWith('[')) continue
      const idMatch = p.match(/^([a-zA-Z_$][\w$]*)/)
      if (idMatch && !SKIP_NAMES.has(idMatch[1])) {
        names.add(idMatch[1])
      }
    }
  }

  if (names.size > 0) {
    body += '\n  return { ' + [...names].sort().join(', ') + ' }'
  }
  return body
}

/**
 * 构建使用 Vue 3 CDN 的独立 HTML 文件
 * 内联编译所有 .vue 文件，无需额外构建工具
 */
export function buildVueCDNHtml(filesMap, options = {}) {
  const { editMode = false } = options
  const entries = Object.entries(filesMap)
  if (entries.length === 0) return null

  // 1. 解析所有 .vue 文件
  const components = {}
  const allStyles = []

  for (const [path, content] of entries) {
    const code = typeof content === 'string' ? content : (content.code || '')
    if (!code) continue

    const filename = path.split('/').pop()
    if (filename.endsWith('.vue')) {
      const parsed = parseSFC(code)
      const name = filename.replace(/\.vue$/, '')
      components[name] = { ...parsed, path }
      if (parsed.style) {
        allStyles.push('/* ' + filename + ' */\n' + parsed.style)
      }
    }
    if (filename.endsWith('.css')) {
      allStyles.push('/* ' + filename + ' */\n' + code)
    }
  }

  // 2. 必须有 App.vue
  if (!components['App']) return null

  // 3. 拓扑排序（App 最后定义，依赖在前）
  const visited = new Set()
  const ordered = []

  function visit(name) {
    if (visited.has(name) || !components[name]) return
    visited.add(name)
    const comp = components[name]
    const compImports = detectComponentImports(comp.script)
    for (const importedName of Object.values(compImports)) {
      visit(importedName)
    }
    ordered.push(name)
  }
  visit('App')
  for (const name of Object.keys(components)) { visit(name) }

  // 4. 生成每个组件的 JavaScript 定义
  const componentDefs = []

  for (const name of ordered) {
    const comp = components[name]

    let setupBody = ''
    if (comp.isSetup && comp.script) {
      setupBody = convertSetupScript(comp.script)
    }

    const compImports = detectComponentImports(comp.script)
    const childNames = Object.values(compImports).filter(n => components[n])

    let def = 'const ' + name + ' = {\n'
    def += '  template: ' + JSON.stringify(comp.template)

    if (setupBody) {
      def += ',\n  setup() {\n' + setupBody + '\n  }'
    }

    if (childNames.length > 0) {
      const childObj = childNames.join(', ')
      def += ',\n  components: { ' + childObj + ' }'
    }

    def += '\n}'
    componentDefs.push(def)
  }

  // 5. 检测依赖
  const allCode = Object.values(components).map(c => c.script + c.template).join(' ')
  const hasElementPlus = /element-plus|elementPlus|ElMessage|ElButton|el-button|ElIcon/i.test(allCode)

  // 6. 构建 HTML
  const lines = []

  lines.push('<!DOCTYPE html>')
  lines.push('<html lang="zh-CN">')
  lines.push('<head>')
  lines.push('  <meta charset="UTF-8">')
  lines.push('  <meta name="viewport" content="width=device-width, initial-scale=1.0">')
  lines.push('  <title>项目预览</title>')
  lines.push('  <script src="https://unpkg.com/vue@3/dist/vue.global.prod.js"></scr' + 'ipt>')

  if (hasElementPlus) {
    lines.push('  <script src="https://unpkg.com/element-plus/dist/index.full.min.js"></scr' + 'ipt>')
    lines.push('  <link rel="stylesheet" href="https://unpkg.com/element-plus/dist/index.css">')
  }

  lines.push('  <style>')
  lines.push('    * { box-sizing: border-box; margin: 0; padding: 0; }')
  lines.push('    body { font-family: system-ui, -apple-system, sans-serif; padding: 16px; background: #f5f5f5; min-height: 100vh; }')
  lines.push('    #app { max-width: 1200px; margin: 0 auto; min-height: 200px; }')
  lines.push('    #__err { position: fixed; top: 0; left: 0; right: 0; background: #f56c6c; color: #fff; padding: 12px 16px; font-size: 13px; white-space: pre-wrap; z-index: 9999; display: none; font-family: monospace; max-height: 40vh; overflow-y: auto; }')
  if (allStyles.length > 0) {
    lines.push('    /* === 组件样式 === */')
    lines.push(allStyles.join('\n'))
  }
  lines.push('  </style>')
  lines.push('</head>')
  lines.push('<body>')
  lines.push('  <div id="app"></div>')
  lines.push('  <div id="__err"></div>')
  lines.push('  <script>')
  lines.push('    (function() {')
  lines.push('      function showErr(e) {')
  lines.push('        var el = document.getElementById("__err");')
  lines.push('        if (el) {')
  lines.push('          el.style.display = "block";')
  lines.push('          el.textContent = "预览错误: " + (e.message || String(e));')
  lines.push('        }')
  lines.push('        console.error(e);')
  lines.push('        try { window.parent.postMessage({ type: "preview-error", payload: { message: e.message || String(e), stack: e.stack || "" } }, "*"); } catch(_) {}')
  lines.push('      }')
  lines.push('')
  lines.push('      try {')
  lines.push('        if (typeof Vue === "undefined") throw new Error("Vue 3 CDN 加载失败");')
  lines.push('        var { createApp, ref, reactive, computed, watch, watchEffect,')
  lines.push('          onMounted, onUnmounted, onBeforeMount, onBeforeUnmount,')
  lines.push('          nextTick, provide, inject, toRef, toRefs, isRef, unref,')
  lines.push('          defineComponent, h } = Vue;')

  if (hasElementPlus) {
    lines.push('        var ElementPlus = window.ElementPlus;')
  }

  lines.push('')
  for (const def of componentDefs) {
    lines.push('        ' + def.replace(/\n/g, '\n        '))
  }
  lines.push('')
  lines.push('        var app = createApp(App);')

  if (hasElementPlus) {
    lines.push('        app.use(ElementPlus);')
  }

  lines.push('        app.config.errorHandler = function(err) { showErr(err); };')
  lines.push('        app.mount("#app");')
  lines.push('      } catch(e) {')
  lines.push('        showErr(e);')
  lines.push('      }')
  lines.push('    })();')
  lines.push('  </scr' + 'ipt>')

  // ===== 编辑模式：元素选择器 =====
  if (editMode) {
    lines.push('  <script>')
    lines.push('    (function() {')
    lines.push('      var ov = document.createElement("div");')
    lines.push('      ov.id = "__picker";')
    lines.push('      ov.style.cssText = "position:fixed;pointer-events:none;border:2px solid #f56c6c;background:rgba(245,108,108,.08);z-index:99999;display:none;border-radius:2px;transition:all .1s";')
    lines.push('      document.body.appendChild(ov);')
    lines.push('      document.addEventListener("mouseover", function(e) {')
    lines.push('        var t = e.target;')
    lines.push('        if (t === ov || t.id === "__picker" || t.id === "__err" || t.closest("#__err")) return;')
    lines.push('        var r = t.getBoundingClientRect();')
    lines.push('        ov.style.display = "block";')
    lines.push('        ov.style.top = r.top + "px";')
    lines.push('        ov.style.left = r.left + "px";')
    lines.push('        ov.style.width = r.width + "px";')
    lines.push('        ov.style.height = r.height + "px";')
    lines.push('      }, true);')
    lines.push('      document.addEventListener("mouseout", function(e) {')
    lines.push('        var t = e.target;')
    lines.push('        if (t === ov || t.id === "__picker") return;')
    lines.push('        ov.style.display = "none";')
    lines.push('      }, true);')
    lines.push('      document.addEventListener("click", function(e) {')
    lines.push('        e.preventDefault();')
    lines.push('        e.stopPropagation();')
    lines.push('        var t = e.target;')
    lines.push('        if (t === ov || t.id === "__picker" || t.id === "__err" || t.closest("#__err")) return;')
    lines.push('        ov.style.borderColor = "#67c23a";')
    lines.push('        ov.style.background = "rgba(103,194,58,.1)";')
    lines.push('        var info = {')
    lines.push('          tag: t.tagName ? t.tagName.toLowerCase() : "",')
    lines.push('          id: t.id || "",')
    lines.push('          cls: (t.className || "").toString(),')
    lines.push('          text: (t.textContent || "").substring(0, 80),')
    lines.push('          html: (t.outerHTML || "").substring(0, 500)')
    lines.push('        };')
    lines.push('        window.parent.postMessage({ type: "element-selected", payload: info }, "*");')
    lines.push('      }, true);')
    lines.push('    })();')
    lines.push('  </scr' + 'ipt>')
  }

  lines.push('</body>')
  lines.push('</html>')

  return lines.join('\n')
}
