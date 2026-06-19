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

// ===== 项目修改 =====

/** 将文件映射格式化为 AI 友好的字符串 */
function formatFilesForAI(filesMap) {
  if (!filesMap || Object.keys(filesMap).length === 0) return ''
  const parts = []
  for (const [path, content] of Object.entries(filesMap)) {
    const code = typeof content === 'string' ? content : (content.code || '')
    if (!code) continue
    parts.push('// ===== ' + path + ' =====\n' + code)
  }
  return parts.join('\n\n')
}

/** 解析 AI 返回的多文件代码 */
function parseAiFiles(raw) {
  const filesMap = {}
  const sepRe = /\/\/\s*={3,}\s*(.+?)\s*={3,}\s*$/gm
  const parts = []
  let lastEnd = 0
  let sm
  while ((sm = sepRe.exec(raw)) !== null) {
    if (sm.index > lastEnd) {
      parts.push({ name: null, code: raw.substring(lastEnd, sm.index).trim() })
    }
    parts.push({ name: sm[1].trim(), code: null })
    lastEnd = sm.index + sm[0].length
  }
  if (lastEnd < raw.length) {
    parts.push({ name: null, code: raw.substring(lastEnd).trim() })
  }
  for (let i = 0; i < parts.length; i++) {
    if (parts[i].name && i + 1 < parts.length && parts[i + 1].name === null) {
      filesMap[parts[i].name] = parts[i + 1].code
      i++
    } else if (parts[i].name === null && parts[i].code && Object.keys(filesMap).length === 0) {
      // 单文件修改无标记
      filesMap[''] = parts[i].code
    }
  }
  return filesMap
}

/**
 * 流式修改项目文件（AI 对话修改）
 */
export function modifyProjectStream(projectId, context, { onToken, onDone, onError, signal }) {
  const currentCode = formatFilesForAI(context.files)

  let fullPrompt = context.modifyPrompt
  if (context.elementInfo) {
    fullPrompt = '[修改元素] ' + context.elementInfo + '\n[修改要求] ' + context.modifyPrompt + '\n\n请只修改相关文件的代码，使用 // ===== 文件路径 ===== 标记每个文件，保持其他文件不变。'
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
