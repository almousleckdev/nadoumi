import { inject, type InjectionKey } from 'vue'
import {
  DURATION_OPTIONS, MIN_DURATION_MONTHS, isDegree as isDegreeKind,
  type AccommodationPeriod, type DegreeLevel, type Program, type ProgramInput, type TermLength, type TuitionPeriod,
} from '@/api/program'
import { numberOrNull, textOrNull } from '@/utils/formValues'

export interface OfferingRow {
  level: DegreeLevel | null
  durationMonths: number | null
  tuitionAmount: number | null
  tuitionPeriod: TuitionPeriod
  accommodationAmount: number | null
  accommodationPeriod: AccommodationPeriod
  note: string
}

/** A new offering row, starting at the shortest typical duration for its level. */
export function blankOffering(level: DegreeLevel | null): OfferingRow {
  const options = level ? DURATION_OPTIONS[level] : DURATION_OPTIONS.OTHER
  const fallback = level ? MIN_DURATION_MONTHS[level] : 12
  return {
    level,
    durationMonths: level ? options[0] ?? fallback : 12,
    tuitionAmount: null, tuitionPeriod: 'PER_YEAR',
    accommodationAmount: null, accommodationPeriod: 'PER_MONTH',
    note: '',
  }
}

export type OfferingProblem = { kind: 'TOO_SHORT' | 'MISSING_DURATION' | 'DUPLICATE', level: DegreeLevel | null, min: number }

/** Mirrors the server's checks so the form can explain a problem before saving. */
export function offeringProblems(form: Pick<ProgramForm, 'offerings'>): OfferingProblem[] {
  const problems: OfferingProblem[] = []
  const seen = new Set<string>()
  for (const o of form.offerings) {
    if (o.durationMonths == null) {
      problems.push({ kind: 'MISSING_DURATION', level: o.level, min: 0 })
      continue
    }
    const min = o.level ? MIN_DURATION_MONTHS[o.level] : 0
    if (o.durationMonths < min) problems.push({ kind: 'TOO_SHORT', level: o.level, min })
    const key = `${o.level}|${o.durationMonths}`
    if (seen.has(key)) problems.push({ kind: 'DUPLICATE', level: o.level, min: 0 })
    seen.add(key)
  }
  return problems
}

/**
 * Degree programmes get one offering row per selected level; rows of a level that was unticked go away.
 * Rows already filled in are kept untouched. Other programme kinds manage their rows by hand.
 */
export function syncOfferingsToLevels(form: Pick<ProgramForm, 'programType' | 'levels' | 'offerings'>): void {
  if (!isDegreeKind(form.programType)) return
  form.offerings = form.offerings.filter(o => o.level != null && form.levels.includes(o.level))
  for (const level of form.levels) {
    if (!form.offerings.some(o => o.level === level)) form.offerings.push(blankOffering(level))
  }
  const order = (l: DegreeLevel | null) => (l ? form.levels.indexOf(l) : -1)
  form.offerings.sort((a, b) => order(a.level) - order(b.level))
}

export function blankProgramForm(lockedUniversityId: number | null) {
  return {
    id: undefined as number | undefined,
    universityId: lockedUniversityId,
    name: '', nameCn: '',
    programType: 'DEGREE' as ProgramInput['programType'],
    levels: [] as DegreeLevel[],
    field: '',
    termLength: null as TermLength | null,
    teachingLanguage: null as ProgramInput['teachingLanguage'],
    summary: '',
    admissionRequirements: '', applicationDocuments: '', careerProspects: '', scholarshipInfo: '',
    scholarshipCategories: [] as string[],
    minHsk: null as number | null, minIelts: null as number | null, minToefl: null as number | null,
    applicationFeeAmount: null as number | null,
    offerings: [] as OfferingRow[],
    /** Saved through its own endpoint after the programme itself, so it is not part of `buildProgramPayload`. */
    documentRequirements: [] as { docType: string, mandatory: boolean, notes: string, appliesWhen: string }[],
    imageMediaId: null as number | null,
    majors: [] as { majorId: number | null, name: string, nameCn: string | null, departmentId: number | null, level: DegreeLevel | null }[],
    intakes: [] as { term: string, applicationOpen: string | null, applicationClose: string | null }[],
    featured: false, hot: false, recommended: false,
    status: 'ACTIVE' as ProgramInput['status'],
    publishStatus: 'DRAFT' as ProgramInput['publishStatus'],
    remark: '',
  }
}

export type ProgramForm = ReturnType<typeof blankProgramForm>

