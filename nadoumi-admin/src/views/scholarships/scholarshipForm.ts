import { inject, type InjectionKey } from 'vue'
import { numberOrNull, textOrNull } from '@/utils/formValues'
import type {
  Scholarship, ScholarshipInput,
  EducationLevel, FeeKind, NationalityScope,
  RoomType, NonDegreeDuration, StipendFrequency,
  CoverageKind, ApplicationChannel,
} from '@/api/scholarship'

export function blankEligibility() {
  return {
    ageMin: null as number | null, ageMax: null as number | null,
    nationalityScope: 'ANY' as NationalityScope, acceptedCountries: '',
    inChina: null as boolean | null,
    gpaMin: null as number | null, ieltsMin: null as number | null, toeflMin: null as number | null,
    duolingoMin: null as number | null, hskMin: null as number | null, cscaMin: null as number | null,
    notes: '',
  }
}
export function blankForm() {
  return {
    title: '', summary: '', country: '', province: '', city: '', field: '',
    teachingLanguage: null as string | null,
    fundingModel: 'FULLY' as ScholarshipInput['fundingModel'],
    categoryCodes: [] as string[],
    levels: [] as EducationLevel[],
    benefits: '', requirements: '', policy: '', renewalConditions: '',
    applicationFeeAmount: null as number | null, applicationFeeCurrency: 'CNY',
    serviceFeeAmount: null as number | null, serviceFeeCurrency: 'CNY',
    slots: null as number | null,
    deadline: '' as string | null,
    nonDegreeDuration: null as NonDegreeDuration | null,
    studyDurationMonths: null as number | null,
    applicationChannel: null as ApplicationChannel | null,
    agencyNumber: '',
    requiresFinancialProof: false,
    requiresFoundationYear: false,
    coverage: [] as { kind: CoverageKind, detail: string }[],
    fees: [] as { kind: FeeKind, amount: number | null, currency: string, note: string }[],
    eligibility: blankEligibility(),
    levelStipends: [] as { level: EducationLevel, amount: number | null, currency: string, frequency: StipendFrequency, durationMonths: number | null, conditions: string }[],
    accommodations: [] as { roomType: RoomType, amount: number | null, currency: string, note: string }[],
    intakes: [] as { term: string, applicationOpen: string | null, applicationClose: string | null }[],
    documentRequirements: [] as { docType: string, mandatory: boolean, note: string }[],
    featured: false, recommended: false, hot: false,
    id: undefined as number | undefined,
    heroImageUrl: null as string | null, coverImageUrl: null as string | null,
    heroMediaId: null as number | null, coverMediaId: null as number | null,
    status: 'ACTIVE' as ScholarshipInput['status'],
    publishStatus: 'DRAFT' as ScholarshipInput['publishStatus'],
    remark: '',
  }
}

export type ScholarshipForm = ReturnType<typeof blankForm>

export function formFromScholarship(source: Scholarship): ScholarshipForm {
  const v = source.view
  return {
    ...blankForm(),
    title: v.title, summary: v.summary ?? '', country: v.country,
    province: v.province ?? '', city: v.city ?? '', field: v.field ?? '',
    teachingLanguage: v.teachingLanguage ?? null, fundingModel: v.fundingModel,
    categoryCodes: [...v.categories], levels: [...v.levels] as EducationLevel[],
    benefits: v.benefits ?? '', requirements: v.requirements ?? '', policy: v.policy ?? '',
    renewalConditions: v.renewalConditions ?? '',
    nonDegreeDuration: v.nonDegreeDuration ?? null,
    studyDurationMonths: v.studyDurationMonths ?? null,
    applicationChannel: v.applicationChannel ?? null,
    agencyNumber: v.agencyNumber ?? '',
    requiresFinancialProof: v.requiresFinancialProof,
    requiresFoundationYear: v.requiresFoundationYear,
    coverage: v.coverage.map(c => ({ kind: c.kind as CoverageKind, detail: c.detail ?? '' })),
    applicationFeeAmount: v.applicationFee?.amountRmb ?? null, applicationFeeCurrency: 'CNY',
    serviceFeeAmount: v.serviceFee?.amountRmb ?? null, serviceFeeCurrency: 'CNY',
    slots: v.slots ?? null, deadline: v.deadline ?? '',
    fees: v.fees.map(f => ({ kind: f.kind as FeeKind, amount: f.amountRmb, currency: 'CNY', note: f.note ?? '' })),
    eligibility: { ...blankEligibility(), ...(v.eligibility ?? {}), acceptedCountries: v.eligibility?.acceptedCountries ?? '', notes: v.eligibility?.notes ?? '', nationalityScope: (v.eligibility?.nationalityScope as NationalityScope) ?? 'ANY' },
    levelStipends: v.stipends.map(st => ({
      level: st.level as EducationLevel, amount: st.amountRmb, currency: 'CNY',
      frequency: st.frequency, durationMonths: st.durationMonths ?? null, conditions: st.conditions ?? '',
    })),
    accommodations: v.accommodation.map(a => ({
      roomType: a.roomType as RoomType, amount: a.amountRmb ?? null, currency: 'CNY', note: a.note ?? '',
    })),
    intakes: v.intakes.map(i => ({ term: i.term, applicationOpen: i.applicationOpen ?? null, applicationClose: i.applicationClose ?? null })),
    documentRequirements: v.documentRequirements.map(d => ({ docType: d.docType, mandatory: d.mandatory, note: d.note ?? '' })),
    featured: v.featured, recommended: v.recommended, hot: v.hot,
    id: v.id,
    heroImageUrl: v.heroImageUrl ?? null, coverImageUrl: v.coverImageUrl ?? null,
    heroMediaId: v.heroMediaId ?? null, coverMediaId: v.coverMediaId ?? null,
    status: source.status, publishStatus: source.publishStatus, remark: source.remark ?? '',
  }
}

