import { http } from './http'
import type { TenantResponse } from '../types/auth'

export function getTenant(): Promise<TenantResponse> {
  return http.get<TenantResponse>('/api/tenant')
}
