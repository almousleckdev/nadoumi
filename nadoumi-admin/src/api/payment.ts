import request from '@/utils/request'
import type { Page } from './applicant'

export type PaymentStatus = 'PENDING' | 'PROOF_SUBMITTED' | 'CONFIRMED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED'

/** The only states staff receive: a reported payment on a submitted application, then the decision on it. */
export const PAYMENT_STATUSES: readonly PaymentStatus[] = ['PROOF_SUBMITTED', 'CONFIRMED', 'REJECTED']

export interface StaffPayment {
  id: number
  applicationId: number
  applicationRef: string
  applicantName: string
  kind: string
  amount: number
  currency: string
  reference: string
  status: PaymentStatus
  studentTxnNo: string | null
  hasProof: boolean
  createdAt: string
  submittedAt: string | null
  expiresAt: string
  verifiedAt: string | null
  rejectionReason: string | null
}

const P = '/api/staff/payments'

export const PAYMENT_KINDS = ['APPLICATION_FEE', 'SERVICE_FEE'] as const

export const listPayments = (params: { status?: string, q?: string, applicationId?: number, kind?: string, page?: number, size?: number }) =>
  request.get<unknown, Page<StaffPayment>>(P, { params })

export const getPayment = (id: number) => request.get<unknown, StaffPayment>(`${P}/${id}`)

/** Decision calls are `silent`: the caller shows the 403/409 detail itself. */
export const confirmPayment = (id: number) =>
  request.post<unknown, StaffPayment>(`${P}/${id}/confirm`, undefined, { silent: true } as object)

export const rejectPayment = (id: number, reason: string) =>
  request.post<unknown, StaffPayment>(`${P}/${id}/reject`, { reason }, { silent: true } as object)

/** The proof bytes; needs the session, so it is fetched as a Blob and shown via an object URL. */
export const fetchPaymentProof = (id: number) =>
  request.get<unknown, Blob>(`${P}/${id}/proof`, { responseType: 'blob' })

/** Permanent; the server refuses a confirmed payment. */
export const deletePayment = (id: number, reason: string) =>
  request.delete<unknown, void>(`${P}/${id}`, { data: { reason } })
