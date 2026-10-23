import request from '@/utils/request'
import type { Page } from './applicant'

/** One subject in the shared catalogue, reused by programmes at any university. */
export interface Major {
  id: number
  name: string
  nameCn: string | null
  /** A broad subject group code such as MEDICINE; null when ungrouped. */
  discipline: string | null
  /** How many programmes use this major. */
  programCount: number
}

export interface MajorBody {
  name: string
  nameCn?: string | null
  discipline?: string | null
}

export interface MajorBulkResult {
  majors: Major[]
  created: number
}

const BASE = '/api/staff/majors'

export const listMajors = (params: { q?: string, discipline?: string, page?: number, size?: number }): Promise<Page<Major>> =>
  request({ url: BASE, method: 'get', params })

export const createMajor = (body: MajorBody): Promise<Major> => request({ url: BASE, method: 'post', data: body })

export const updateMajor = (id: number, body: MajorBody): Promise<Major> =>
  request({ url: `${BASE}/${id}`, method: 'put', data: body })

export const deleteMajor = (id: number): Promise<void> => request({ url: `${BASE}/${id}`, method: 'delete' })

/** Moves the programme links of `id` to `intoId` and deletes `id`. */
export const mergeMajor = (id: number, intoId: number): Promise<Major> =>
  request({ url: `${BASE}/${id}/merge`, method: 'post', data: { intoId } })

/** Pasted lines, one major each; existing names are reused, missing ones created. */
export const bulkMajors = (text: string, discipline?: string | null): Promise<MajorBulkResult> =>
  request({ url: `${BASE}/bulk`, method: 'post', data: { text, discipline } })

/** The broad groups offered in the screens; the server accepts any short code. */
export const DISCIPLINES = [
  'MEDICINE', 'ENGINEERING', 'BUSINESS', 'SCIENCE', 'ARTS', 'EDUCATION', 'LAW', 'AGRICULTURE', 'LANGUAGES', 'OTHER',
] as const
