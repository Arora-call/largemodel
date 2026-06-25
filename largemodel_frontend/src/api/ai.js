/**
 * 流式代码生成（SSE）
 * 通过 fetch + ReadableStream 解析 SSE 事件流：
 *   event: token      → 逐 token 回调
 *   event: tool-call  → 工具调用通知（@Tool 模式）
 *   event: done       → 生成完成 + 完整代码
 *   event: error      → 错误信息
 */
/** 流式代码修改（文件级） */
export function modifyCodeStream(data, { onToken, onDone, onError, signal }) {
  return streamRequest('/api/codegen/modify/stream', data, { onToken, onDone, onError, signal })
}

/** 流式代码生成 */
export function generateCodeStream(data, { onToken, onDone, onError, signal }) {
  return streamRequest('/api/ai/generate/stream', data, { onToken, onDone, onError, signal })
}

/** 流式代码审查 */
export function reviewCodeStream(data, { onToken, onDone, onError, signal }) {
  return streamRequest('/api/ai/review', data, { onToken, onDone, onError, signal })
}

/**
 * 统一流式代码生成（新架构 /api/codegen/stream）。
 * data.mode: 'SINGLE_FILE' | 'MULTI_FILE' | 'VUE_PROJECT'
 */
export function unifiedGenerateStream(data, { onToken, onToolCall, onDone, onError, signal }) {
  return streamRequest('/api/codegen/stream', data, { onToken, onToolCall, onDone, onError, signal })
}

function streamRequest(url, data, { onToken, onToolCall, onDone, onError, signal }) {
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

      // SSE 事件以 \n\n 分隔（兼容 \r\n\r\n）
      const blocks = buffer.split(/\n\n|\r\n\r\n/)
      buffer = blocks.pop() || ''

      for (const block of blocks) {
        processBlock(block, onToken, onToolCall, onDone, onError)
      }
    }

    // 处理 reader 结束后 buffer 中剩余的数据
    if (buffer.trim()) {
      processBlock(buffer, onToken, onToolCall, onDone, onError)
    }
  })
}

function processBlock(block, onToken, onToolCall, onDone, onError) {
  const eventMatch = block.match(/^event:\s*(\w+)/m)

  // 逐行提取 data: 内容后 join（SSE 多行 data 标准）
  const dataLines = []
  const dataRe = /^data:\s?(.*)$/gm
  let dm
  while ((dm = dataRe.exec(block)) !== null) {
    dataLines.push(dm[1])
  }
  if (dataLines.length === 0) return

  const data = dataLines.join('\n')
  const eventType = eventMatch ? eventMatch[1] : 'message'

  // JSON 包裹解析: {"d":"..."} → 提取 d 字段
  const unwrapped = unwrapJsonD(data)

  if (eventType === 'token') {
    onToken?.(unwrapped)
  } else if (eventType === 'tool-call') {
    onToolCall?.(unwrapped)
  } else if (eventType === 'done') {
    // done 的 data: {"d":"{...json...}"} → 先解外层，再传内层 JSON 字符串
    onDone?.(unwrapped)
  } else if (eventType === 'error') {
    onError?.(new Error(unwrapped))
  }
}

/** 解包 {"d":"..."} JSON 格式，返回 d 字段的原始值 */
function unwrapJsonD(raw) {
  try {
    const obj = JSON.parse(raw)
    if (obj && typeof obj.d === 'string') return obj.d
  } catch (e) { /* not JSON wrapped, return raw */ }
  return raw
}

/**
 * 部署对话代码到 Nginx 预览目录。
 * POST /api/codegen/deploy  { conversationId }
 * → { code: 200, data: { deployKey, url } }
 */
export function deployConversation(conversationId) {
  const token = localStorage.getItem('token')
  return fetch('/api/codegen/deploy', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ conversationId })
  }).then(res => res.json())
}

/**
 * 从应用 ID 部署代码到 Nginx 预览目录。
 * POST /api/codegen/deploy-by-app  { appId }
 * → { code: 200, data: { deployKey, url } }
 */
export function deployByAppId(appId) {
  const token = localStorage.getItem('token')
  return fetch('/api/codegen/deploy-by-app', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ appId })
  }).then(res => res.json())
}
