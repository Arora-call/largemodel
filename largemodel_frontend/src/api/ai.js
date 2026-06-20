/**
 * 流式代码生成（SSE）
 * 通过 fetch + ReadableStream 解析 SSE 事件流：
 *   event: token  → 逐 token 回调
 *   event: done   → 生成完成 + 完整代码
 *   event: error  → 错误信息
 */
/** 流式代码修改 */
export function modifyCodeStream(data, { onToken, onDone, onError, signal }) {
  return streamRequest('/api/ai/modify/stream', data, { onToken, onDone, onError, signal })
}

/** 流式代码生成 */
export function generateCodeStream(data, { onToken, onDone, onError, signal }) {
  return streamRequest('/api/ai/generate/stream', data, { onToken, onDone, onError, signal })
}

/** 流式代码审查 */
export function reviewCodeStream(data, { onToken, onDone, onError, signal }) {
  return streamRequest('/api/ai/review', data, { onToken, onDone, onError, signal })
}

function streamRequest(url, data, { onToken, onDone, onError, signal }) {
  const token = localStorage.getItem('token')

  return fetch(url, {
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

      // SSE 事件以 \n\n 分隔
      const blocks = buffer.split('\n\n')
      buffer = blocks.pop() || ''

      for (const block of blocks) {
        const eventMatch = block.match(/^event:\s*(\w+)/m)

        // 逐行提取 data: 内容后 join（SSE 多行 data 标准）
        const dataLines = []
        const dataRe = /^data:\s?(.*)$/gm
        let dm
        while ((dm = dataRe.exec(block)) !== null) {
          dataLines.push(dm[1])
        }
        if (dataLines.length === 0) continue

        const data = dataLines.join('\n').replace(/^data:\s?/gm, '')
        const eventType = eventMatch ? eventMatch[1] : 'message'

        if (eventType === 'token') {
          onToken?.(data)
        } else if (eventType === 'done') {
          onDone?.(data)
        } else if (eventType === 'error') {
          onError?.(new Error(data))
        }
      }
    }
  })
}
