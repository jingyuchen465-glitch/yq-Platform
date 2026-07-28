export function submitAlipayForm(payForm) {
  if (typeof payForm !== 'string' || !payForm.trim()) {
    throw new Error('支付表单为空，请稍后重试')
  }
  const parsed = new DOMParser().parseFromString(payForm, 'text/html')
  const sourceForm = parsed.querySelector('form')
  if (!sourceForm) throw new Error('支付表单格式无效，请稍后重试')

  const action = new URL(sourceForm.getAttribute('action') || '', window.location.href)
  const hostname = action.hostname.toLowerCase()
  const isAlipayHost = hostname === 'alipay.com' || hostname.endsWith('.alipay.com') ||
    hostname === 'alipaydev.com' || hostname.endsWith('.alipaydev.com')
  if (action.protocol !== 'https:' || !isAlipayHost) {
    throw new Error('支付地址校验失败，请稍后重试')
  }
  const form = document.importNode(sourceForm, true)
  form.style.display = 'none'
  form.removeAttribute('target')
  document.body.appendChild(form)
  HTMLFormElement.prototype.submit.call(form)
}

export function formatMoney(value) {
  const amount = Number(value)
  return Number.isFinite(amount)
    ? new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount)
    : '0.00'
}

export function formatShortDate(value) {
  if (!value) return '时间待确认'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '时间待确认'
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit' }).format(date)
}
