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
  nameCn?: string | null
  country: string
  city?: string | null
  province?: string | null
  type?: 'PUBLIC' | 'PRIVATE' | null
  featured?: boolean
}

export interface UniversityRanking {
  id: number
  source: string
  rankPosition: number
  rankYear?: number | null
  note?: string | null
}

export interface UniversityHighlight {
  id: number
  kind: 'HIGHLIGHT' | 'ADVANTAGE'
  text: string
}

/** `/api/public/universities/{id}` — published, active universities only. */
export interface UniversityDetail extends UniversitySummary {
  foundedYear?: number | null
  totalStudents?: number | null
  internationalStudents?: number | null
  facultyCount?: number | null
  website?: string | null
  rankingTier?: string | null
  introduction?: string | null
  history?: string | null
  campusInfo?: string | null
  accommodationInfo?: string | null
  nearbyInfo?: string | null
  admissionsEmail?: string | null
  officePhone?: string | null
  recommended: boolean
  rankings: UniversityRanking[]
  highlights: UniversityHighlight[]
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

export interface SessionDto {
  authenticated: boolean
  user?: { userId: number; username: string; nickName: string | null; email: string | null }
  applicants?: { applicantId: number; accessRole: string; capabilities: string[] }[]
}

export interface ApplicantDto {
  id: number
  givenName: string
  familyName: string
  dob: string | null
  nationality: string | null
  passportNo: string | null
  email: string | null
  phone: string | null
  status: 'DRAFT' | 'ACTIVE' | 'UNLINKED' | 'ARCHIVED'
}

export interface EducationDto {
  id: number
  institution: string
  level: string | null
  field: string | null
  gpa: number | null
  gpaScale: number | null
  startDate: string | null
  endDate: string | null
}

export interface TestScoreDto {
  id: number
  testType: string
  score: string
  subScoresJson: string | null
  takenOn: string | null
  expiresOn: string | null
}

export interface ContactDto {
  id: number
  relation: 'GUARDIAN' | 'EMERGENCY' | 'OTHER'
  name: string
  email: string | null
  phone: string | null
}
