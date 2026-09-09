import { http } from './http'
import type { QueryApiResponse, SchemaResponse } from '../types/query'

export function postQuery(question: string): Promise<QueryApiResponse> {
  return http.post<QueryApiResponse>('/api/query', { question })
}

export function getSchema(): Promise<SchemaResponse> {
  return http.get<SchemaResponse>('/api/schema')
}
