// 백엔드 com.nl2sql.gate.glossary 와 1:1 대응.
export interface GlossaryTerm {
  id: string
  term: string
  aliases: string[]
  definition: string
  sqlHint: string | null
  relatedTables: string[]
  minRoleLevel: number
  enabled: boolean
  hasEmbedding: boolean
  updatedAt: string
}

export interface GlossaryTermInput {
  term: string
  aliases: string[]
  definition: string
  sqlHint: string | null
  relatedTables: string[]
  minRoleLevel: number
  enabled: boolean
}
