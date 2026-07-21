/**
 * 请求签名工具 —— 与后端 SignInterceptor 验签逻辑严格对齐
 *
 * 签名原文格式: METHOD
URI
QueryString
Timestamp
Nonce
 *   - URI 为包含 context-path 的完整路径（如 /yq-admin/emp/sysUser/page）
 *   - QueryString 为 URL 编码后的查询串，无参数时为空字符串
 *
 * 签名算法: HMAC-SHA256(source, signSecret) → 标准 Base64
 */

/**
 * 生成随机 nonce（32 位十六进制，每次请求唯一，防重放）
 */
export function generateNonce() {
  const bytes = new Uint8Array(16)
  crypto.getRandomValues(bytes)
  return Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('')
}

/**
 * 序列化查询参数，与后端 request.getQueryString() 拿到的原始格式对齐。
 * 注意：必须在 axios 消费 params 之前完成序列化，保证「签名用的串」和「实际发出的串」完全一致。
 */
export function buildQueryString(params) {
  if (!params) return ''
  const usp = new URLSearchParams()
  Object.keys(params)
    .filter(k => params[k] !== undefined && params[k] !== null && params[k] !== '')
    .forEach(k => usp.append(k, params[k]))
  return usp.toString()
}

/**
 * 拼接签名原文（与后端 buildSignSource 一一对应）
 */
function buildSignSource(method, uri, query, timestamp, nonce) {
  return [method, uri, query, timestamp, nonce].join('\n')
}

/**
 * HMAC-SHA256 签名，输出标准 Base64（与后端 hutool digestBase64 结果一致）
 */
async function hmacSha256Base64(source, secret) {
  const enc = new TextEncoder()
  const key = await crypto.subtle.importKey(
    'raw',
    enc.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  )
  const sig = await crypto.subtle.sign('HMAC', key, enc.encode(source))
  return btoa(String.fromCharCode(...new Uint8Array(sig)))
}

/**
 * 为一次请求构建完整签名三要素
 * @param {string} method  请求方法（大写，如 GET / POST）
 * @param {string} uri     完整路径，含 context-path（如 /yq-admin/emp/sysUser/page）
 * @param {string} query   序列化后的查询串（无参数传空串）
 * @param {string} secret  登录时下发的 signSecret
 * @returns {Promise<{timestamp: string, nonce: string, sign: string}>}
 */
export async function buildRequestSign(method, uri, query, secret) {
  const timestamp = String(Date.now())
  const nonce = generateNonce()
  const source = buildSignSource(method.toUpperCase(), uri, query, timestamp, nonce)
  const sign = await hmacSha256Base64(source, secret)
  return { timestamp, nonce, sign }
}
