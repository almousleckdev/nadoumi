// Student sign-in: exchange credentials at the Spring API, then keep the JWT in an
// httpOnly cookie. The token is never returned to the browser.
export default defineEventHandler(async (event) => {
  const body = await readBody<{ username: string; password: string; code?: string; uuid?: string }>(event)
  const res = await $fetch<{ token: string }>(`${backendBaseUrl(event)}/api/student/login`, {
    method: 'POST',
    body,
  })
  setStudentToken(event, res.token)
  return { signedIn: true }
})
