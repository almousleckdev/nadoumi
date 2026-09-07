/**
 * Shapes returned by `/api/public/**`. Placeholder until an OpenAPI generator is
 * wired to `/api/v1/**`. The **student** scholarship view can never carry a
 * university / partnership field (enforced server-side, docs/DOMAIN_MODEL.md §6).
 */
/** `/api/public/scholarships` — student-safe only; never a university/partnership field. */
/** Every scholarship monetary value is shown in both RMB and USD (server-computed). */
export interface Money {
  amountRmb: number
  amountUsd: number
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
  referenceCode?: string | null
  title: string
  summary?: string | null
  country: string
  province?: string | null
  city?: string | null
  field?: string | null
  teachingLanguage?: 'ENGLISH' | 'CHINESE' | 'BOTH' | null
  fundingModel: 'FULLY' | 'PARTIAL' | 'SELF'
  hasStipend: boolean
  nonDegreeDuration?: string | null
  studyDurationMonths?: number | null
  applicationChannel?: string | null
  agencyNumber?: string | null
  requiresFinancialProof?: boolean
  requiresFoundationYear?: boolean
  deadline?: string | null
  applicationFee?: Money | null
  serviceFee?: Money | null
  slots?: number | null
  featured: boolean
  recommended: boolean
  hot: boolean
  /** Legacy raw ref (may be a relative `/profile/...` path); kept for one release. */
  heroImageUrl?: string | null
  coverImageUrl?: string | null
  /** Resolved URL — absolute (Cloudinary) when a media id is set, else the legacy string. */
  heroUrl?: string | null
  coverUrl?: string | null
  levels: string[]
  categories: string[]
  intakes: ScholarshipIntake[]
}

export interface ScholarshipDetail extends ScholarshipCard {
  benefits?: string | null
  requirements?: string | null
  policy?: string | null
  renewalConditions?: string | null
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
  fees: { kind: string, amountRmb: number, amountUsd: number, currency: string, note?: string | null }[]
  stipends: {
    level: string
    amountRmb: number
    amountUsd: number
    currency: string
    frequency: string
    durationMonths?: number | null
    conditions?: string | null
  }[]
  accommodation: {
    roomType: string
    amountRmb?: number | null
    amountUsd?: number | null
    currency: string
    note?: string | null
  }[]
  coverage: { kind: string, detail?: string | null }[]
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
  slug: string
  name: string
  nameCn?: string | null
  country: string
  city?: string | null
  province?: string | null
  type?: 'PUBLIC' | 'PRIVATE' | null
  featured?: boolean
  /** Curated editorial flag — shown in the public Partners showcase. */
  publicPartner?: boolean
  /** Legacy raw refs (may be relative `/profile/...` paths); kept for one release. */
  logoImageUrl?: string | null
  coverImageUrl?: string | null
  /** Resolved URLs — absolute (Cloudinary) when a media id is set, else the legacy string. */
  logoUrl?: string | null
  bannerUrl?: string | null
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
  /** Legacy raw ref (may be a relative `/profile/...` path); kept for one release. */
  imageUrl: string
  /** Resolved URL — absolute (Cloudinary) when a media id is set, else the legacy string. */
  url?: string | null
  caption?: string | null
}

export type ProgramType = 'DEGREE' | 'LANGUAGE' | 'NON_DEGREE'
export type DegreeLevel = 'DIPLOMA' | 'BACHELOR' | 'MASTER' | 'PHD'
export type ProgramLanguage = 'ENGLISH' | 'CHINESE' | 'BILINGUAL'

export interface ProgramMajor {
  id: number
  name: string
  nameCn?: string | null
  departmentName?: string | null
  /** One of the programme's levels (DEGREE only). */
  level?: DegreeLevel | null
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
  slug: string
  universityId: number
  universitySlug: string | null
  universityName: string | null
  name: string
  nameCn?: string | null
  programType: ProgramType
  /** DEGREE only — levels the programme spans, DIPLOMA→PHD order. Empty for LANGUAGE / NON_DEGREE. */
  levels: DegreeLevel[]
  field?: string | null
  teachingLanguage?: ProgramLanguage | null
  durationMonths?: number | null
  tuitionAmount?: number | null
  tuitionCurrency?: string | null
  /** Server-computed from the editable CNY→USD rate; null unless tuition is in CNY. */
  tuitionAmountUsd?: number | null
  summary?: string | null
  featured: boolean
  hot: boolean
  /** Resolved image URL — absolute (Cloudinary) when set, else `null` (programmes may have no image). */
  imageUrl?: string | null
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
