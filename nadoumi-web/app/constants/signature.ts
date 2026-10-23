/** Limits for the signature image; the server re-encodes it and rejects an empty one. */
export const SIGNATURE_MAX_MB = 2
export const SIGNATURE_ACCEPT = 'image/png,image/jpeg'
export const SIGNATURE_MIME = /^image\/(png|jpeg)$/
/** Drawing surface in CSS pixels; the canvas is scaled for sharp lines on dense screens. */
export const PAD_WIDTH = 480
export const PAD_HEIGHT = 160
export const PEN_WIDTH = 2.6
