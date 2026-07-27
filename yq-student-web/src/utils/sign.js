export function generateNonce() {
  const bytes = new Uint8Array(16)
  window.crypto.getRandomValues(bytes)
  return Array.from(bytes, value => value.toString(16).padStart(2, '0')).join('')
}

export function buildQueryString(params) {
  if (!params) return ''
  const query = new URLSearchParams()
  Object.keys(params).forEach(key => {
    const value = params[key]
    if (value === undefined || value === null || value === '') return
    const values = Array.isArray(value) ? value : [value]
    values.forEach(item => query.append(key, item))
  })
  return query.toString()
}

async function hmacSha256Base64(source, secret) {
  const encoder = new TextEncoder()
  const key = await window.crypto.subtle.importKey(
    'raw',
    encoder.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  )
  const signature = await window.crypto.subtle.sign('HMAC', key, encoder.encode(source))
  return window.btoa(String.fromCharCode(...new Uint8Array(signature)))
}

export async function buildRequestSign(method, uri, query, secret) {
  const timestamp = String(Date.now())
  const nonce = generateNonce()
  const source = [method.toUpperCase(), uri, query, timestamp, nonce].join('\n')
  const sign = await hmacSha256Base64(source, secret)
  return { timestamp, nonce, sign }
}
