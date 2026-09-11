// 백엔드 com.nl2sql.gate.auth.dto / com.nl2sql.gate.user.Role 과 1:1 대응.
export type Role = 'STAFF' | 'MANAGER' | 'ADMIN'

export interface LoginRequest {
  email: string
  password: string
}

export interface SignupRequest {
  email: string
  password: string
  name: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresInMs: number
  name: string
  role: Role
}

export interface UserResponse {
  id: string
  email: string
  name: string
  role: Role
  roleLevel: number
}

export interface TenantResponse {
  name: string
}