export function formFromProgram(p: Program): ProgramForm {
  return {
    ...blankProgramForm(null),
    id: p.id,
    universityId: p.universityId,
    name: p.name, nameCn: p.nameCn ?? '',
    programType: p.programType,
    levels: [...(p.levels ?? [])],
    field: p.field ?? '',
    termLength: p.termLength ?? null,
    teachingLanguage: p.teachingLanguage ?? null,
    summary: p.summary ?? '',
    admissionRequirements: p.admissionRequirements ?? '', applicationDocuments: p.applicationDocuments ?? '',
    careerProspects: p.careerProspects ?? '', scholarshipInfo: p.scholarshipInfo ?? '',
    scholarshipCategories: [...(p.scholarshipCategories ?? [])],
    minHsk: p.minHsk ?? null, minIelts: p.minIelts ?? null, minToefl: p.minToefl ?? null,
    applicationFeeAmount: p.applicationFeeAmount ?? null,
    offerings: (p.offerings ?? []).map(o => ({
      level: o.level ?? null, durationMonths: o.durationMonths,
      tuitionAmount: o.tuitionAmount ?? null, tuitionPeriod: o.tuitionPeriod ?? 'PER_YEAR',
      accommodationAmount: o.accommodationAmount ?? null, accommodationPeriod: o.accommodationPeriod ?? 'PER_MONTH',
      note: o.note ?? '',
    })),
    imageMediaId: p.imageMediaId ?? null,
    majors: p.majors.map(m => ({
      majorId: m.majorId ?? null, name: m.name, nameCn: m.nameCn ?? null, departmentId: m.departmentId ?? null,
      level: m.level ?? null,
    })),
    intakes: p.intakes.map(i => ({
      term: i.term, applicationOpen: i.applicationOpen ?? null, applicationClose: i.applicationClose ?? null,
    })),
    featured: p.featured, hot: p.hot, recommended: p.recommended ?? false,
    status: p.status, publishStatus: p.publishStatus, remark: p.remark ?? '',
  }
}

export function buildProgramPayload(form: ProgramForm): ProgramInput {
  const isDegree = isDegreeKind(form.programType)
  return {
    universityId: form.universityId as number,
    name: form.name.trim(),
    nameCn: textOrNull(form.nameCn),
    programType: form.programType,
    levels: isDegree ? [...form.levels] : [],
    field: textOrNull(form.field),
    termLength: isDegree ? null : (form.termLength || null),
    teachingLanguage: form.teachingLanguage || null,
    summary: textOrNull(form.summary),
    admissionRequirements: textOrNull(form.admissionRequirements),
    applicationDocuments: textOrNull(form.applicationDocuments),
    careerProspects: textOrNull(form.careerProspects),
    scholarshipInfo: textOrNull(form.scholarshipInfo),
    scholarshipCategories: [...form.scholarshipCategories],
    minHsk: numberOrNull(form.minHsk), minIelts: numberOrNull(form.minIelts), minToefl: numberOrNull(form.minToefl),
    applicationFeeAmount: numberOrNull(form.applicationFeeAmount),
    applicationFeeCurrency: form.applicationFeeAmount != null ? 'CNY' : null,
    offerings: form.offerings.filter(o => o.durationMonths != null).map(o => ({
      level: isDegree ? o.level : null,
      durationMonths: o.durationMonths as number,
      tuitionAmount: numberOrNull(o.tuitionAmount),
      tuitionCurrency: o.tuitionAmount != null ? 'CNY' : null,
      tuitionPeriod: o.tuitionPeriod,
      accommodationAmount: numberOrNull(o.accommodationAmount),
      accommodationCurrency: o.accommodationAmount != null ? 'CNY' : null,
      accommodationPeriod: o.accommodationPeriod,
      note: textOrNull(o.note),
    })),
    imageMediaId: form.imageMediaId,
    featured: form.featured, hot: form.hot, recommended: form.recommended,
    status: form.status, publishStatus: form.publishStatus, remark: textOrNull(form.remark),
    majors: isDegree
      ? form.majors.filter(m => m.majorId != null || m.name.trim()).map(m => ({
          // a picked major sends only its id (the catalogue supplies the title); a typed one is added to the catalogue
          majorId: m.majorId ?? null,
          name: m.majorId != null ? '' : m.name.trim(),
          nameCn: m.majorId != null ? null : textOrNull(m.nameCn ?? ''),
          departmentId: m.departmentId ?? null,
          level: m.level ?? null,
        }))
      : [],
    intakes: form.intakes.filter(i => i.term).map(i => ({
      term: i.term, applicationOpen: i.applicationOpen || null, applicationClose: i.applicationClose || null,
    })),
  }
}

export const programFormKey: InjectionKey<ProgramForm> = Symbol('programForm')

export function useProgramForm(): ProgramForm {
  const form = inject(programFormKey)
  if (!form) throw new Error('program form sections must be rendered inside ProgramDrawer')
  return form
}
