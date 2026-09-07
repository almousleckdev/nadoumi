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
  durationMonths?: number | null
  tuitionAmount?: number | null
  tuitionCurrency?: string | null
  summary?: string | null
  featured: boolean
  hot: boolean
  status: ProgramStatus
  publishStatus: PublishStatus
  remark?: string | null
  imageMediaId?: number | null
  majors: ProgramMajor[]
  intakes: ProgramIntake[]
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
