import type { SignatureStatusDto } from '~/types/family'

/** The student's handwritten signature (`/api/student/applicants/{id}/signature`, via the BFF proxy). */
export function useApplicantSignature(applicantId: MaybeRefOrGetter<number>) {
  const { studentFetch } = useApi()
  const base = () => `applicants/${toValue(applicantId)}/signature`

  return {
    status: () => studentFetch<SignatureStatusDto>(base()),
    /** The image is private, so it is fetched with the session and shown from an object URL. */
    image: () => studentFetch<Blob>(`${base()}/image`, { responseType: 'blob' }),
    save: (png: Blob) => {
      const form = new FormData()
      form.append('file', png, 'signature.png')
      return studentFetch<SignatureStatusDto>(base(), { method: 'POST', body: form })
    },
  }
}
