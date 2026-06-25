import request from './request'

export function saveApplication(data) {
  return request.post('/applications', data)
}

export function listApplications(params) {
  return request.get('/applications', { params })
}

export function getApplication(id) {
  return request.get(`/applications/${id}`)
}

export function updateApplication(id, data) {
  return request.put(`/applications/${id}`, data)
}

export function deleteApplication(id) {
  return request.delete(`/applications/${id}`)
}

export function downloadUrl(id) {
  return `/api/applications/${id}/download`
}

/**
 * 下载应用代码（带认证的 blob 下载）
 * @param {number} id 应用ID
 * @param {string} fallbackName 无 Content-Disposition 时的备选文件名
 */
export async function downloadApplication(id, fallbackName = 'code.zip') {
  const token = localStorage.getItem('token')
  const resp = await fetch(`/api/applications/${id}/download`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!resp.ok) {
    const err = await resp.text().catch(() => '下载失败')
    throw new Error(err || `HTTP ${resp.status}`)
  }
  // 从响应头提取文件名
  const disposition = resp.headers.get('Content-Disposition')
  let filename = fallbackName
  if (disposition) {
    const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
    if (match) filename = match[1].replace(/['"]/g, '')
  }
  const blob = await resp.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

/* 对话 */
export function listConversations(params) {
  return request.get('/conversations', { params })
}

export function getMessages(convId) {
  return request.get(`/conversations/${convId}/messages`)
}

export function deleteConversation(convId) {
  return request.delete(`/conversations/${convId}`)
}

export function clearConversations() {
  return request.delete('/conversations/clear')
}

export function deleteProject(id) {
  return request.delete(`/projects/${id}`)
}

export function getDashboardStats() {
  return request.get('/dashboard/stats')
}

/** 导出对话为 Markdown */
export async function exportConversation(convId) {
  const token = localStorage.getItem('token')
  const resp = await fetch(`/api/conversations/${convId}/export`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!resp.ok) throw new Error('导出失败')
  return resp.text()
}

/** 设置应用优先级: 0-默认, 99-精选, 999-置顶 */
export function setAppPriority(id, priority) {
  return request.put(`/applications/${id}/priority`, { priority })
}

/* ── 管理员：应用管理 ── */
export function listAllApplications(params) {
  return request.get('/admin/applications', { params })
}

export function adminUpdateApplication(id, data) {
  return request.put(`/admin/applications/${id}`, data)
}

export function adminDeleteApplication(id) {
  return request.delete(`/admin/applications/${id}`)
}
