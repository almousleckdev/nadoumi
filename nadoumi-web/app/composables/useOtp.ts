export type OtpPurpose = 'REGISTER' | 'PASSWORD_RESET'

/**
 * Drives the shared email-verification step: request a code, count down the resend
 * cooldown, exchange a code for a single-use ticket. Used by `/register` and
 * `/forgot-password` through `<EmailVerifyStep>` — one implementation, no
 * duplicated OTP logic.
 */
export function useOtp() {
  const cooldown = ref(0)
  const busy = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  function stopTimer() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function startCooldown(seconds = 60) {
    cooldown.value = seconds
    stopTimer()
    timer = setInterval(() => {
      cooldown.value -= 1
      if (cooldown.value <= 0) stopTimer()
    }, 1000)
  }

  if (getCurrentScope()) onScopeDispose(stopTimer)

  /**
   * Ask the backend to email a code. The backend answers 200 whether or not a
   * mail actually went out: `throttled` is true when the request fell inside the
   * 60s resend window (no new mail), and `retryAfter` is the seconds until the
   * next request is allowed — used to size the cooldown exactly.
   *
   * `captcha` must be supplied when the server has the login captcha enabled
   * (production does). Without it the backend rejects the call and no mail is
   * ever generated.
   */
  async function request(
    email: string,
    purpose: OtpPurpose,
    captcha?: { code: string, uuid: string },
  ): Promise<{ throttled: boolean, retryAfter: number }> {
    busy.value = true
    try {
      const body: Record<string, string> = { email, purpose }
      if (captcha?.code) {
        body.code = captcha.code
        body.uuid = captcha.uuid
      }
      const res = await $fetch<{ sent?: boolean, throttled?: boolean, retryAfter?: number, msg?: string, code?: number }>(
        '/api/student-email-otp', { method: 'POST', body })
      // The endpoint answers HTTP 200 both for its own success body AND for the
      // framework's `{ code, msg }` error envelope (e.g. a missing/expired
      // captcha). Anything that is not an explicit `{ sent: true }` is a
      // failure — surface it instead of pretending a mail went out.
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

  async function verify(email: string, purpose: OtpPurpose, otp: string): Promise<string> {
    busy.value = true
    try {
      const res = await $fetch<{ ticket: string }>('/api/student-email-otp-verify', {
        method: 'POST',
        body: { email, purpose, otp },
      })
      return res.ticket
    }
    finally {
      busy.value = false
    }
  }

  return { request, verify, cooldown, busy }
}
