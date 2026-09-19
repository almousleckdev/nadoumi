import type {
  ApplicantDto, ContactDto, EducationDto, OnboardingStatusDto, PassportStatusDto, TestScoreDto,
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
  institution: string; level?: string; field?: string
  gpa?: number; gpaScale?: number; startDate?: string; endDate?: string
}
export interface TestScoreBody {
  testType: string; score: string; subScoresJson?: string; takenOn?: string; expiresOn?: string
}
export interface ContactBody {
  relation: 'GUARDIAN' | 'EMERGENCY' | 'OTHER'; name: string; email?: string; phone?: string
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

    onboardingStatus: (id: number) => p<OnboardingStatusDto>(`applicants/${id}/onboarding`, undefined),
    completeOnboarding: (id: number) =>
      p<OnboardingStatusDto>(`applicants/${id}/onboarding/complete`, { method: 'POST' }),

    listEducation: (id: number) => p<EducationDto[]>(`applicants/${id}/education`, undefined),
    addEducation: (id: number, b: EducationBody) => p<EducationDto>(`applicants/${id}/education`, { method: 'POST', body: clean(b) }),
    updateEducation: (id: number, eduId: number, b: EducationBody) => p<EducationDto>(`applicants/${id}/education/${eduId}`, { method: 'PUT', body: clean(b) }),
    deleteEducation: (id: number, eduId: number): Promise<void> => p<undefined>(`applicants/${id}/education/${eduId}`, { method: 'DELETE' }),

    listTestScores: (id: number) => p<TestScoreDto[]>(`applicants/${id}/test-scores`, undefined),
    addTestScore: (id: number, b: TestScoreBody) => p<TestScoreDto>(`applicants/${id}/test-scores`, { method: 'POST', body: clean(b) }),
    deleteTestScore: (id: number, scoreId: number): Promise<void> => p<undefined>(`applicants/${id}/test-scores/${scoreId}`, { method: 'DELETE' }),

    listContacts: (id: number) => p<ContactDto[]>(`applicants/${id}/contacts`, undefined),
    addContact: (id: number, b: ContactBody) => p<ContactDto>(`applicants/${id}/contacts`, { method: 'POST', body: clean(b) }),
    deleteContact: (id: number, contactId: number): Promise<void> => p<undefined>(`applicants/${id}/contacts/${contactId}`, { method: 'DELETE' }),
  }
}
