import { inject, type InjectionKey } from 'vue'
import {
  type University, type UniversityInput, type UniversityLink, type PartnerStatus,
} from '@/api/university'
import { numberOrNull, textOrNull } from '@/utils/formValues'

export const MAX_GALLERY = 10

// Nadoumi's own contact details, pre-filled when a university has none of its own.
export const DEFAULT_WEBSITE = 'https://nadoumi.com/'
export const DEFAULT_ADMISSIONS_EMAIL = 'team@nadoumiconsulting.com'
export const DEFAULT_OFFICE_PHONE = '+86 159 0823 7607 / +86 155 2057 6024'

export type GalleryRow = {
  imageUrl: string | null
  mediaId: number | null
  url: string | null
  caption: string | null
  _file?: File | null
}
export type DeptRow = { id: number | undefined, name: string, nameCn: string | null }

export function blankUniversityForm() {
  return {
    id: undefined as number | undefined,
    name: '', nameCn: '', country: '', type: null as string | null,
    city: '', province: '', foundedYear: null as number | null,
    totalStudents: null as number | null, internationalStudents: null as number | null,
    facultyCount: null as number | null, website: DEFAULT_WEBSITE, rankingTier: '',
    introduction: '', history: '', campusInfo: '', accommodationInfo: '', nearbyInfo: '', address: '',
    admissionsEmail: DEFAULT_ADMISSIONS_EMAIL, officePhone: DEFAULT_OFFICE_PHONE,
    logoImageUrl: null as string | null, coverImageUrl: null as string | null,
    logoMediaId: null as number | null, bannerMediaId: null as number | null,
    recommended: false, featured: false, publicPartner: false,
    partnerStatus: 'NONE' as PartnerStatus,
    status: 'ACTIVE' as UniversityInput['status'],
    publishStatus: 'DRAFT' as UniversityInput['publishStatus'],
    remark: '',
    highlights: [] as University['highlights'],
    rankings: [] as University['rankings'],
    gallery: [] as GalleryRow[],
    links: [] as UniversityLink[],
    departments: [] as DeptRow[],
  }
}

export type UniversityForm = ReturnType<typeof blankUniversityForm>

export function formFromUniversity(u: University): UniversityForm {
  return {
    ...blankUniversityForm(),
    id: u.id,
    name: u.name, nameCn: u.nameCn ?? '', country: u.country, type: u.type,
    city: u.city ?? '', province: u.province ?? '', foundedYear: u.foundedYear,
    totalStudents: u.totalStudents, internationalStudents: u.internationalStudents,
    facultyCount: u.facultyCount, website: u.website || DEFAULT_WEBSITE, rankingTier: u.rankingTier ?? '',
    introduction: u.introduction ?? '', history: u.history ?? '', campusInfo: u.campusInfo ?? '',
    accommodationInfo: u.accommodationInfo ?? '', nearbyInfo: u.nearbyInfo ?? '', address: u.address ?? '',
    admissionsEmail: u.admissionsEmail || DEFAULT_ADMISSIONS_EMAIL, officePhone: u.officePhone || DEFAULT_OFFICE_PHONE,
    logoImageUrl: u.logoImageUrl ?? null, coverImageUrl: u.coverImageUrl ?? null,
    logoMediaId: u.logoMediaId ?? null, bannerMediaId: u.bannerMediaId ?? null,
    recommended: u.recommended, featured: u.featured, publicPartner: u.publicPartner ?? false,
    partnerStatus: (u.partnerStatus ?? 'NONE') as PartnerStatus,
    status: u.status, publishStatus: u.publishStatus, remark: u.remark ?? '',
    highlights: (u.highlights ?? []).map(h => ({ ...h })),
    rankings: (u.rankings ?? []).map(r => ({ ...r })),
    links: (u.links ?? []).map(l => ({ kind: l.kind, value: l.value })),
    gallery: (u.gallery ?? []).map(g => ({
      imageUrl: g.imageUrl ?? null, mediaId: g.mediaId ?? null, url: g.url ?? null, caption: g.caption,
    })),
  }
}

export function buildUniversityPayload(form: UniversityForm): UniversityInput {
  return {
    name: form.name.trim(),
    nameCn: textOrNull(form.nameCn),
    country: form.country.trim().toUpperCase(),
    type: (form.type as UniversityInput['type']) || null,
    city: textOrNull(form.city),
    province: textOrNull(form.province),
    foundedYear: numberOrNull(form.foundedYear),
    totalStudents: numberOrNull(form.totalStudents),
    internationalStudents: numberOrNull(form.internationalStudents),
    facultyCount: numberOrNull(form.facultyCount),
    website: textOrNull(form.website),
    rankingTier: textOrNull(form.rankingTier),
    introduction: textOrNull(form.introduction),
    history: textOrNull(form.history),
    campusInfo: textOrNull(form.campusInfo),
    accommodationInfo: textOrNull(form.accommodationInfo),
    nearbyInfo: textOrNull(form.nearbyInfo),
    address: textOrNull(form.address),
    admissionsEmail: textOrNull(form.admissionsEmail),
    officePhone: textOrNull(form.officePhone),
    logoImageUrl: form.logoImageUrl,
    coverImageUrl: form.coverImageUrl,
    logoMediaId: form.logoMediaId,
    bannerMediaId: form.bannerMediaId,
    recommended: form.recommended,
    featured: form.featured,
    publicPartner: form.publicPartner,
    partnerStatus: form.partnerStatus,
    status: form.status,
    publishStatus: form.publishStatus,
    remark: textOrNull(form.remark),
    highlights: form.highlights
      .filter(h => h.text.trim())
      .map(h => ({ kind: h.kind, text: h.text.trim() })),
    rankings: form.rankings
      .filter(r => r.source.trim() && r.rankPosition)
      .map(r => ({ source: r.source.trim(), rankPosition: Number(r.rankPosition), rankYear: numberOrNull(r.rankYear), note: textOrNull(r.note ?? '') })),
    links: form.links
      .filter(l => l.value.trim())
      .map(l => ({ kind: l.kind, value: l.value.trim() })),
    gallery: form.gallery
      .filter(g => g.mediaId != null || Boolean(g.imageUrl?.trim()))
      .slice(0, MAX_GALLERY)
      .map(g => ({
        mediaId: g.mediaId ?? null,
        imageUrl: g.imageUrl?.trim() || undefined,
        caption: textOrNull(g.caption ?? ''),
      })),
  }
}

export const universityFormKey: InjectionKey<UniversityForm> = Symbol('universityForm')

export function useUniversityForm(): UniversityForm {
  const form = inject(universityFormKey)
  if (!form) throw new Error('university form sections must be rendered inside UniversityDrawer')
  return form
}
