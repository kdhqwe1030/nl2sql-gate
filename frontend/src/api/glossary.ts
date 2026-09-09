import { http } from './http'
import type { GlossaryTerm, GlossaryTermInput } from '../types/glossary'

export function getGlossaryTerms(): Promise<GlossaryTerm[]> {
  return http.get<GlossaryTerm[]>('/api/admin/glossary')
}

export function createGlossaryTerm(input: GlossaryTermInput): Promise<GlossaryTerm> {
  return http.post<GlossaryTerm>('/api/admin/glossary', input)
}

export function updateGlossaryTerm(id: string, input: GlossaryTermInput): Promise<GlossaryTerm> {
  return http.patch<GlossaryTerm>(`/api/admin/glossary/${id}`, input)
}

export function deleteGlossaryTerm(id: string): Promise<void> {
  return http.delete<void>(`/api/admin/glossary/${id}`)
}
