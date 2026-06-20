/**
 * 工程项目 API
 */
import request from './request'
import { modifyCodeStream } from './ai'

const API_BASE = '/api'

/** 流式生成工程项目（SSE） */
export function generateProject(data, { onToken, onDone, onError, signal }) {
  const token = localStorage.getItem('token')
  return fetch(API_BASE + '/projects/generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(data),
    signal
  }).then(async (response) => {
    if (!response.ok) {
      const errData = await response.json().catch(() => ({}))
      throw new Error(errData.message || `请求失败: ${response.status}`)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split('\n\n')
      buffer = blocks.pop() || ''
      for (const block of blocks) {
        const eventMatch = block.match(/^event:\s*(\w+)/m)
        const dataLines = []; const re = /^data:\s?(.*)$/gm; let dm
        while ((dm = re.exec(block)) !== null) dataLines.push(dm[1])
        if (!dataLines.length) continue
        const payload = dataLines.join('\n')
        const evt = eventMatch ? eventMatch[1] : 'message'
        if (evt === 'token') onToken?.(payload)
        else if (evt === 'done') onDone?.(payload)
        else if (evt === 'error') onError?.(new Error(payload))
      }
    }
  })
}

/** 获取项目文件树 */
export function getProjectTree(projectId) {
  return request.get(`/projects/${projectId}/tree`)
}

/** 读取项目中的单个文件 */
export function getProjectFile(projectId, filePath) {
  return request.get(`/projects/${projectId}/file`, { params: { path: filePath } })
}

/** 下载项目 ZIP（使用 fetch + Authorization header，避免 anchor 跳转的认证问题） */
export async function downloadProjectZip(projectId) {
  const token = localStorage.getItem('token')
  try {
    const resp = await fetch(`${API_BASE}/projects/${projectId}/download`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}))
      throw new Error(err.message || `下载失败: ${resp.status}`)
    }
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `project_${projectId}.zip`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    // 兜底：用 anchor 方式重试
    const a = document.createElement('a')
    a.href = `${API_BASE}/projects/${projectId}/download?token=${token}`
    a.download = `project_${projectId}.zip`
    a.click()
  }
}

/** 保存项目 */
export function saveProject(projectId, name, type) {
  return request.post(`/projects/${projectId}/save`, null, { params: { name, type } })
}

/** AI 修改后保存项目文件到磁盘 */
export function saveProjectFiles(projectId, filesMap) {
  return request.post(`/projects/${projectId}/files`, filesMap)
}

// ===== 项目修改 =====

/** UI 相关文件扩展名（修改请求只发这些给 AI） */
const UI_FILE_EXTS = new Set(['vue', 'html', 'htm', 'css', 'scss', 'less', 'js', 'ts', 'jsx', 'tsx'])

/** 配置文件路径模式（绝对不发给 AI 修改） */
const CONFIG_PATTERNS = [
  /vite\.config/i, /package\.json/i, /tsconfig/i, /jsconfig/i,
  /\.eslintrc/i, /\.prettierrc/i, /postcss\.config/i, /tailwind\.config/i,
  /README/i, /\.gitignore/i, /\.env/i, /\.editorconfig/i
]

function isConfigFile(path) {
  return CONFIG_PATTERNS.some(p => p.test(path))
}

function isUiFile(path) {
  const ext = path.split('.').pop()?.toLowerCase()
  return UI_FILE_EXTS.has(ext)
}

/** 将文件映射格式化为 AI 友好的字符串（分层：target 文件在前，config 文件过滤掉） */
function formatFilesForAI(filesMap, targetPath) {
  if (!filesMap || Object.keys(filesMap).length === 0) return ''

  const targetParts = []    // [TARGET FILE] — 优先修改
  const contextParts = []   // [CONTEXT ONLY] — 仅供了解结构，不要修改
  const entries = Object.entries(filesMap)

  for (const [path, content] of entries) {
    const code = typeof content === 'string' ? content : (content.code || '')
    if (!code) continue
    // 过滤配置文件
    if (isConfigFile(path)) continue
    // 过滤非 UI 文件（Python/Java 等后端文件）
    if (!isUiFile(path)) continue

    if (path === targetPath || (targetPath && path.endsWith('/' + targetPath)) || (targetPath && path.endsWith(targetPath))) {
      targetParts.push('// [TARGET FILE] ' + path + ' ← 请主要修改此文件\n// ===== ' + path + ' =====\n' + code)
    } else {
      contextParts.push('// [CONTEXT ONLY — DO NOT MODIFY] ' + path + '\n// ===== ' + path + ' =====\n' + code)
    }
  }

  // target 文件在最前面，context 文件在后面
  return [...targetParts, ...contextParts].join('\n\n')
}

