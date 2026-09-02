const BACKEND = process.env.NUXT_BACKEND_BASE_URL ?? 'http://localhost:8080'

/**
 * Reads the 6-digit OTP from the last email captured for `email`. Requires the
 * backend to run with `nadoumi.mail.transport=log`, which mounts
 * `GET /api/dev/mail/latest`.
 *
 * `expectSubject` guards against reading a stale earlier mail (e.g. the
 * registration code when we want the password-reset code) — it keeps polling
 * until the latest captured mail's subject matches.
 */
export async function readOtp(email: string, expectSubject?: RegExp): Promise<string> {
  for (let attempt = 0; attempt < 30; attempt++) {
    const res = await fetch(`${BACKEND}/api/dev/mail/latest?to=${encodeURIComponent(email)}`)
    if (res.ok) {
      const { subject, body } = (await res.json()) as { subject: string; body: string }
      if (!expectSubject || expectSubject.test(subject)) {
        const match = body.match(/\b(\d{6})\b/)
        if (match) return match[1]!
      }
    }
    await new Promise(resolve => setTimeout(resolve, 500))
  }
  throw new Error(`no matching OTP email captured for ${email}`)
}

export const REGISTER_SUBJECT = /verify your email/i
export const RESET_SUBJECT = /reset your password/i

export function uniqueEmail(prefix = 'e2e'): string {
  return `${prefix}${Date.now().toString().slice(-9)}${Math.floor(Math.random() * 1000)}@example.com`
}
