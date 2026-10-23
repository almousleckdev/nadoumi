/** `GET /api/public/documents/verify`: only whether Nadoumi issued the document, never who it is for. */
export interface DocumentVerificationDto {
  valid: boolean
  kind: 'RECEIPT' | 'APPLICATION_FORM' | null
  number: string | null
  issuedOn: string | null
}
