/**
 * 简易 Markdown 渲染器 — 将 AI 文本说明转为带样式的 HTML
 * 参考 deepseek-webui 的 renderMarkdown 思路
 */

export function renderMarkdown(text) {
  if (!text) return ''

  // 1. 保护代码块，避免内部内容被 markdown 规则误处理
  const codeBlocks = []
  let html = text.replace(/```(\w*)\s*[\r\n]*([\s\S]*?)```/g, (_, lang, code) => {
    const id = codeBlocks.length
    codeBlocks.push({ lang: lang || 'text', code: code.trim() })
    return `__CODE_${id}__`
  })

  // 2. 保护行内代码
  const inlineCodes = []
  html = html.replace(/`([^`]+)`/g, (_, code) => {
    const id = inlineCodes.length
    inlineCodes.push(code)
    return `__ICODE_${id}__`
  })

  // 3. HTML 转义（除已保护的内容外）
  html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

  // 4. Markdown 规则
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  html = html.replace(/^\- (.+)$/gm, '<li>$1</li>')
  html = html.replace(/^(\d+)\. (.+)$/gm, '<li>$2</li>')
  html = html.replace(/\n\n/g, '</p><p>')
  html = html.replace(/\n/g, '<br>')
  html = '<p>' + html + '</p>'

  // 5. 恢复行内代码
  html = html.replace(/__ICODE_(\d+)__/g, (_, id) =>
    `<code>${inlineCodes[id].replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')}</code>`)

  // 6. 恢复代码块
  html = html.replace(/__CODE_(\d+)__/g, (_, id) => {
    const b = codeBlocks[id]
    return `<pre class="md-code"><code>${b.code.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')}</code></pre>`
  })

  return html
}

/**
 * 复制到剪贴板，带 execCommand 兜底（参考 deepseek-webui）
 */
export async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    // 兜底方案
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.cssText = 'position:fixed;opacity:0'
    document.body.appendChild(ta)
    ta.select()
    try {
      document.execCommand('copy')
      document.body.removeChild(ta)
      return true
    } catch {
      document.body.removeChild(ta)
      return false
    }
  }
}
