/**
 * Signed-in student changing their own sign-in email: request a code to the new
 * address, then confirm it together with the current password. Mirrors the shape
 * of `useOtp`, but the second step applies the change directly rather than
 * returning a ticket for a later step.
 */
export function useEmailChange() {
  const { seconds: cooldown, start: startCooldown } = useCooldown()
  const requestFetch = useRequestFetch()
  const busy = ref(false)

  async function requestCode(newEmail: string): Promise<{ throttled: boolean, retryAfter: number }> {
    busy.value = true
    try {
      const res = await requestFetch<{ sent?: boolean, throttled?: boolean, retryAfter?: number, msg?: string }>(
        '/api/student-email-change-otp', { method: 'POST', body: { newEmail } })
      if (res.sent !== true) {
        throw { statusCode: 400, data: { detail: res.msg || 'the verification code could not be sent' } }
      }
      const retryAfter = res.retryAfter && res.retryAfter > 0 ? res.retryAfter : 60
      startCooldown(retryAfter)
      return { throttled: Boolean(res.throttled), retryAfter }
    }
    finally {
      busy.value = false
    }
  }

  async function confirm(newEmail: string, otp: string, currentPassword: string): Promise<void> {
    busy.value = true
    try {
      await requestFetch('/api/student-email-change', { method: 'POST', body: { newEmail, otp, currentPassword } })
    }
    finally {
      busy.value = false
    }
  }

  return { requestCode, confirm, cooldown, busy }
}