export function buildPayload(form: ScholarshipForm): ScholarshipInput {
  const elig = form.eligibility
  const anyElig = elig.ageMin != null || elig.ageMax != null || elig.gpaMin != null || elig.ieltsMin != null
    || elig.toeflMin != null || elig.duolingoMin != null || elig.hskMin != null || elig.cscaMin != null
    || elig.nationalityScope !== 'ANY' || textOrNull(elig.notes) != null
  return {
    title: form.title.trim(),
    summary: textOrNull(form.summary),
    country: form.country.trim().toUpperCase(),
    province: textOrNull(form.province),
    city: textOrNull(form.city),
    field: textOrNull(form.field),
    teachingLanguage: (form.teachingLanguage as ScholarshipInput['teachingLanguage']) || null,
    fundingModel: form.fundingModel,
    hasStipend: form.levelStipends.some(st => st.amount != null),
    nonDegreeDuration: form.levels.includes('NON_DEGREE') ? form.nonDegreeDuration : null,
    studyDurationMonths: numberOrNull(form.studyDurationMonths),
    applicationChannel: form.applicationChannel || null,
    agencyNumber: form.applicationChannel === 'CSC_AGENCY' ? textOrNull(form.agencyNumber) : null,
    requiresFinancialProof: form.requiresFinancialProof,
    requiresFoundationYear: form.requiresFoundationYear,
    deadline: textOrNull(form.deadline ?? ''),
    benefits: textOrNull(form.benefits),
    requirements: textOrNull(form.requirements),
    policy: textOrNull(form.policy),
    renewalConditions: textOrNull(form.renewalConditions),
    applicationFeeAmount: numberOrNull(form.applicationFeeAmount),
    applicationFeeCurrency: form.applicationFeeAmount != null ? 'CNY' : null,
    serviceFeeAmount: numberOrNull(form.serviceFeeAmount),
    serviceFeeCurrency: form.serviceFeeAmount != null ? 'CNY' : null,
    slots: numberOrNull(form.slots),
    featured: form.featured, recommended: form.recommended, hot: form.hot,
    heroImageUrl: form.heroImageUrl, coverImageUrl: form.coverImageUrl,
    heroMediaId: form.heroMediaId, coverMediaId: form.coverMediaId,
    status: form.status, publishStatus: form.publishStatus, remark: textOrNull(form.remark),
    levels: [...form.levels],
    categoryCodes: [...form.categoryCodes],
    intakes: form.intakes.filter(i => i.term).map(i => ({
      term: i.term, applicationOpen: i.applicationOpen || null, applicationClose: i.applicationClose || null,
    })),
    eligibility: anyElig
      ? {
          ageMin: numberOrNull(elig.ageMin), ageMax: numberOrNull(elig.ageMax),
          nationalityScope: elig.nationalityScope,
          acceptedCountries: elig.nationalityScope === 'ANY' ? null : textOrNull(elig.acceptedCountries),
          inChina: elig.inChina,
          gpaMin: numberOrNull(elig.gpaMin), ieltsMin: numberOrNull(elig.ieltsMin), toeflMin: numberOrNull(elig.toeflMin),
          duolingoMin: numberOrNull(elig.duolingoMin), hskMin: numberOrNull(elig.hskMin), cscaMin: numberOrNull(elig.cscaMin),
          notes: textOrNull(elig.notes),
        }
      : null,
    fees: form.fees.filter(f => f.amount != null && f.kind).map(f => ({
      kind: f.kind, amount: Number(f.amount), currency: 'CNY', note: textOrNull(f.note),
    })),
    levelStipends: form.levelStipends.filter(st => st.amount != null && st.level).map(st => ({
      level: st.level, amount: Number(st.amount), currency: 'CNY',
      frequency: st.frequency, durationMonths: numberOrNull(st.durationMonths), conditions: textOrNull(st.conditions),
    })),
    accommodations: form.accommodations.filter(a => a.roomType).map(a => ({
      roomType: a.roomType, amount: numberOrNull(a.amount), currency: 'CNY', note: textOrNull(a.note),
    })),
    coverage: form.coverage.filter(c => c.kind).map(c => ({ kind: c.kind, detail: textOrNull(c.detail) })),
    documentRequirements: form.documentRequirements.filter(d => d.docType.trim()).map(d => ({
      docType: d.docType.trim().toUpperCase(), mandatory: d.mandatory, note: textOrNull(d.note),
    })),
  }
}

export const scholarshipFormKey: InjectionKey<ScholarshipForm> = Symbol('scholarshipForm')

export function useScholarshipForm(): ScholarshipForm {
  const form = inject(scholarshipFormKey)
  if (!form) throw new Error('scholarship form sections must be rendered inside ScholarshipDrawer')
  return form
}
