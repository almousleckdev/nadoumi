import request from '@/utils/request'
import { uploadMedia, type MediaUploadResult } from './media'
import type { Page } from './applicant'
import type { PublishStatus } from './university'

export type { PublishStatus }
export type ProgramStatus = 'ACTIVE' | 'INACTIVE'
export type ProgramType = 'LANGUAGE' | 'NON_DEGREE' | 'DIPLOMA' | 'BACHELOR' | 'MASTER' | 'PHD'
export type ProgramTeachingLanguage = 'ENGLISH' | 'CHINESE' | 'BILINGUAL'

export const PROGRAM_TYPES: ProgramType[] = [
  'LANGUAGE', 'NON_DEGREE', 'DIPLOMA', 'BACHELOR', 'MASTER', 'PHD',
]
export const PROGRAM_LANGUAGES: ProgramTeachingLanguage[] = ['ENGLISH', 'CHINESE', 'BILINGUAL']
export const INTAKE_TERMS = ['SPRING_MARCH', 'AUTUMN_SEPTEMBER', 'SUMMER', 'WINTER', 'ROLLING']

export interface ProgramMajor {
  id?: number
  name: string
  nameCn: string | null
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
  field: string | null
  teachingLanguage: ProgramTeachingLanguage | null
  durationMonths: number | null
  tuitionAmount: number | null
  tuitionCurrency: string | null
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
  field?: string | null
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
