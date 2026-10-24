/** `GET /api/public/documents/verify`: whether Nadoumi issued the document and, when it did, what the QR code may show. */
export interface DocumentVerificationDto {
  valid: boolean
  kind: 'RECEIPT' | 'APPLICATION_FORM' | null
  number: string | null
  issuedOn: string | null
  details: VerifiedDocumentDetails | null
}

export interface VerifiedDocumentDetails {
  applicantName: string
  applicationReference: string | null
  opportunity: string | null
  universityName: string | null
  intake: string | null
  feeAmount: number | null
  currency: string | null
  feeStatus: 'PENDING' | 'PROOF_SUBMITTED' | 'CONFIRMED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED'
  feePaidAt: string | null
  issuedAt: string | null
}
