import { http } from './http'
import type { QueryLogPage, StatsResponse } from '../types/audit'

export function getStats(month: string): Promise<StatsResponse> {
  return http.get<StatsResponse>(`/api/admin/stats?month=${month}`)
}

export interface QueryLogFilter {
  from?: string
  to?: string
  limit?: number
  cursor?: string
}

export function getQueryLogs(filter: QueryLogFilter = {}): Promise<QueryLogPage> {
  const params = new URLSearchParams()
  if (filter.from) params.set('from', filter.from)
  if (filter.to) params.set('to', filter.to)
  if (filter.limit) params.set('limit', String(filter.limit))
  if (filter.cursor) params.set('cursor', filter.cursor)
  const qs = params.toString()
  return http.get<QueryLogPage>(`/api/admin/query-logs${qs ? `?${qs}` : ''}`)
}
