/**
 * 工程项目 API
 */
import request from './request'

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

/** 下载项目 ZIP */
export function downloadProjectZip(projectId) {
  const token = localStorage.getItem('token')
  const a = document.createElement('a')
  a.href = `${API_BASE}/projects/${projectId}/download?token=${token}`
  a.download = `project_${projectId}.zip`
  a.click()
}

/** 保存项目 */
export function saveProject(projectId, name, type) {
  return request.post(`/projects/${projectId}/save`, null, { params: { name, type } })
}
