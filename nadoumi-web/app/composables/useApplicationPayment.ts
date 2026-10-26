import type { PaymentDocumentDto, PaymentDocumentKind, StudentPaymentDto } from '~/types/payment'

/** Which fee of an application: the one paid to submit it, or the service fee paid once admission is confirmed. */
export type FeeVariant = 'application' | 'service'

const FEE_PATH: Record<FeeVariant, string> = { application: 'payment', service: 'service-fee' }

/** The student's Nadoumi fee for one application, via the BFF proxy. */
export function useApplicationPayment(applicationId: MaybeRefOrGetter<number>, variant: FeeVariant = 'application') {
  const { studentFetch } = useApi()
  const base = () => `applications/${toValue(applicationId)}/${FEE_PATH[variant]}`

  return {
    overview: () => studentFetch<StudentPaymentDto>(base()),
    start: () => studentFetch<StudentPaymentDto>(`${base()}/session`, { method: 'POST' }),
    hold: () => studentFetch<StudentPaymentDto>(`${base()}/hold`, { method: 'POST' }),
    cancel: () => studentFetch<StudentPaymentDto>(`${base()}/cancel`, { method: 'POST' }),
    /** The QR is private to an open session, so it is fetched with the session cookie and shown from a blob URL. */
    qr: () => studentFetch<Blob>(`${base()}/qr`, { responseType: 'blob' }),
    documents: () => studentFetch<PaymentDocumentDto[]>(`${base()}/documents`),
    /** A PDF needs the session, so it is fetched as a blob and saved from an object URL. */
    document: (kind: PaymentDocumentKind) => studentFetch<Blob>(`${base()}/documents/${kind}`, { responseType: 'blob' }),
    /** Emails the receipt to the student again; the server allows it once every few minutes. */
    resendReceipt: () => studentFetch(`${base()}/documents/RECEIPT/resend`, { method: 'POST' }).then(() => undefined),
    submitProof: (transactionNo: string, file: File) => {
      const form = new FormData()
      form.append('transactionNo', transactionNo)
      form.append('file', file, file.name)
      return studentFetch<StudentPaymentDto>(`${base()}/proof`, { method: 'POST', body: form })
    },
  }
}
