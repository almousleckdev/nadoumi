const BACKEND = process.env.NUXT_BACKEND_BASE_URL ?? 'http://localhost:8080'

/**
 * Reads the 6-digit OTP from the last email captured for `email`. Requires the
 * backend to run with `nadoumi.mail.transport=log`, which mounts
 * `GET /api/dev/mail/latest`.
 */
export async function readOtp(email: string): Promise<string> {
  for (let attempt = 0; attempt < 20; attempt++) {
    const res = await fetch(`${BACKEND}/api/dev/mail/latest?to=${encodeURIComponent(email)}`)
    if (res.ok) {
      const { body } = (await res.json()) as { body: string }
      const match = body.match(/\b(\d{6})\b/)
      if (match) return match[1]!
    }
    await new Promise(resolve => setTimeout(resolve, 500))
  }
  throw new Error(`no OTP email captured for ${email}`)
}

export function uniqueEmail(prefix = 'e2e'): string {
  return `${prefix}${Date.now().toString().slice(-9)}${Math.floor(Math.random() * 1000)}@example.com`
}
