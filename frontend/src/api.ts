// 與後端溝通的唯一出口
// - 寫入類請求自動帶 X-XSRF-TOKEN，值從 Cookie 讀
// - 後端的錯誤格式 {code, message} 統一轉成 ApiError，連線失敗也一樣
// - 任何請求收到「未登入」，通知畫面回到登入頁
import type { Account, Favorite, FavoriteInput, FavoritePage, Me } from './types'

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message)
  }
}

let onSessionExpired: () => void = () => {}

/** 任何請求收到「未登入」時通知畫面回到登入頁 */
export function setSessionExpiredHandler(handler: () => void) {
  onSessionExpired = handler
}

function csrfToken(): string {
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)
  return match?.[1] ? decodeURIComponent(match[1]) : ''
}

/** 後端的回應格式固定，這裡只做型別標註，不再逐欄驗證 */
async function readJson<T>(response: Response): Promise<T> {
  return (await response.json()) as T
}

export function errorMessage(e: unknown): string {
  return e instanceof ApiError ? e.message : '發生錯誤，請稍後再試'
}

interface Options {
  json?: unknown
  form?: Record<string, string>
  ifMatch?: number
}

async function request(method: string, path: string, options: Options = {}): Promise<Response> {
  const headers: Record<string, string> = {}
  let body: string | undefined
  if (method !== 'GET') headers['X-XSRF-TOKEN'] = csrfToken()
  if (options.json !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(options.json)
  }
  if (options.form) {
    headers['Content-Type'] = 'application/x-www-form-urlencoded'
    body = new URLSearchParams(options.form).toString()
  }
  if (options.ifMatch !== undefined) headers['If-Match'] = `"${options.ifMatch}"`

  let response: Response
  try {
    response = await fetch(path, { method, headers, body, credentials: 'same-origin' })
  } catch {
    throw new ApiError(0, 'NETWORK', '連不上伺服器，請確認網路後再試')
  }
  if (response.ok) return response

  let code = 'UNKNOWN'
  let message = '發生錯誤，請稍後再試'
  try {
    const error = (await response.json()) as { code: string; message: string }
    code = error.code
    message = error.message
  } catch {
    // 不是我們的 API 回的（例如代理伺服器的 502），沿用預設訊息
  }
  if (code === 'UNAUTHENTICATED') onSessionExpired()
  throw new ApiError(response.status, code, message)
}

export async function fetchMe(): Promise<Me> {
  return readJson(await request('GET', '/api/me'))
}

export async function login(userId: string, password: string): Promise<void> {
  // 第一次載入時 /api/me 的 401 會順便發 CSRF Cookie，萬一被清掉就先補領
  if (!csrfToken()) await fetchMe().catch(() => undefined)
  await request('POST', '/api/auth/login', { form: { userId, password } })
}

export async function logout(): Promise<void> {
  await request('POST', '/api/auth/logout')
}

export async function addAccount(account: string, accountName: string): Promise<Account> {
  return readJson(await request('POST', '/api/me/accounts', { json: { account, accountName } }))
}

export async function listFavorites(afterSn = 0): Promise<FavoritePage> {
  return readJson(await request('GET', `/api/favorites?afterSn=${afterSn}`))
}

export async function getFavorite(sn: number): Promise<Favorite> {
  return readJson(await request('GET', `/api/favorites/${sn}`))
}

export async function createFavorite(input: FavoriteInput): Promise<Favorite> {
  return readJson(await request('POST', '/api/favorites', { json: input }))
}

export async function updateFavorite(
  sn: number,
  version: number,
  input: FavoriteInput,
): Promise<Favorite> {
  return readJson(await request('PUT', `/api/favorites/${sn}`, { json: input, ifMatch: version }))
}

export async function deleteFavorite(sn: number, version: number): Promise<void> {
  await request('DELETE', `/api/favorites/${sn}`, { ifMatch: version })
}
