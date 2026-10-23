import request from '@/utils/request'
import { uploadMedia, type MediaUploadResult } from './media'
import type { Page } from './applicant'
import type { PublishStatus } from './university'

export type { PublishStatus }
export type ProgramStatus = 'ACTIVE' | 'INACTIVE'
/** The kind of programme. DEGREE carries one or more levels + majors; LANGUAGE / NON_DEGREE carry a term length. */
export type ProgramType = 'DEGREE' | 'LANGUAGE' | 'NON_DEGREE'
export type ProgramTeachingLanguage = 'ENGLISH' | 'CHINESE' | 'BILINGUAL'

export const PROGRAM_TYPES: ProgramType[] = ['DEGREE', 'LANGUAGE', 'NON_DEGREE']
export const PROGRAM_LANGUAGES: ProgramTeachingLanguage[] = ['ENGLISH', 'CHINESE', 'BILINGUAL']
export const INTAKE_TERMS = ['SPRING_MARCH', 'AUTUMN_SEPTEMBER', 'SUMMER', 'WINTER', 'ROLLING']

/** Academic levels a DEGREE programme can span; each major is tied to one of these. */
export type DegreeLevel = 'DIPLOMA' | 'BACHELOR' | 'MASTER' | 'PHD'
export const DEGREE_LEVELS: DegreeLevel[] = ['DIPLOMA', 'BACHELOR', 'MASTER', 'PHD']

export type TermLength = 'ONE_SEMESTER' | 'ONE_YEAR'
export const TERM_LENGTHS: TermLength[] = ['ONE_SEMESTER', 'ONE_YEAR']
export const isDegree = (t: ProgramType) => t === 'DEGREE'

export interface ProgramMajor {
  id?: number
  name: string
  nameCn: string | null
  departmentId?: number | null
  departmentName?: string | null
  /** One of the programme's levels (DEGREE only); null otherwise. */
  level?: DegreeLevel | null
}

export interface ProgramIntake {
  id?: number
  term: string
  applicationOpen: string | null
  applicationClose: string | null
}

export type TuitionPeriod = 'PER_YEAR' | 'PER_SEMESTER' | 'TOTAL'
export type AccommodationPeriod = 'PER_MONTH' | 'PER_YEAR' | 'TOTAL'
export const TUITION_PERIODS: TuitionPeriod[] = ['PER_YEAR', 'PER_SEMESTER', 'TOTAL']
export const ACCOMMODATION_PERIODS: AccommodationPeriod[] = ['PER_MONTH', 'PER_YEAR', 'TOTAL']

/** Shortest accepted duration per degree level, in months. Mirrors the server's ProgramOfferingRules. */
export const MIN_DURATION_MONTHS: Record<DegreeLevel, number> = { DIPLOMA: 12, BACHELOR: 48, MASTER: 24, PHD: 36 }

/** Typical durations offered in the picker; any value at or above the level minimum can also be typed. */
export const DURATION_OPTIONS: Record<DegreeLevel | 'OTHER', number[]> = {
  DIPLOMA: [24, 36],
  BACHELOR: [48, 60],
  MASTER: [24, 36],
  PHD: [36, 48, 60],
  OTHER: [3, 6, 9, 12, 18, 24],
}

/** One duration of one level, with its tuition and accommodation. `level` is null for language / non-degree programmes. */
export interface ProgramOffering {
  id?: number
  level: DegreeLevel | null
  durationMonths: number
  tuitionAmount: number | null
  tuitionCurrency: string | null
  tuitionPeriod: TuitionPeriod
  /** Server-computed from the editable CNY→USD rate. */
  tuitionAmountUsd?: number | null
  accommodationAmount: number | null
  accommodationCurrency: string | null
  accommodationPeriod: AccommodationPeriod
  accommodationAmountUsd?: number | null
  note: string | null
}

export interface Program {
  id: number
  universityId: number
  universityName: string | null
  slug: string
  name: string
  nameCn: string | null
  programType: ProgramType
  /** DEGREE only — the levels the programme spans, in DIPLOMA→PHD order. */
  levels: DegreeLevel[]
  field: string | null
  termLength: TermLength | null
  teachingLanguage: ProgramTeachingLanguage | null
  durationMonths: number | null
  tuitionAmount: number | null
  tuitionCurrency: string | null
  /** Computed on the server from the editable CNY→USD rate; null unless tuition is in CNY. */
  tuitionAmountUsd: number | null
  summary: string | null
  admissionRequirements: string | null
  applicationDocuments: string | null
  careerProspects: string | null
  scholarshipInfo: string | null
  minHsk: number | null
  minIelts: number | null
  minToefl: number | null
  applicationFeeAmount: number | null
  applicationFeeCurrency: string | null
  featured: boolean
  hot: boolean
  status: ProgramStatus
  publishStatus: PublishStatus
  remark: string | null
  createdAt: string | null
  updatedAt: string | null
  imageMediaId?: number | null
  /** Resolved absolute URL for the programme image, when one is set. */
  imageUrl?: string | null
  majors: ProgramMajor[]
  intakes: ProgramIntake[]
  offerings: ProgramOffering[]
}

export interface ProgramInput {
  universityId: number
  name: string
  nameCn?: string | null
  programType: ProgramType
  levels?: DegreeLevel[]
  field?: string | null
  termLength?: TermLength | null
  teachingLanguage?: ProgramTeachingLanguage | null
  summary?: string | null
  admissionRequirements?: string | null
  applicationDocuments?: string | null
  careerProspects?: string | null
  scholarshipInfo?: string | null
  minHsk?: number | null
  minIelts?: number | null
  minToefl?: number | null
  applicationFeeAmount?: number | null
  applicationFeeCurrency?: string | null
  featured: boolean
  hot: boolean
  status: ProgramStatus
  publishStatus: PublishStatus
  remark?: string | null
  imageMediaId?: number | null
  majors: ProgramMajor[]
  intakes: ProgramIntake[]
  offerings: ProgramOffering[]
}

const BASE = '/api/staff/programs'

export const listPrograms = (params: {
  q?: string
  universityId?: number
  type?: string
  language?: string
  field?: string
  status?: string
  page?: number
  size?: number
}) => request.get<unknown, Page<Program>>(BASE, { params })

export const getProgram = (id: number | string) =>
  request.get<unknown, Program>(`${BASE}/${id}`)

export const createProgram = (body: ProgramInput) =>
  request.post<unknown, Program>(BASE, body)

export const updateProgram = (id: number | string, body: ProgramInput) =>
  request.put<unknown, Program>(`${BASE}/${id}`, body)

export const deleteProgram = (id: number | string) =>
  request.delete(`${BASE}/${id}`)

// ---- media upload (multipart, field `file`) ----
export const uploadProgramImage = (id: number | string, file: File): Promise<MediaUploadResult> =>
  uploadMedia(`${BASE}/${id}/image`, file)


/** One document a programme asks for; the student's uploads are matched to it by `docType`. */
export interface ProgramDocumentRequirement {
  docType: string
  mandatory: boolean
  notes: string | null
  /** A condition code (`DOC_CONDITIONS`), or null when the document is always required. */
  appliesWhen: string | null
}

export function getProgramDocuments(id: number): Promise<ProgramDocumentRequirement[]> {
  return request({ url: `/api/staff/document-requirements/programs/${id}`, method: 'get' })
}

/** Replaces the whole list. */
export function saveProgramDocuments(id: number, requirements: ProgramDocumentRequirement[]): Promise<ProgramDocumentRequirement[]> {
  return request({ url: `/api/staff/document-requirements/programs/${id}`, method: 'put', data: { requirements } })
}
