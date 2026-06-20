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
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event: ')) {
          // handle event type change
          continue
        }
        if (line.startsWith('data: ')) {
          try {
            const data = JSON.parse(line.slice(6))
            if (data.type === 'phase') onPhase?.(data)
            else if (data.type === 'progress') onProgress?.(data)
            else if (data.type === 'done') onDone?.(data)
          } catch { /* ignore parse errors */ }
        }
      }
    }
    onDone?.({})
  }).catch(err => {
    onError?.(err)
  })
}

// 获取任务结果
export function getTaskResult(id) {
  return request.get(`/agents/tasks/${id}`)
}
