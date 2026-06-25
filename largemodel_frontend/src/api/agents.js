/**
 * Agent 工作流 API — Agent Service
 */
import request from './request'

// 创建工作流
export function createWorkflow(data) {
  return request.post('/agents/workflow', data)
}

// 工作流列表
export function listWorkflows(params) {
  return request.get('/agents/workflow', { params })
}

// 工作流详情/状态
export function getWorkflow(id) {
  return request.get(`/agents/workflow/${id}`)
}

// 更新工作流
export function updateWorkflow(id, data) {
  return request.put(`/agents/workflow/${id}`, data)
}

// 删除工作流
export function deleteWorkflow(id) {
  return request.delete(`/agents/workflow/${id}`)
}

// 执行工作流
export function executeWorkflow(id, params) {
  return request.post(`/agents/workflow/${id}/execute`, params)
}

// 执行工作流 (SSE 流式返回各阶段进度)
export function executeWorkflowStream(id, params, callbacks) {
  const { onPhase, onProgress, onDone, onError } = callbacks
  const token = localStorage.getItem('token')

  return fetch(`/api/agents/workflow/${id}/execute`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(params)
  }).then(async response => {
    if (!response.ok) {
      const text = await response.text().catch(() => '')
      throw new Error(text || `HTTP ${response.status}`)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      // SSE 事件以双换行分隔
      const blocks = buffer.split(/\n\n|\r\n\r\n/)
      buffer = blocks.pop() || ''

      for (const block of blocks) {
        if (!block.trim()) continue
        // 提取 data: 行（支持多行 data）
        const dataLines = []
        for (const line of block.split('\n')) {
          if (line.startsWith('data: ')) {
            dataLines.push(line.slice(6))
          }
        }
        if (!dataLines.length) continue
        const raw = dataLines.join('\n')
        try {
          const data = JSON.parse(raw)
          if (data.type === 'phase') onPhase?.(data)
          else if (data.type === 'progress') onProgress?.(data)
          else if (data.type === 'done') onDone?.(data)
          else if (data.type === 'error') onError?.(new Error(data.error || 'Agent 执行出错'))
          // 兼容无 type 字段：从 event 名推断
          else if (data.phase && !data.token) onPhase?.(data)
          else if (data.token) onProgress?.(data)
          else if (data.status) onDone?.(data)
        } catch { /* ignore parse errors */ }
      }
    }
    // 处理剩余 buffer
    if (buffer.trim()) {
      try {
        const m = buffer.match(/data:\s?(.+)/)
        if (m) {
          const data = JSON.parse(m[1])
          if (data.type === 'done' || data.status) onDone?.(data)
        }
      } catch { /* ignore */ }
    }
  }).catch(err => {
    onError?.(err)
  })
}

// 获取任务结果
export function getTaskResult(id) {
  return request.get(`/agents/tasks/${id}`)
}
