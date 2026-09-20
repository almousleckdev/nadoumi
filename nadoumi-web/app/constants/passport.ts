/** Limits and copy keys for the passport step, so no component hard-codes them. */
export const PASSPORT_MAX_MB = 10
export const PASSPORT_ACCEPT = 'image/jpeg,image/png,application/pdf'
export const PASSPORT_MIME = /^(image\/(jpeg|png)|application\/pdf)$/
export const PASSPORT_NUMBER_PATTERN = /^[A-Za-z0-9]{5,20}$/
export const PASSPORT_MIN_VALIDITY_MONTHS = 6

export const PHOTO_MAX_MB = 5
export const PHOTO_ACCEPT = 'image/jpeg,image/png,image/webp'
export const PHOTO_MIME = /^image\/(jpeg|png|webp)$/
export const PHOTO_MIN_PX = 400
