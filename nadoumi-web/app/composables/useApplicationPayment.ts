import type { PaymentDocumentDto, PaymentDocumentKind, StudentPaymentDto } from '~/types/payment'

/** The student's Nadoumi application fee for one application, via the BFF proxy. */
export function useApplicationPayment(applicationId: number) {
  const { studentFetch } = useApi()
  const base = `applications/${applicationId}/payment`

  return {
    overview: () => studentFetch<StudentPaymentDto>(base),
    start: () => studentFetch<StudentPaymentDto>(`${base}/session`, { method: 'POST' }),
    cancel: () => studentFetch<StudentPaymentDto>(`${base}/cancel`, { method: 'POST' }),
    /** The QR is private to an open session, so it is fetched with the session cookie and shown from a blob URL. */
    qr: () => studentFetch<Blob>(`${base}/qr`, { responseType: 'blob' }),
    documents: () => studentFetch<PaymentDocumentDto[]>(`${base}/documents`),
    /** A PDF needs the session, so it is fetched as a blob and saved from an object URL. */
    document: (kind: PaymentDocumentKind) => studentFetch<Blob>(`${base}/documents/${kind}`, { responseType: 'blob' }),
    submitProof: (transactionNo: string, file: File) => {
      const form = new FormData()
      form.append('transactionNo', transactionNo)
      form.append('file', file, file.name)
      return studentFetch<StudentPaymentDto>(`${base}/proof`, { method: 'POST', body: form })
    },
  }
}
