import type { Role } from './auth'

// 백엔드 com.nl2sql.gate.user.UserSummary / UserUpdateInput 과 1:1 대응.
export interface UserSummary {
  id: string
  email: string
  name: string
  role: Role
  roleLevel: number
  active: boolean
  createdAt: string
}

export interface UserUpdateInput {
  active?: boolean
  role?: Role
}