/** 解析 AI 返回的多文件代码（兼容 extractCode 输出 + 原始 AI 输出） */
function parseAiFiles(raw) {
  const filesMap = {}

  // 方式1: // ===== path ===== 分隔符（extractCode 标准输出）
  const sepRe = /\/\/\s*={3,}\s*(.+?)\s*={3,}\s*$/gm
  const parts = []
  let lastEnd = 0
  let sm
  while ((sm = sepRe.exec(raw)) !== null) {
    if (sm.index > lastEnd) parts.push({ name: null, code: raw.substring(lastEnd, sm.index).trim() })
    parts.push({ name: sm[1].trim(), code: null })
    lastEnd = sm.index + sm[0].length
  }
  if (lastEnd < raw.length) parts.push({ name: null, code: raw.substring(lastEnd).trim() })

  for (let i = 0; i < parts.length; i++) {
    if (parts[i].name && i + 1 < parts.length && parts[i + 1].name === null) {
      filesMap[parts[i].name] = parts[i + 1].code
      i++
    } else if (parts[i].name === null && parts[i].code && Object.keys(filesMap).length === 0) {
      filesMap[''] = parts[i].code
    }
  }

  // 方式2: 原始 AI 输出中的 ``` 代码块 + // File: 内标记（extractCode 失败时的兜底）
  if (Object.keys(filesMap).length === 0 || (Object.keys(filesMap).length === 1 && filesMap[''])) {
    const blockRe = /```(\w*)\s*[\r\n]*([\s\S]*?)```/g
    let bm
    while ((bm = blockRe.exec(raw)) !== null) {
      let code = bm[2].trim()
      if (!code) continue
      // 检查代码块内是否有 // File: 标记
      const fileLine = code.match(/^\s*\/\/\s*File:\s*(.+)/m)
      const path = fileLine ? fileLine[1].trim() : ''
      if (fileLine) {
        code = code.replace(/^\s*\/\/\s*File:[^\n]*\n?/, '').trim()
      }
      if (path && code) {
        filesMap[path] = code
      }
    }
  }

  // 清理空 key（单文件兜底产生的）
  if (filesMap['']) delete filesMap['']

  return filesMap
}

/**
 * 流式修改项目文件（AI 对话修改）
 */
export function modifyProjectStream(projectId, context, { onToken, onDone, onError, signal }) {
  const currentCode = formatFilesForAI(context.files, context.targetPath)

  let fullPrompt = context.modifyPrompt
  if (context.elementInfo) {
    fullPrompt = '[修改元素] ' + context.elementInfo + '\n[修改要求] ' + context.modifyPrompt
      + '\n\n⚠️ 请只修改标记为 [TARGET FILE] 的文件。标记为 [CONTEXT ONLY — DO NOT MODIFY] 的文件绝对不能修改！'
      + '\n输出每个被修改的文件时使用格式：```语言\\n// File: 文件路径\\n完整代码\\n```'
  }

  return modifyCodeStream(
    { currentCode, elementInfo: context.elementInfo || '', modifyPrompt: fullPrompt, conversationId: context.conversationId || null },
    {
      signal,
      onToken: (token) => onToken?.(token),
      onDone: (data) => {
        let result
        try { result = JSON.parse(data) } catch { result = { code: data } }
        const modifiedCode = result.code || data || ''
        const updatedFiles = parseAiFiles(modifiedCode)
        onDone?.({ files: updatedFiles, code: modifiedCode, raw: modifiedCode, conversationId: result.conversationId })
      },
      onError: (err) => onError?.(err)
    }
  )
}
