function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

function renderInline(value) {
  const codeSpans = []
  let text = escapeHtml(value).replace(/`([^`]+)`/g, (_, code) => {
    const index = codeSpans.push(`<code>${code}</code>`) - 1
    return `@@CODE${index}@@`
  })
  text = text
    .replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+|mailto:[^\s)]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/~~([^~]+)~~/g, '<del>$1</del>')
    .replace(/(^|[^*])\*([^*]+)\*/g, '$1<em>$2</em>')
  return text.replace(/@@CODE(\d+)@@/g, (_, index) => codeSpans[Number(index)])
}

function isTableDivider(line) {
  return /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(line)
}

function splitTableRow(line) {
  return line.trim().replace(/^\||\|$/g, '').split('|').map(cell => cell.trim())
}

export function renderMarkdown(markdown) {
  const lines = String(markdown || '').replace(/\r\n?/g, '\n').split('\n')
  const html = []
  let inCode = false
  let codeLanguage = ''
  let codeLines = []
  let listType = ''
  const closeList = () => {
    if (listType) html.push(`</${listType}>`)
    listType = ''
  }

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index]
    const fence = line.match(/^```\s*([\w-]*)\s*$/)
    if (fence) {
      if (inCode) {
        html.push(`<pre><code class="language-${escapeHtml(codeLanguage)}">${escapeHtml(codeLines.join('\n'))}</code></pre>`)
        inCode = false
        codeLanguage = ''
        codeLines = []
      } else {
        closeList()
        inCode = true
        codeLanguage = fence[1] || ''
      }
      continue
    }
    if (inCode) {
      codeLines.push(line)
      continue
    }
    if (index + 1 < lines.length && line.includes('|') && isTableDivider(lines[index + 1])) {
      closeList()
      const headers = splitTableRow(line)
      const rows = []
      index += 2
      while (index < lines.length && lines[index].includes('|') && lines[index].trim()) {
        rows.push(splitTableRow(lines[index]))
        index += 1
      }
      index -= 1
      html.push(`<div class="md-table-wrap"><table><thead><tr>${headers.map(cell => `<th>${renderInline(cell)}</th>`).join('')}</tr></thead><tbody>${rows.map(row => `<tr>${row.map(cell => `<td>${renderInline(cell)}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`)
      continue
    }
    const heading = line.match(/^(#{1,6})\s+(.+)$/)
    if (heading) {
      closeList()
      html.push(`<h${heading[1].length}>${renderInline(heading[2])}</h${heading[1].length}>`)
      continue
    }
    if (/^\s*([-*_])(?:\s*\1){2,}\s*$/.test(line)) {
      closeList()
      html.push('<hr>')
      continue
    }
    const unordered = line.match(/^\s*[-*+]\s+(.+)$/)
    const ordered = line.match(/^\s*\d+[.)]\s+(.+)$/)
    if (unordered || ordered) {
      const nextType = unordered ? 'ul' : 'ol'
      if (listType !== nextType) {
        closeList()
        listType = nextType
        html.push(`<${listType}>`)
      }
      html.push(`<li>${renderInline((unordered || ordered)[1])}</li>`)
      continue
    }
    closeList()
    const quote = line.match(/^>\s?(.*)$/)
    if (quote) html.push(`<blockquote>${renderInline(quote[1])}</blockquote>`)
    else if (!line.trim()) html.push('')
    else html.push(`<p>${renderInline(line)}</p>`)
  }

  closeList()
  if (inCode) html.push(`<pre><code class="language-${escapeHtml(codeLanguage)}">${escapeHtml(codeLines.join('\n'))}</code></pre>`)
  return html.join('\n')
}
