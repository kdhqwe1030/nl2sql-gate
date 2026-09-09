// 백엔드 com.nl2sql.gate.orchestrator / com.nl2sql.gate.execution 과 1:1 대응.
export interface QueryResult {
  columns: string[]
  rows: unknown[][]
  executedSql: string
  totalCount: number
  truncated: boolean
}

export interface QueryResultResponse {
  type: 'RESULT'
  status: 'SUCCESS'
  result: QueryResult
}

export interface ClarifyResponse {
  type: 'CLARIFY'
  status: 'CLARIFY'
  clarify: string
}

export interface ErrorResponse {
  type: 'ERROR'
  status: 'DENIED' | 'BLOCKED' | 'ERROR'
  message: string
}

export type QueryApiResponse = QueryResultResponse | ClarifyResponse | ErrorResponse

export interface SchemaTable {
  name: string
  columns: string[]
}

export interface SchemaResponse {
  tables: SchemaTable[]
}

// GET /api/schema 결과를 그대로 보여주는 답변 — 백엔드엔 없는, 프론트 전용 메타 질문 처리.
export interface MetaAnswer {
  type: 'META'
  tables: SchemaTable[]
}

export type AskAnswer = QueryApiResponse | MetaAnswer

export interface HistoryEntry {
  id: string
  question: string
  loading: boolean
  open: boolean
  answer: AskAnswer | null
}
