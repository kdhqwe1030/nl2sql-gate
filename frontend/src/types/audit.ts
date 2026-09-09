// 백엔드 com.nl2sql.gate.audit 과 1:1 대응.
export type LogStatus = 'SUCCESS' | 'CLARIFY' | 'DENIED' | 'BLOCKED' | 'ERROR'

export interface QueryLogEntry {
  id: string
  userName: string
  userRole: 'STAFF' | 'MANAGER' | 'ADMIN'
  question: string
  status: LogStatus
  deniedDetail: string | null
  rowCount: number | null
  latencyMs: number | null
  retryCount: number
  executedSql: string | null
  createdAt: string
}

export interface QueryLogPage {
  items: QueryLogEntry[]
  nextCursor: string | null
}

export interface TermUsage {
  term: string
  count: number
}

export interface StatsResponse {
  questionCount: number
  deniedCount: number
  clarifyRate: number
  avgLatencyMs: number | null
  topTerms: TermUsage[]
}
