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

  async function request(email: string, purpose: OtpPurpose): Promise<void> {
    busy.value = true
    try {
      await $fetch('/api/student-email-otp', { method: 'POST', body: { email, purpose } })
      startCooldown()
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
