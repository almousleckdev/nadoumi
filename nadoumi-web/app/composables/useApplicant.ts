import type {
  ApplicantDto, ContactDto, EducationDto, InterestDto, OnboardingStatusDto, PassportStatusDto, ResidenceDto,
  TestScoreDto, WorkDto,
} from '~/types/catalog'

export interface SelfApplicantBody {
  givenName: string; familyName: string
  dob?: string; nationality?: string; passportNo?: string; email?: string; phone?: string
  gender?: string; countryOfOrigin?: string; countryOfResidence?: string; nativeLanguage?: string
  wechatId?: string; whatsapp?: string
}
export interface PassportBody {
  passportNo: string; givenName: string; familyName: string
  dob: string; issueDate: string; expiryDate: string
  readMethod: 'MRZ' | 'MANUAL'; edited: boolean
}
export interface SignedFileUrl { url: string; expiresAt: string }
export interface EmailCodeSent { sent: boolean; throttled: boolean; retryAfter: number }
export interface EducationBody {
  institution: string; country: string; level: string; city?: string; qualification?: string; field?: string
  gpa?: number; gpaScale?: number; startDate?: string; endDate?: string; current: boolean
}
export interface InterestBody {
  desiredLevel: string; fields: string[]; cities: string[]
  scholarshipInterest?: string; intakeYear?: number; intakeTerm?: string; teachingLanguage?: string; notes?: string
}
export interface ResidenceBody {
  inChina: boolean; country: string; city: string; address?: string
  chinaEducationLevel?: string; chinaSchool?: string; visaType?: string; visaExpiryDate?: string
}
export interface WorkBody {
  employer: string; jobTitle: string; country: string; startDate: string; current: boolean
  employmentType?: string; city?: string; endDate?: string; description?: string
  workVisaType?: string; workVisaExpiry?: string
}
export interface TestScoreBody {
  testType: string; score: string; subScoresJson?: string; takenOn?: string; expiresOn?: string
}
export interface ContactBody {
  relation: string; name: string; email?: string; phone?: string
}

function clean<T extends object>(body: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(body).filter(([, v]) => v !== undefined && v !== '' && v !== null),
  ) as Partial<T>
}

function fileForm(file: Blob, filename: string): FormData {
  const form = new FormData()
  form.append('file', file, filename)
  return form
}

export function useApplicant() {
  const { studentFetch } = useApi()
  const p = <T>(path: string, opts?: Parameters<typeof studentFetch>[1]) => studentFetch<T>(path, opts)

  return {
    listMine: () => p<ApplicantDto[]>('applicants', undefined),
    create: (b: SelfApplicantBody) => p<ApplicantDto>('applicants', { method: 'POST', body: clean(b) }),
    get: (id: number) => p<ApplicantDto>(`applicants/${id}`, undefined),
    update: (id: number, b: SelfApplicantBody) => p<ApplicantDto>(`applicants/${id}`, { method: 'PUT', body: clean(b) }),

    requestEmailCode: (id: number, email: string) =>
      p<EmailCodeSent>(`applicants/${id}/email/otp`, { method: 'POST', body: { email } }),
    verifyEmail: (id: number, email: string, otp: string) =>
      p<ApplicantDto>(`applicants/${id}/email/verify`, { method: 'POST', body: { email, otp } }),

    uploadPhoto: (id: number, file: Blob) => p<{ mediaId: number }>(`applicants/${id}/photo`, { method: 'POST', body: fileForm(file, 'photo.jpg') }),
    photoUrl: (id: number) => p<SignedFileUrl>(`applicants/${id}/photo`, { query: { json: 1 } }),
    uploadPassportScan: (id: number, file: File) => p<{ mediaId: number }>(`applicants/${id}/passport/scan`, { method: 'POST', body: fileForm(file, file.name) }),
    passportScanUrl: (id: number) => p<SignedFileUrl>(`applicants/${id}/passport/scan`, { query: { json: 1 } }),
    passportStatus: (id: number) => p<PassportStatusDto>(`applicants/${id}/passport`, undefined),
    savePassport: (id: number, b: PassportBody) => p<PassportStatusDto>(`applicants/${id}/passport`, { method: 'PUT', body: b }),

    /** 204 (nothing saved yet) reads as null. */
    getInterests: (id: number) => p<InterestDto | undefined>(`applicants/${id}/interests`, undefined).then(r => r ?? null),
    saveInterests: (id: number, b: InterestBody) => p<InterestDto>(`applicants/${id}/interests`, { method: 'PUT', body: clean(b) }),
    getResidence: (id: number) => p<ResidenceDto | undefined>(`applicants/${id}/residence`, undefined).then(r => r ?? null),
    saveResidence: (id: number, b: ResidenceBody) => p<ResidenceDto>(`applicants/${id}/residence`, { method: 'PUT', body: clean(b) }),

    listWork: (id: number) => p<WorkDto[]>(`applicants/${id}/work`, undefined),
    addWork: (id: number, b: WorkBody) => p<WorkDto>(`applicants/${id}/work`, { method: 'POST', body: clean(b) }),
    updateWork: (id: number, workId: number, b: WorkBody) => p<WorkDto>(`applicants/${id}/work/${workId}`, { method: 'PUT', body: clean(b) }),
    deleteWork: (id: number, workId: number): Promise<void> => p<undefined>(`applicants/${id}/work/${workId}`, { method: 'DELETE' }),

    onboardingStatus: (id: number) => p<OnboardingStatusDto>(`applicants/${id}/onboarding`, undefined),
    completeOnboarding: (id: number) =>
      p<OnboardingStatusDto>(`applicants/${id}/onboarding/complete`, { method: 'POST' }),
    markWelcomed: (id: number): Promise<void> => p<undefined>(`applicants/${id}/onboarding/welcomed`, { method: 'POST' }),

    listEducation: (id: number) => p<EducationDto[]>(`applicants/${id}/education`, undefined),
    addEducation: (id: number, b: EducationBody) => p<EducationDto>(`applicants/${id}/education`, { method: 'POST', body: clean(b) }),
    updateEducation: (id: number, eduId: number, b: EducationBody) => p<EducationDto>(`applicants/${id}/education/${eduId}`, { method: 'PUT', body: clean(b) }),
    deleteEducation: (id: number, eduId: number): Promise<void> => p<undefined>(`applicants/${id}/education/${eduId}`, { method: 'DELETE' }),

    listTestScores: (id: number) => p<TestScoreDto[]>(`applicants/${id}/test-scores`, undefined),
    addTestScore: (id: number, b: TestScoreBody) => p<TestScoreDto>(`applicants/${id}/test-scores`, { method: 'POST', body: clean(b) }),
    deleteTestScore: (id: number, scoreId: number): Promise<void> => p<undefined>(`applicants/${id}/test-scores/${scoreId}`, { method: 'DELETE' }),

    listContacts: (id: number) => p<ContactDto[]>(`applicants/${id}/contacts`, undefined),
    addContact: (id: number, b: ContactBody) => p<ContactDto>(`applicants/${id}/contacts`, { method: 'POST', body: clean(b) }),
    updateContact: (id: number, contactId: number, b: ContactBody) => p<ContactDto>(`applicants/${id}/contacts/${contactId}`, { method: 'PUT', body: clean(b) }),
    deleteContact: (id: number, contactId: number): Promise<void> => p<undefined>(`applicants/${id}/contacts/${contactId}`, { method: 'DELETE' }),
  }
}
