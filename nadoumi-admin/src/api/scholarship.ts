import request from '@/utils/request'
import { uploadMedia, type MediaUploadResult } from './media'
import type { Page } from './applicant'

export type TeachingLanguage = 'ENGLISH' | 'CHINESE' | 'BOTH'
export type FundingModel = 'FULLY' | 'PARTIAL' | 'SELF'
export type EducationLevel = 'NON_DEGREE' | 'DIPLOMA' | 'BACHELOR' | 'MASTER' | 'PHD'
export type StipendFrequency = 'MONTHLY' | 'YEARLY' | 'ONE_OFF'
export type PublishStatus = 'DRAFT' | 'PUBLISHED'
export type ScholarshipStatus = 'ACTIVE' | 'INACTIVE'
export type NationalityScope = 'ANY' | 'INCLUDE' | 'EXCLUDE'
export type RoomType = 'SINGLE' | 'DOUBLE' | 'TRIPLE' | 'QUAD' | 'SHARED'
export type NonDegreeDuration = 'HALF_YEAR' | 'ONE_YEAR'
export type CoverageKind =
  | 'TUITION' | 'ACCOMMODATION' | 'STIPEND' | 'MEDICAL_INSURANCE' | 'SETTLEMENT_ALLOWANCE'
  | 'TRAVEL' | 'REGISTRATION_FEE' | 'VISA_FEE' | 'OTHER'
export type ApplicationChannel = 'DIRECT_UNIVERSITY' | 'CSC_AGENCY' | 'NADOUMI' | 'OTHER'
export type FeeKind =
  | 'TUITION_BEFORE' | 'TUITION_AFTER'
  | 'REGISTRATION' | 'APPLICATION' | 'NADOUMI_APPLICATION' | 'NADOUMI_SERVICE'
  | 'INSURANCE' | 'VISA' | 'OTHER'

// ACCOMMODATION_BEFORE/AFTER are retired from the picker — accommodation is now
// its own room-type section — but stay valid codes for historical rows.
export const FEE_KINDS: FeeKind[] = [
  'TUITION_BEFORE', 'TUITION_AFTER',
  'REGISTRATION', 'APPLICATION', 'NADOUMI_APPLICATION', 'NADOUMI_SERVICE', 'INSURANCE', 'VISA', 'OTHER',
]
export const ROOM_TYPES: RoomType[] = ['SINGLE', 'DOUBLE', 'TRIPLE', 'QUAD', 'SHARED']
export const NON_DEGREE_DURATIONS: NonDegreeDuration[] = ['HALF_YEAR', 'ONE_YEAR']
export const COVERAGE_KINDS: CoverageKind[] = [
  'TUITION', 'ACCOMMODATION', 'STIPEND', 'MEDICAL_INSURANCE', 'SETTLEMENT_ALLOWANCE',
  'TRAVEL', 'REGISTRATION_FEE', 'VISA_FEE', 'OTHER',
]
export const APPLICATION_CHANNELS: ApplicationChannel[] = ['DIRECT_UNIVERSITY', 'CSC_AGENCY', 'NADOUMI', 'OTHER']
export const EDUCATION_LEVELS: EducationLevel[] = ['NON_DEGREE', 'DIPLOMA', 'BACHELOR', 'MASTER', 'PHD']
export const DOC_TYPES = [
  'PASSPORT', 'DEGREE', 'TRANSCRIPT', 'LANGUAGE_CERT', 'VISA', 'PHYSICAL_EXAM',
  'STUDY_PLAN', 'RECOMMENDATION_LETTER', 'BANK_STATEMENT', 'PHOTO', 'CV', 'OTHER',
]
export const INTAKE_TERMS = ['SPRING_MARCH', 'AUTUMN_SEPTEMBER']

export interface Money { amountRmb: number, amountUsd: number, currency: string }

export interface ScholarshipIntakeInput {
  term: string
  applicationOpen?: string | null
  applicationClose?: string | null
}
export interface ScholarshipFeeInput {
  kind: FeeKind
  amount: number | null
  currency: string
  note?: string | null
}
export interface ScholarshipEligibilityInput {
  ageMin?: number | null
  ageMax?: number | null
  nationalityScope?: NationalityScope | null
  acceptedCountries?: string | null
  inChina?: boolean | null
  gpaMin?: number | null
  ieltsMin?: number | null
  toeflMin?: number | null
  duolingoMin?: number | null
  hskMin?: number | null
  cscaMin?: number | null
  notes?: string | null
}
export interface ScholarshipLevelStipendInput {
  level: EducationLevel
  amount: number | null
  currency: string
  frequency: StipendFrequency
  durationMonths?: number | null
  conditions?: string | null
}
export interface ScholarshipAccommodationInput {
  roomType: RoomType
  amount: number | null
  currency: string
  note?: string | null
}
export interface ScholarshipCoverageInput {
  kind: CoverageKind
  detail?: string | null
}
export interface ScholarshipDocumentRequirementInput {
  docType: string
  mandatory: boolean
  note?: string | null
}

