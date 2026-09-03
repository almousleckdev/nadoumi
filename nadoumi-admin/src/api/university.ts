import request from '@/utils/request'
import type { Page } from './applicant'

export type UniversityStatus = 'ACTIVE' | 'INACTIVE'
export type PublishStatus = 'DRAFT' | 'PUBLISHED'
export type UniversityType = 'PUBLIC' | 'PRIVATE'
export type HighlightKind = 'HIGHLIGHT' | 'ADVANTAGE'

export interface UniversityRanking {
  id?: number
  source: string
  rankPosition: number | null
  rankYear: number | null
  note: string | null
}

export interface UniversityHighlight {
  id?: number
  kind: HighlightKind
  text: string
}

export interface UniversityGalleryImage {
  id?: number
  imageUrl: string
  caption: string | null
}

export interface University {
  id: number
  slug: string
  name: string
  nameCn: string | null
  country: string
  type: UniversityType | null
  city: string | null
  province: string | null
  foundedYear: number | null
  totalStudents: number | null
  internationalStudents: number | null
  facultyCount: number | null
  website: string | null
  rankingTier: string | null
  introduction: string | null
  history: string | null
  campusInfo: string | null
  accommodationInfo: string | null
  nearbyInfo: string | null
  admissionsEmail: string | null
  officePhone: string | null
  logoDocumentId: number | null
  bannerDocumentId: number | null
  logoImageUrl: string | null
  coverImageUrl: string | null
  recommended: boolean
  featured: boolean
  status: UniversityStatus
  publishStatus: PublishStatus
  remark: string | null
  createdAt: string | null
  updatedAt: string | null
  rankings: UniversityRanking[]
  highlights: UniversityHighlight[]
  gallery: UniversityGalleryImage[]
}

export interface UniversityInput {
  name: string
  nameCn?: string | null
  country: string
  type?: UniversityType | null
  city?: string | null
  province?: string | null
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
  logoImageUrl?: string | null
  coverImageUrl?: string | null
  recommended: boolean
  featured: boolean
  status: UniversityStatus
  publishStatus: PublishStatus
  remark?: string | null
  rankings: UniversityRanking[]
  highlights: UniversityHighlight[]
  gallery: UniversityGalleryImage[]
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
