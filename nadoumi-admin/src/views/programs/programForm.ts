import { inject, type InjectionKey } from 'vue'
import {
  isDegree as isDegreeKind,
  type DegreeLevel, type Program, type ProgramInput, type TermLength,
} from '@/api/program'
import { numberOrNull, textOrNull } from '@/utils/formValues'

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
    durationMonths: null as number | null,
    tuitionAmount: null as number | null,
    tuitionCurrency: 'CNY',
    summary: '',
    imageMediaId: null as number | null,
    majors: [] as { name: string, nameCn: string | null, departmentId: number | null, level: DegreeLevel | null }[],
    intakes: [] as { term: string, applicationOpen: string | null, applicationClose: string | null }[],
    featured: false, hot: false,
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
    durationMonths: p.durationMonths ?? null,
    tuitionAmount: p.tuitionAmount ?? null,
    tuitionCurrency: p.tuitionCurrency ?? 'CNY',
    summary: p.summary ?? '',
    imageMediaId: p.imageMediaId ?? null,
    majors: p.majors.map(m => ({
      name: m.name, nameCn: m.nameCn ?? null, departmentId: m.departmentId ?? null, level: m.level ?? null,
    })),
    intakes: p.intakes.map(i => ({
      term: i.term, applicationOpen: i.applicationOpen ?? null, applicationClose: i.applicationClose ?? null,
    })),
    featured: p.featured, hot: p.hot,
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
    durationMonths: numberOrNull(form.durationMonths),
    tuitionAmount: numberOrNull(form.tuitionAmount),
    tuitionCurrency: form.tuitionAmount != null ? 'CNY' : null,
    summary: textOrNull(form.summary),
    imageMediaId: form.imageMediaId,
    featured: form.featured, hot: form.hot,
    status: form.status, publishStatus: form.publishStatus, remark: textOrNull(form.remark),
    majors: isDegree
      ? form.majors.filter(m => m.name.trim()).map(m => ({
          name: m.name.trim(), nameCn: textOrNull(m.nameCn ?? ''), departmentId: m.departmentId ?? null,
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
