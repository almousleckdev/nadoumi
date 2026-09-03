import request from '@/utils/request'
import type { Page } from './applicant'

export type TeachingLanguage = 'ENGLISH' | 'CHINESE' | 'BOTH'
export type FundingModel = 'FULLY' | 'PARTIAL' | 'SELF'
export type EducationLevel = 'NON_DEGREE' | 'DIPLOMA' | 'BACHELOR' | 'MASTER' | 'PHD'
export type StipendFrequency = 'MONTHLY' | 'YEARLY' | 'ONE_OFF'
export type PublishStatus = 'DRAFT' | 'PUBLISHED'
export type ScholarshipStatus = 'ACTIVE' | 'INACTIVE'
export type NationalityScope = 'ANY' | 'INCLUDE' | 'EXCLUDE'
export type FeeKind =
  | 'TUITION_BEFORE' | 'TUITION_AFTER' | 'ACCOMMODATION_BEFORE' | 'ACCOMMODATION_AFTER'
  | 'REGISTRATION' | 'APPLICATION' | 'NADOUMI_APPLICATION' | 'NADOUMI_SERVICE'
  | 'INSURANCE' | 'VISA' | 'OTHER'

export const FEE_KINDS: FeeKind[] = [
  'TUITION_BEFORE', 'TUITION_AFTER', 'ACCOMMODATION_BEFORE', 'ACCOMMODATION_AFTER',
  'REGISTRATION', 'APPLICATION', 'NADOUMI_APPLICATION', 'NADOUMI_SERVICE', 'INSURANCE', 'VISA', 'OTHER',
]
export const EDUCATION_LEVELS: EducationLevel[] = ['NON_DEGREE', 'DIPLOMA', 'BACHELOR', 'MASTER', 'PHD']
export const DOC_TYPES = [
  'PASSPORT', 'DEGREE', 'TRANSCRIPT', 'LANGUAGE_CERT', 'VISA', 'PHYSICAL_EXAM',
  'STUDY_PLAN', 'RECOMMENDATION_LETTER', 'BANK_STATEMENT', 'PHOTO', 'CV', 'OTHER',
]
export const INTAKE_TERMS = ['SPRING_MARCH', 'AUTUMN_SEPTEMBER']

export interface Money { amount: number, currency: string }

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
export interface ScholarshipStipendInput {
  amount: number | null
  currency: string
  frequency: StipendFrequency
  durationMonths?: number | null
  conditions?: string | null
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
  title: string
  summary?: string | null
  country: string
  province?: string | null
  city?: string | null
  field?: string | null
  teachingLanguage?: TeachingLanguage | null
  fundingModel: FundingModel
  hasStipend: boolean
  deadline?: string | null
  applicationFee?: Money | null
  serviceFee?: Money | null
  slots?: number | null
  featured: boolean
  recommended: boolean
  hot: boolean
  heroImageUrl?: string | null
  coverImageUrl?: string | null
  levels: string[]
  categories: string[]
  intakes: ScholarshipIntakeInput[]
  benefits?: string | null
  requirements?: string | null
  policy?: string | null
  eligibility?: ScholarshipEligibilityInput | null
  fees: { kind: string, amount: number, currency: string, note?: string | null }[]
  stipend?: {
    amount: number
    currency: string
    frequency: StipendFrequency
    durationMonths?: number | null
    conditions?: string | null
  } | null
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
  deadline?: string | null
  benefits?: string | null
  requirements?: string | null
  policy?: string | null
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
  heroImageUrl?: string | null
  coverImageUrl?: string | null
  levels: EducationLevel[]
  categoryCodes: string[]
  intakes: ScholarshipIntakeInput[]
  eligibility?: ScholarshipEligibilityInput | null
  fees: ScholarshipFeeInput[]
  stipend?: ScholarshipStipendInput | null
  documentRequirements: ScholarshipDocumentRequirementInput[]
}

export interface ScholarshipInternal {
  universityId?: number | null
  universityName?: string | null
  partnershipId?: number | null
  internalStatus?: string | null
  operationalNotes?: string | null
  confidentialTerms?: string | null
  commissionModelJson?: string | null
}
export interface ScholarshipInternalInput {
  universityId?: number | null
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
