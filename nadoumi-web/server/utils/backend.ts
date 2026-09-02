import type { H3Event } from 'h3'

/** Base URL of the Spring API, from runtime config (NADOUMI_API_URL). */
export function backendBaseUrl(event: H3Event): string {
  return useRuntimeConfig(event).backendBaseUrl
}

const STUDENT_COOKIE = 'nad_student_token'

/** The student JWT held in the httpOnly BFF cookie, if the caller is signed in. */
export function studentToken(event: H3Event): string | undefined {
  return getCookie(event, STUDENT_COOKIE)
}

export function setStudentToken(event: H3Event, token: string): void {
  setCookie(event, STUDENT_COOKIE, token, {
    httpOnly: true,
    secure: true,
    sameSite: 'lax',
    path: '/',
    maxAge: 60 * 30,
  })
}

export function clearStudentToken(event: H3Event): void {
  deleteCookie(event, STUDENT_COOKIE, { path: '/' })
}
