/** `PENDING` is an open QR session; the others are the states a payment can rest in. */
export type PaymentState =
  | 'NOT_REQUIRED' | 'NOT_STARTED' | 'PENDING' | 'PROOF_SUBMITTED'
  | 'CONFIRMED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED'

/** `/api/student/applications/{id}/payment`: the Nadoumi application fee of one application. */
export interface StudentPaymentDto {
  state: PaymentState
  required: boolean
  /** True once the fee is confirmed (or no fee is configured): the application may be submitted. */
  settled: boolean
  /** A new payment session may be opened. */
  canStart: boolean
  amount: number
  currency: string
  payee: string
  reference: string | null
  /** ISO-8601 UTC end of the payment window. */
  expiresAt: string | null
  /** Seconds left in the window, computed by the server; zero unless the state is `PENDING`. */
  secondsLeft: number
  rejectionReason: string | null
}

export type PaymentDocumentKind = 'RECEIPT' | 'APPLICATION_FORM'

/** A generated PDF the student can download once the fee is confirmed. */
export interface PaymentDocumentDto {
  kind: PaymentDocumentKind
  number: string
  version: number
  generatedAt: string
  sizeBytes: number
}
