// Same-origin entry point for chat attachments. An <img> or a download link cannot send the bearer token and the
// browser never holds it, so this route asks the backend for a short-lived signed URL (which re-checks that the
// caller is in the conversation) and redirects the browser there.
const ID_PATTERN = /^\d{1,18}$/

export default defineEventHandler(async (event) => {
  const token = studentToken(event)
  if (!token) {
    throw createError({ statusCode: 401, statusMessage: 'Not signed in' })
  }
  const conversationId = getRouterParam(event, 'conversationId') ?? ''
  const attachmentId = getRouterParam(event, 'attachmentId') ?? ''
  if (!ID_PATTERN.test(conversationId) || !ID_PATTERN.test(attachmentId)) {
    throw createError({ statusCode: 400, statusMessage: 'Bad attachment reference' })
  }
  const download = getQuery(event).download === '1' ? '&download=1' : ''
  const url = `${backendBaseUrl(event)}/api/student/conversations/${conversationId}/attachments/${attachmentId}?json=1${download}`
  let access: { url?: string }
  try {
    access = await $fetch<{ url?: string }>(url, { headers: { authorization: `Bearer ${token}` } })
  }
  catch (e) {
    const status = (e as { statusCode?: number, status?: number }).statusCode ?? (e as { status?: number }).status ?? 502
    throw createError({ statusCode: status === 403 || status === 404 ? status : 502, statusMessage: 'Attachment unavailable' })
  }
  if (!access.url || !/^https:\/\//i.test(access.url)) {
    throw createError({ statusCode: 502, statusMessage: 'Attachment unavailable' })
  }
  setHeader(event, 'Cache-Control', 'private, no-store')
  return sendRedirect(event, access.url, 302)
})
