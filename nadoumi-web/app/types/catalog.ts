/**
 * Shapes returned by `/api/public/**`. Placeholder until an OpenAPI generator is
 * wired to `/api/v1/**`. The **student** scholarship view can never carry a
 * university / partnership field (enforced server-side, docs/DOMAIN_MODEL.md §6).
 */
export interface ScholarshipSummary {
  id: number
  title: string
  country: string
  degreeLevel?: string
  field?: string
  deadline?: string
}

export interface UniversitySummary {
  id: number
  name: string
  country: string
  city?: string
}

export interface ProgramSummary {
  id: number
  universityId: number
  name: string
  degreeLevel: string
  field?: string
  language?: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
