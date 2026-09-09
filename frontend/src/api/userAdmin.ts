import { http } from './http'
import type { UserSummary, UserUpdateInput } from '../types/userAdmin'

export function getUsers(): Promise<UserSummary[]> {
  return http.get<UserSummary[]>('/api/admin/users')
}

export function updateUser(id: string, input: UserUpdateInput): Promise<UserSummary> {
  return http.patch<UserSummary>(`/api/admin/users/${id}`, input)
}
