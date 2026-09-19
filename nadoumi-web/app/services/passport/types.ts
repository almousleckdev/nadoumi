/** Holder data read from a passport's machine-readable zone. Dates are ISO (yyyy-mm-dd). */
export interface PassportReading {
  documentNumber: string
  surname: string
  givenNames: string
  dateOfBirth: string
  expiryDate: string
  issuingCountry: string | null
  nationality: string | null
}

/**
 * Reads passport details from a scanned page. Implementations are swappable: the
 * browser MRZ reader ships first; a server-side reader can replace it without
 * touching the onboarding flow. `null` means "could not read it", never a guess.
 */
export interface PassportReader {
  read: (image: Blob) => Promise<PassportReading | null>
}
