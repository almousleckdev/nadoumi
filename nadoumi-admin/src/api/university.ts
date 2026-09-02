import request from '@/utils/request'
import type { Page } from './applicant'

export type UniversityStatus = 'ACTIVE' | 'INACTIVE'

export interface University {
  id: number
  name: string
  country: string
  city: string | null
  website: string | null
  rankingTier: string | null
  logoDocumentId: number | null
  status: UniversityStatus
  createdAt: string | null
  updatedAt: string | null
}

export interface UniversityInput {
  name: string
  country: string
  city?: string | null
  website?: string | null
  rankingTier?: string | null
  status: UniversityStatus
  remark?: string | null
}

const BASE = '/api/staff/universities'

export const listUniversities = (params: {
  q?: string
  country?: string
  status?: string
  page?: number
  size?: number
}) => request.get<unknown, Page<University>>(BASE, { params })

export const getUniversity = (id: number | string) =>
  request.get<unknown, University>(`${BASE}/${id}`)

export const createUniversity = (body: UniversityInput) =>
  request.post<unknown, University>(BASE, body)

export const updateUniversity = (id: number | string, body: UniversityInput) =>
  request.put<unknown, University>(`${BASE}/${id}`, body)

export const deleteUniversity = (id: number | string) =>
  request.delete(`${BASE}/${id}`)
