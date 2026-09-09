// localStorage 접근을 한 곳에 모아둔다 — 프라이빗 모드 등에서 접근이 막혀도
// 로그인 자체는 실패하지 않고 세션 동안만(메모리) 유지되도록 조용히 무시한다.
const KEY = 'nl2sql-gate.token'

export function getToken(): string | null {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null
  }
}

export function setToken(token: string): void {
  try {
    localStorage.setItem(KEY, token)
  } catch {
    // no-op
  }
}

export function clearToken(): void {
  try {
    localStorage.removeItem(KEY)
  } catch {
    // no-op
  }
}
