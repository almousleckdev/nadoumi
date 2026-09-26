/** Mirrors the backend MESSAGE_ATTACHMENT policy (photo or document, 15 MB); the server still enforces it. */
export const MESSAGE_ATTACHMENT_MAX_MB = 15
export const MESSAGE_ATTACHMENT_ACCEPT =
  'image/jpeg,image/png,image/webp,application/pdf,application/msword,'
  + 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
export const MESSAGE_ATTACHMENT_MIME =
  /^(image\/(jpeg|png|webp)|application\/pdf|application\/msword|application\/vnd\.openxmlformats-officedocument\.wordprocessingml\.document)$/

export function isImageAttachment(contentType: string | null): boolean {
  return Boolean(contentType && /^image\//.test(contentType))
}
