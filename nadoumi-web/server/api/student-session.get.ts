import type { SessionDto } from '~/types/catalog'

export default defineEventHandler(async (event): Promise<SessionDto> => {
  const token = studentToken(event)
  if (!token) return { authenticated: false }

  try {
    const me = await $fetch<{
      userId: number; username: string; nickName: string | null
      accessibleApplicants: { applicantId: number; accessRole: string; capabilities: string[] }[]
    }>(`${backendBaseUrl(event)}/api/student/me`, {
      headers: { authorization: `Bearer ${token}` },
    })
    return {
      authenticated: true,
      user: { userId: me.userId, username: me.username, nickName: me.nickName },
      applicants: me.accessibleApplicants,
    }
  }
  catch (err) {
    if ((err as { statusCode?: number }).statusCode === 401) {
      clearStudentToken(event)
      return { authenticated: false }
    }
    throw err
  }
})
