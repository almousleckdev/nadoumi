import request from '@/utils/request'
import { uploadMedia, type MediaUploadResult } from './media'
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
  /** @deprecated legacy URL string; new rows carry `mediaId` and resolve to `url`. */
  imageUrl?: string
  /** New media FK; null on legacy rows that still carry only `imageUrl`. */
  mediaId?: number | null
  /** Resolved absolute URL (Cloudinary), falling back to `imageUrl` server-side. */
  url?: string | null
  caption: string | null
}

export type PartnerStatus = 'NONE' | 'PROSPECT' | 'PARTNER'
export const PARTNER_STATUSES: PartnerStatus[] = ['NONE', 'PROSPECT', 'PARTNER']

export interface University {
  id: number
  slug: string
  /** Human reference, e.g. NAD-UNI-0007. Assigned on create. */
  referenceCode: string | null
  /** INTERNAL: catalog-only / talking / active relationship. */
  partnerStatus: PartnerStatus
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
  /** @deprecated legacy URL string; use `logoUrl`. Kept for one release. */
  logoImageUrl: string | null
  /** @deprecated legacy URL string; use `bannerUrl`. Kept for one release. */
  coverImageUrl: string | null
  logoMediaId?: number | null
  bannerMediaId?: number | null
  /** Resolved absolute URL, or the legacy `logoImageUrl` fallback. */
  logoUrl?: string | null
  /** Resolved absolute URL, or the legacy `coverImageUrl` fallback. */
  bannerUrl?: string | null
  recommended: boolean
  featured: boolean
  /** Curated: shown in the public Partners showcase. Separate from partnerStatus. */
  publicPartner: boolean
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
  /** @deprecated still accepted by the backend; new uploads set `logoMediaId`. */
  logoImageUrl?: string | null
  /** @deprecated still accepted by the backend; new uploads set `bannerMediaId`. */
  coverImageUrl?: string | null
  logoMediaId?: number | null
  bannerMediaId?: number | null
  recommended: boolean
  featured: boolean
  publicPartner?: boolean
  partnerStatus?: PartnerStatus
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

// ---- media uploads (multipart, field `file`) ----
export const uploadUniversityLogo = (id: number | string, file: File): Promise<MediaUploadResult> =>
  uploadMedia(`${BASE}/${id}/logo`, file)
export const uploadUniversityBanner = (id: number | string, file: File): Promise<MediaUploadResult> =>
  uploadMedia(`${BASE}/${id}/banner`, file)
export const uploadUniversityGalleryImage = (id: number | string, file: File): Promise<MediaUploadResult> =>
  uploadMedia(`${BASE}/${id}/gallery`, file)

// ---- academic departments (nested under a university) ----
export interface Department {
  id: number
  universityId: number
  name: string
  nameCn: string | null
  sortOrder: number
  /** majors that currently reference this department */
  programCount: number
}
export interface DepartmentInput {
  name: string
  nameCn?: string | null
  sortOrder?: number | null
}

const deptBase = (universityId: number | string) => `${BASE}/${universityId}/departments`

export const listDepartments = (universityId: number | string) =>
  request.get<unknown, Department[]>(deptBase(universityId))
export const createDepartment = (universityId: number | string, body: DepartmentInput) =>
  request.post<unknown, Department>(deptBase(universityId), body)
export const updateDepartment = (universityId: number | string, id: number, body: DepartmentInput) =>
  request.put<unknown, Department>(`${deptBase(universityId)}/${id}`, body)
export const deleteDepartment = (universityId: number | string, id: number) =>
  request.delete(`${deptBase(universityId)}/${id}`)
