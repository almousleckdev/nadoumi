/** Mirrors the backend MESSAGE_ATTACHMENT policy (photo or document, 15 MB); the server still enforces it. */
export const MESSAGE_ATTACHMENT_MAX_MB = 15
export const MESSAGE_ATTACHMENT_ACCEPT = [
  'image/jpeg', 'image/png', 'image/webp', 'application/pdf', 'text/plain',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
].join(',')
export const MESSAGE_ATTACHMENT_MIME = new RegExp(
  '^(image/(jpeg|png|webp)|application/pdf|text/plain|application/msword'
  + '|application/vnd\\.openxmlformats-officedocument\\.(wordprocessingml\\.document|spreadsheetml\\.sheet|presentationml\\.presentation))$',
)