/** The student-safe view carried inside every ScholarshipResponse. */
export interface ScholarshipView {
  id: number
  slug: string
  referenceCode?: string | null
  title: string
  summary?: string | null
  country: string
  province?: string | null
  city?: string | null
  field?: string | null
  teachingLanguage?: TeachingLanguage | null
  fundingModel: FundingModel
  hasStipend: boolean
  nonDegreeDuration?: NonDegreeDuration | null
  studyDurationMonths?: number | null
  applicationChannel?: ApplicationChannel | null
  agencyNumber?: string | null
  requiresFinancialProof: boolean
  requiresFoundationYear: boolean
  deadline?: string | null
  applicationFee?: Money | null
  serviceFee?: Money | null
  slots?: number | null
  featured: boolean
  recommended: boolean
  hot: boolean
  /** @deprecated legacy URL string; use `heroUrl`. */
  heroImageUrl?: string | null
  /** @deprecated legacy URL string; use `coverUrl`. */
  coverImageUrl?: string | null
  heroMediaId?: number | null
  coverMediaId?: number | null
  /** Resolved absolute URL, or the legacy `heroImageUrl` fallback. */
  heroUrl?: string | null
  /** Resolved absolute URL, or the legacy `coverImageUrl` fallback. */
  coverUrl?: string | null
  levels: string[]
  categories: string[]
  intakes: ScholarshipIntakeInput[]
  benefits?: string | null
  requirements?: string | null
  policy?: string | null
  renewalConditions?: string | null
  eligibility?: ScholarshipEligibilityInput | null
  fees: { kind: string, amountRmb: number, amountUsd: number, currency: string, note?: string | null }[]
  stipends: {
    level: string
    amountRmb: number
    amountUsd: number
    currency: string
    frequency: StipendFrequency
    durationMonths?: number | null
    conditions?: string | null
  }[]
  accommodation: {
    roomType: string
    amountRmb: number | null
    amountUsd: number | null
    currency: string
    note?: string | null
  }[]
  coverage: { kind: string, detail?: string | null }[]
  documentRequirements: ScholarshipDocumentRequirementInput[]
}

export interface Scholarship {
  view: ScholarshipView
  status: ScholarshipStatus
  publishStatus: PublishStatus
  publishedAt?: string | null
  remark?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface ScholarshipInput {
  title: string
  summary?: string | null
  country: string
  province?: string | null
  city?: string | null
  field?: string | null
  teachingLanguage?: TeachingLanguage | null
  fundingModel: FundingModel
  hasStipend: boolean
  nonDegreeDuration?: NonDegreeDuration | null
  studyDurationMonths?: number | null
  applicationChannel?: ApplicationChannel | null
  agencyNumber?: string | null
  requiresFinancialProof: boolean
  requiresFoundationYear: boolean
  deadline?: string | null
  benefits?: string | null
  requirements?: string | null
  policy?: string | null
  renewalConditions?: string | null
  applicationFeeAmount?: number | null
  applicationFeeCurrency?: string | null
  serviceFeeAmount?: number | null
  serviceFeeCurrency?: string | null
  slots?: number | null
  featured: boolean
  recommended: boolean
  hot: boolean
  status: ScholarshipStatus
  publishStatus: PublishStatus
  remark?: string | null
  /** @deprecated still accepted by the backend; new uploads set `heroMediaId`. */
  heroImageUrl?: string | null
  /** @deprecated still accepted by the backend; new uploads set `coverMediaId`. */
  coverImageUrl?: string | null
  heroMediaId?: number | null
  coverMediaId?: number | null
  levels: EducationLevel[]
  categoryCodes: string[]
  intakes: ScholarshipIntakeInput[]
  eligibility?: ScholarshipEligibilityInput | null
  fees: ScholarshipFeeInput[]
  levelStipends: ScholarshipLevelStipendInput[]
  accommodations: ScholarshipAccommodationInput[]
  coverage: ScholarshipCoverageInput[]
  documentRequirements: ScholarshipDocumentRequirementInput[]
}

export interface ScholarshipInternal {
  universityId?: number | null
  universityName?: string | null
  /** Confidential: the specific programme this award funds. */
  programId?: number | null
  partnershipId?: number | null
  internalStatus?: string | null
  operationalNotes?: string | null
  confidentialTerms?: string | null
  commissionModelJson?: string | null
}
export interface ScholarshipInternalInput {
  universityId?: number | null
  programId?: number | null
  partnershipId?: number | null
  internalStatus?: string | null
  operationalNotes?: string | null
  confidentialTerms?: string | null
  commissionModelJson?: string | null
}

export interface ScholarshipCategoryOption { code: string, name: string }

const BASE = '/api/staff/scholarships'

export const listScholarships = (params: {
  q?: string, country?: string, funding?: string, publishStatus?: string, status?: string
  page?: number, size?: number
}) => request.get<unknown, Page<Scholarship>>(BASE, { params })

export const getScholarship = (id: number | string) =>
  request.get<unknown, Scholarship>(`${BASE}/${id}`)

export const createScholarship = (body: ScholarshipInput) =>
  request.post<unknown, Scholarship>(BASE, body)

export const updateScholarship = (id: number | string, body: ScholarshipInput) =>
  request.put<unknown, Scholarship>(`${BASE}/${id}`, body)

export const deleteScholarship = (id: number | string) =>
  request.delete(`${BASE}/${id}`)

export const getScholarshipInternal = (id: number | string) =>
  request.get<unknown, ScholarshipInternal>(`${BASE}/${id}/internal`)

export const putScholarshipInternal = (id: number | string, body: ScholarshipInternalInput) =>
  request.put<unknown, ScholarshipInternal>(`${BASE}/${id}/internal`, body)

export const listScholarshipCategories = () =>
  request.get<unknown, ScholarshipCategoryOption[]>('/api/public/scholarships/categories')

// ---- media uploads (multipart, field `file`) ----
export const uploadScholarshipHero = (id: number | string, file: File): Promise<MediaUploadResult> =>
  uploadMedia(`${BASE}/${id}/hero`, file)
export const uploadScholarshipCover = (id: number | string, file: File): Promise<MediaUploadResult> =>
  uploadMedia(`${BASE}/${id}/cover`, file)
