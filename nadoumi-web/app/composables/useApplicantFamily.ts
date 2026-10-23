import type {
  FamilyMemberDto, FormReadinessDto, MailingChoice, PersonalDetailsDto, PersonDto, PersonRole,
} from '~/types/family'

export interface PersonalDetailsBody {
  maritalStatus?: string; religion?: string; placeOfBirth?: string; officialLanguage?: string
  permanentAddress?: string; permanentPostcode?: string
  currentPostcode?: string; mailingChoice?: MailingChoice; mailingAddress?: string; mailingPostcode?: string
}
export interface FamilyMemberBody {
  relation: string; fullName: string; deceased: boolean
  dateOfBirth?: string; nationality?: string; note?: string; company?: string; position?: string
  companyAddress?: string; phone?: string; email?: string
}
export interface PersonBody {
  fullName: string; relationship: string; phone: string
  gender?: string; email?: string; position?: string; company?: string; companyAddress?: string; homeAddress?: string
}

/** The personal details, family, supporter and China contact of one applicant, via the BFF proxy. */
export function useApplicantFamily() {
  const { studentFetch } = useApi()
  const p = <T>(path: string, opts?: Parameters<typeof studentFetch>[1]) => studentFetch<T>(path, opts)

  return {
    getPersonal: (id: number) => p<PersonalDetailsDto | undefined>(`applicants/${id}/personal-details`).then(r => r ?? null),
    savePersonal: (id: number, body: PersonalDetailsBody) =>
      p<PersonalDetailsDto>(`applicants/${id}/personal-details`, { method: 'PUT', body }),

    listFamily: (id: number) => p<FamilyMemberDto[]>(`applicants/${id}/family`),
    addFamily: (id: number, body: FamilyMemberBody) => p<FamilyMemberDto>(`applicants/${id}/family`, { method: 'POST', body }),
    updateFamily: (id: number, memberId: number, body: FamilyMemberBody) =>
      p<FamilyMemberDto>(`applicants/${id}/family/${memberId}`, { method: 'PUT', body }),
    deleteFamily: (id: number, memberId: number) =>
      p(`applicants/${id}/family/${memberId}`, { method: 'DELETE' }).then(() => undefined),

    getPerson: (id: number, role: PersonRole) => p<PersonDto | undefined>(`applicants/${id}/people/${role}`).then(r => r ?? null),
    savePerson: (id: number, role: PersonRole, body: PersonBody) =>
      p<PersonDto>(`applicants/${id}/people/${role}`, { method: 'PUT', body }),

    readiness: (id: number) => p<FormReadinessDto>(`applicants/${id}/form-readiness`),
  }
}
