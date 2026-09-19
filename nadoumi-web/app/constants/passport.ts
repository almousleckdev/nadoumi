/** Limits and copy keys for the passport step, so no component hard-codes them. */
export const PASSPORT_MAX_MB = 10
export const PASSPORT_ACCEPT = 'image/jpeg,image/png,application/pdf'
export const PASSPORT_MIME = /^(image\/(jpeg|png)|application\/pdf)$/
export const PASSPORT_NUMBER_PATTERN = /^[A-Za-z0-9]{5,20}$/
export const PASSPORT_MIN_VALIDITY_MONTHS = 6

/** Where the self-hosted OCR engine lives (copied there by `scripts/copy-ocr-assets.mjs`). */
export const OCR_ASSET_BASE = '/vendor/ocr'

/** A crop of the passport page to OCR: the bottom `band` fraction, scaled to `width` pixels. */
export interface MrzPass { band: number, width: number }

/**
 * Passes tried in order until one validates. Each scale gives the OCR different mistakes and the
 * check digits accept only a fully correct read, so trying several raises the hit rate safely.
 */
export const MRZ_PASSES: readonly MrzPass[] = [
  { band: 0.3, width: 1200 },
  { band: 0.3, width: 900 },
  { band: 0.3, width: 1800 },
  { band: 0.45, width: 1200 },
  { band: 0.3, width: 2400 },
  { band: 1, width: 1800 },
]
export const MRZ_CHARSET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<'

export const PHOTO_MAX_MB = 5
export const PHOTO_ACCEPT = 'image/jpeg,image/png,image/webp'
export const PHOTO_MIME = /^image\/(jpeg|png|webp)$/
export const PHOTO_MIN_PX = 400
export const PHOTO_OUTPUT_TYPE = 'image/jpeg'

/** Where the in-browser passport reading stands. */
export type PassportReadState = 'idle' | 'reading' | 'read' | 'unreadable' | 'manual'

export type PassportErrorCode =
  | 'required' | 'nameInvalid' | 'numberInvalid' | 'issueFuture' | 'expiryBeforeIssue' | 'expiryTooSoon'

export const PASSPORT_ERROR_KEYS: Record<PassportErrorCode, string> = {
  required: 'validation.required',
  nameInvalid: 'profileForm.nameInvalid',
  numberInvalid: 'passport.errors.numberInvalid',
  issueFuture: 'passport.errors.issueFuture',
  expiryBeforeIssue: 'passport.errors.expiryBeforeIssue',
  expiryTooSoon: 'passport.errors.expiryTooSoon',
}

/** Form fields the reader fills in; a difference from these means the student edited the reading. */
export const PASSPORT_READ_FIELDS = ['passportNo', 'givenName', 'familyName', 'dob', 'expiryDate'] as const

/** The profile fields a passport is compared with, in display order. */
export const PASSPORT_COMPARED_FIELDS = ['givenName', 'familyName', 'dob'] as const
export type PassportComparedField = typeof PASSPORT_COMPARED_FIELDS[number]
