import { http } from './http'
import type { SchemaCatalogTable } from '../types/schemaCatalog'

export function getSchemaCatalog(): Promise<SchemaCatalogTable[]> {
  return http.get<SchemaCatalogTable[]>('/api/admin/tables')
}
