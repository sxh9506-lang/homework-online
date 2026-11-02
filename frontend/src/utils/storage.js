/**
 * 安全存储：某些环境下（如第三方 iframe 分区存储、隐私模式）访问 localStorage 会抛异常，
 * 这里统一 try/catch，并在 localStorage 不可用时退化为内存存储 + Cookie。
 */
const memory = new Map()

function localAvailable() {
  try {
    const key = '__hw_probe__'
    window.localStorage.setItem(key, '1')
    window.localStorage.removeItem(key)
    return true
  } catch {
    return false
  }
}

const hasLocal = typeof window !== 'undefined' && typeof window.localStorage !== 'undefined' && localAvailable()

export const storage = {
  get(key) {
    if (hasLocal) {
      try {
        return window.localStorage.getItem(key)
      } catch {
        /* 忽略，走内存 */
      }
    }
    return memory.has(key) ? memory.get(key) : null
  },
  set(key, value) {
    memory.set(key, value)
    if (hasLocal) {
      try {
        window.localStorage.setItem(key, value)
      } catch {
        /* 忽略 */
      }
    }
  },
  remove(key) {
    memory.delete(key)
    if (hasLocal) {
      try {
        window.localStorage.removeItem(key)
      } catch {
        /* 忽略 */
      }
    }
  }
}

/* ------------------------- Cookie 通道 -------------------------
 * 同一域名下，Cookie 会随每个请求自动携带；
 * 与 Authorization 头形成双通道，任一路径被代理/插件拦截时仍可鉴权。
 */
export const COOKIE_NAME = 'hw_token'

export function setTokenCookie(token, maxAgeSeconds = 7 * 24 * 3600) {
  try {
    if (!token) {
      document.cookie = `${COOKIE_NAME}=; path=/; max-age=0; SameSite=Lax`
      return
    }
    document.cookie = `${COOKIE_NAME}=${encodeURIComponent(token)}; path=/; max-age=${maxAgeSeconds}; SameSite=Lax`
  } catch {
    /* 忽略 */
  }
}

export function getTokenCookie() {
  try {
    const match = document.cookie.match(new RegExp(`(?:^|;\\s*)${COOKIE_NAME}=([^;]*)`))
    return match ? decodeURIComponent(match[1]) : ''
  } catch {
    return ''
  }
}
