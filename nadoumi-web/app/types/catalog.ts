/**
 * Shapes returned by `/api/public/**`. Placeholder until an OpenAPI generator is
 * wired to `/api/v1/**`. The **student** scholarship view can never carry a
 * university / partnership field (enforced server-side, docs/DOMAIN_MODEL.md §6).
 */
/** `/api/public/scholarships` — student-safe only; never a university/partnership field. */
export interface Money {
  amount: number
  currency: string
}

export interface ScholarshipIntake {
  term: string
  applicationOpen?: string | null
  applicationClose?: string | null
}

export interface ScholarshipCard {
  id: number
  slug: string
  title: string
  summary?: string | null
  country: string
  province?: string | null
  city?: string | null
  field?: string | null
  teachingLanguage?: 'ENGLISH' | 'CHINESE' | 'BOTH' | null
  fundingModel: 'FULLY' | 'PARTIAL' | 'SELF'
  hasStipend: boolean
  deadline?: string | null
  applicationFee?: Money | null
  serviceFee?: Money | null
  slots?: number | null
  featured: boolean
  recommended: boolean
  hot: boolean
  levels: string[]
  categories: string[]
  intakes: ScholarshipIntake[]
}

export interface ScholarshipDetail extends ScholarshipCard {
  benefits?: string | null
  requirements?: string | null
  policy?: string | null
  eligibility?: {
    ageMin?: number | null
    ageMax?: number | null
    nationalityScope?: string | null
    acceptedCountries?: string | null
    inChina?: boolean | null
    gpaMin?: number | null
    ieltsMin?: number | null
    toeflMin?: number | null
    duolingoMin?: number | null
    hskMin?: number | null
    cscaMin?: number | null
    notes?: string | null
  } | null
  fees: { kind: string, amount: number, currency: string, note?: string | null }[]
  stipend?: {
    amount: number
    currency: string
    frequency: string
    durationMonths?: number | null
    conditions?: string | null
  } | null
  documentRequirements: { docType: string, mandatory: boolean, note?: string | null }[]
}

export interface FacetBucket {
  value: string
  count: number
}

export interface ScholarshipFacets {
  levels: FacetBucket[]
  categories: FacetBucket[]
  fundingModels: FacetBucket[]
  teachingLanguages: FacetBucket[]
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
  gallery: UniversityGalleryImage[]
}

export interface UniversityGalleryImage {
  id: number
  imageUrl: string
  caption?: string | null
}

export type ProgramType = 'LANGUAGE' | 'NON_DEGREE' | 'DIPLOMA' | 'BACHELOR' | 'MASTER' | 'PHD'
export type ProgramLanguage = 'ENGLISH' | 'CHINESE' | 'BILINGUAL'

export interface ProgramMajor {
  id: number
  name: string
  nameCn?: string | null
}

export interface ProgramIntake {
  id: number
  term: string
  applicationOpen?: string | null
  applicationClose?: string | null
}

/** `/api/public/programs` — student-safe; carries the owning university's name only. */
export interface ProgramCard {
  id: number
  universityId: number
  universityName: string | null
  name: string
  nameCn?: string | null
  programType: ProgramType
  field?: string | null
  teachingLanguage?: ProgramLanguage | null
  durationMonths?: number | null
  tuitionAmount?: number | null
  tuitionCurrency?: string | null
  summary?: string | null
  featured: boolean
  hot: boolean
}

export interface ProgramDetail extends ProgramCard {
  majors: ProgramMajor[]
  intakes: ProgramIntake[]
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
