// 백엔드 com.nl2sql.gate.orchestrator.SchemaCatalog* 와 1:1 대응.
export interface SchemaCatalogColumn {
  columnName: string
  displayName: string
  minRoleLevel: number
}

export interface SchemaCatalogTable {
  tableName: string
  displayName: string
  description: string | null
  columns: SchemaCatalogColumn[]
}
