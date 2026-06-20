/**
 * Jinja2 / Flask / Django 模板 → 静态 HTML 转换工具
 *
 * 后端项目（Flask/Django/FastAPI Jinja2）的 HTML 模板包含服务端语法
 * （{% %} {{ }} {# #}），无法直接在浏览器沙箱中渲染。
 * 本工具将其转换为「尽力而为」的静态 HTML 预览。
 *
 * 转换策略:
 *   1. 查找 base.html + 子模板 → 内联布局
 *   2. 剥离所有 {% %} 控制语句，保留块内容
 *   3. {{ url_for() }} → #
 *   4. {{ variable }} → '[...]' 占位符
 *   5. {# comment #} → 移除
 *   6. 清理多余空行
 */

/**
 * 判断文件映射中是否包含 Jinja2/Flask 模板文件
 * @param {Object<string,string>} files - { path: content }
 * @returns {boolean}
 */
export function hasJinja2Templates(files) {
  if (!files || typeof files !== 'object') return false
  const paths = Object.keys(files)
  if (paths.length === 0) return false

  // 检测 templates/ 目录结构（Flask/Django 特征）
  const hasTemplateDir = paths.some((p) => /templates[\\/]/i.test(p))

  // 检测 Jinja2 语法
  const allCode = paths.map((p) => files[p] || '').join('\n')
  const hasJinja2Syntax =
    /\{%\s*(extends|block|for|if|include|endfor|endif|endblock|url_for)\s/i.test(allCode) ||
    /\[\[.*?\]\]/g.test(allCode) // Django 也支持 [[ ]] 语法

  return hasTemplateDir || hasJinja2Syntax
}

/**
 * 判断是否有可预览的 HTML 文件（前端或后端模板均可）
 * @param {Object<string,string>} files - { path: content }
 * @returns {boolean}
 */
export function hasPreviewableFiles(files) {
  if (!files || typeof files !== 'object') return false
  return Object.keys(files).some((p) => /\.html?$/i.test(p))
}

/**
 * 剥离 Jinja2/Django 模板语法，转为静态 HTML
 * @param {string} html - 原始模板内容
 * @returns {string} 静态 HTML
 */
export function stripJinja2(html) {
  if (!html) return ''

  let result = html

  // 1. 移除 extends（单行）
  result = result.replace(/\{%\s*extends\s+["'][^"']*["']\s*%\}/g, '')

  // 2. 剥离 block/open/close 语句，保留内部内容
  result = result.replace(/\{%-?\s*block\s+\w+\s*-?%\}/g, '')
  result = result.replace(/\{%-?\s*endblock\s*-?%\}/g, '')

  // 3. 移除 for/endfor — 保留循环体内容
  result = result.replace(/\{%-?\s*for\s+[^%]+?-?%\}/g, '')
  result = result.replace(/\{%-?\s*endfor\s*-?%\}/g, '')

  // 4. 移除 if/elif/else/endif — 保留条件体内容
  result = result.replace(/\{%-?\s*if\s+[^%]+?-?%\}/g, '')
  result = result.replace(/\{%-?\s*elif\s+[^%]+?-?%\}/g, '')
  result = result.replace(/\{%-?\s*else\s*-?%\}/g, '')
  result = result.replace(/\{%-?\s*endif\s*-?%\}/g, '')

  // 5. 移除 include / macro / from / import / set / call / with / endwith 等
  result = result.replace(/\{%-?\s*(include|macro|endmacro|from|import|set|call|endcall|filter|endfilter|with|endwith|while|endwhile|raw|endraw|autoescape|endautoescape|trans|endtrans|pluralize)\s*[^%]*?-?%\}/g, '')

  // 6. 移除 {% url ... %} Django 标签
  result = result.replace(/\{%-?\s*url\s+[^%]+?-?%\}/g, '#')

  // 7. 移除 csrf_token / load / static 等独立标签
  result = result.replace(/\{%-?\s*(csrf_token|load|static|lorem|now|spaceless|endspaceless|verbatim|endverbatim)\s*[^%]*?-?%\}/g, '')

  // 8. url_for(...) → #
  result = result.replace(/\{\{-?\s*url_for\s*\([^)]*\)\s*-?\}\}/g, '#')

  // 9. 其他变量表达式 → 占位符
  result = result.replace(/\{\{-?\s*[^}]+\s*-?\}\}/g, '[...]')

  // 10. 移除 {# comment #} 和 Django {# comment #}
  result = result.replace(/\{#[\s\S]*?#\}/g, '')

  // 11. 清理 HTML 属性中的 Jinja2 表达式（class 拼接等）
  result = result.replace(/\{\{[\s\S]*?\}\}/g, '[...]')

  // 12. 清理残留空行
  result = result.replace(/\n\s*\n\s*\n/g, '\n\n')
  result = result.replace(/^\s*\n/gm, '')

  // 13. 注入响应式覆盖样式
  //   - simplecss 用 grid-template-columns: 1fr min(45rem,90%) 1fr 居中布局 → display:block 干掉
  //   - Bootstrap/Tailwind 用 max-width 限制 → max-width:100% 覆盖
  const RESPONSIVE_OVERRIDE = `
<style>
  /* CodeForge preview: override CSS framework layout constraints */
  body {
    display: block !important;
    max-width: 100% !important;
    width: auto !important;
    margin: 0 !important;
    padding: 12px 16px !important;
    box-sizing: border-box !important;
  }
  body > * {
    grid-column: auto !important;
  }
  main, .container, .wrapper, [role="main"], article {
    max-width: 100% !important;
    width: auto !important;
    margin-left: auto !important;
    margin-right: auto !important;
    padding: 12px 16px !important;
    box-sizing: border-box !important;
  }
  img, video, iframe, pre, table { max-width: 100% !important; height: auto !important; }
</style>`

  if (/<\/head>/i.test(result)) {
    result = result.replace(/<\/head>/i, RESPONSIVE_OVERRIDE + '\n</head>')
  } else if (/<body[^>]*>/i.test(result)) {
    result = result.replace(/(<body[^>]*>)/i, '$1\n' + RESPONSIVE_OVERRIDE)
  } else {
    result = RESPONSIVE_OVERRIDE + '\n' + result
  }

  return result.trim() || '<html><body><p>预览生成中...</p></body></html>'
}

/**
 * 解析 Flask 模板继承关系，将 base.html 和子模板合并为单一 HTML
 *
 * @param {Object<string,string>} files - { path: content } 文件映射
 * @returns {Object<string,string>} 合并后的文件映射（可选：用于传给沙箱预览）
 */
export function resolveFlaskTemplates(files) {
  if (!files || typeof files !== 'object') return { ...files }

  const paths = Object.keys(files)
  const templateFiles = paths.filter((p) => /\.html?$/i.test(p))
  if (templateFiles.length === 0) return { ...files }

  // 查找 base.html / layout.html（布局模板）
  const baseFile =
    templateFiles.find((p) => /base\.html$/i.test(p)) ||
    templateFiles.find((p) => /layout\.html$/i.test(p))

  // 查找入口模板（优先 index.html）
  const entryFile =
    templateFiles.find((p) => /index\.html$/i.test(p)) ||
    templateFiles.filter((p) => !baseFile || p !== baseFile)[0]

  // 如果没有入口文件，直接对每个文件做独立转换
  const result = {}

  for (const path of paths) {
    if (!templateFiles.includes(path)) {
      // 非模板文件保持原样
      result[path] = files[path]
      continue
    }

    let html = files[path]

    // 检查是否 extends 了 base
    const extendsMatch = html.match(/\{%-?\s*extends\s+["']([^"']+)["']\s*-?%\}/)
    if (extendsMatch && baseFile) {
      const parentName = extendsMatch[1] // e.g. "base.html"
      const baseHtml = files[baseFile]

      if (baseHtml) {
        // 提取子模板中所有 block 内容
        const blocks = {}
        const blockRegex = /\{%-?\s*block\s+(\w+)\s*-?%\}([\s\S]*?)\{%-?\s*endblock\s*\1?\s*-?%\}/g
        let match
        while ((match = blockRegex.exec(html)) !== null) {
          blocks[match[1]] = match[2]
        }

        // 将子模板 block 内容替换到 base 对应 block 位置
        let resolved = baseHtml
        for (const [name, content] of Object.entries(blocks)) {
          const bkRegex = new RegExp(
            `\\{%-?\\s*block\\s+${name}\\s*-?%\\}[\\s\\S]*?\\{%-?\\s*endblock\\s*(?:${name})?\\s*-?%\\}`,
            'g'
          )
          resolved = resolved.replace(bkRegex, content)
        }

        html = resolved
      }
    }

    // 剥离 Jinja2 语法
    result[path] = stripJinja2(html)
  }

  return result
}

/**
 * 准备用于沙箱预览的文件映射
 * - 前端 Vue/HTML 项目 → 原样返回
 * - 后端 Jinja2 项目 → 转换为静态 HTML
 *
 * @param {Object<string,string>} files - { path: content }
 * @param {'frontend'|'backend'} projectType
 * @returns {{ files: Object<string,string>, isBackendPreview: boolean, entryPath: string }}
 */
export function preparePreviewFiles(files, projectType = 'frontend') {
  if (projectType === 'backend') {
    const resolved = resolveFlaskTemplates(files)
    // 找到最佳入口文件
    const paths = Object.keys(resolved)
    const entryPath =
      paths.find((p) => /index\.html$/i.test(p)) ||
      paths.find((p) => /\.html$/i.test(p)) ||
      ''
    return { files: resolved, isBackendPreview: true, entryPath }
  }

  return { files: { ...files }, isBackendPreview: false, entryPath: '' }
}
