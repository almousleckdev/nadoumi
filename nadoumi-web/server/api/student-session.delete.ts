// Student sign-out: best-effort tell the API, then drop the cookie.
export default defineEventHandler(async (event) => {
  const token = studentToken(event)
  if (token) {
    await $fetch<unknown>(`${backendBaseUrl(event)}/api/student/logout`, {
      method: 'POST',
      headers: { authorization: `Bearer ${token}` },
    }).catch(() => undefined)
  }
  clearStudentToken(event)
  return { signedIn: false }
})
