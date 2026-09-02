/**
 * Single mapping strategy for every authentication failure (spec Revision 2, §7.2 /
 * requirement "reusable error/validation mapping"). Takes anything a `$fetch` call
 * or a client check can throw and returns one clean, translated user-facing string.
 * Raw backend exceptions and technical text are never surfaced.
 */
type Translate = (key: string, params?: Record<string, unknown>) => string

interface FetchLikeError {
  statusCode?: number
  status?: number
  data?: { detail?: string; title?: string; status?: number } | string
  name?: string
  message?: string
}

/** Ordered: the first backend `detail` substring that matches wins. */
const DETAIL_RULES: [RegExp, string][] = [
  [/already registered/i, 'errors.emailTaken'],
  [/email or password is incorrect/i, 'errors.badCredentials'],
  [/current password is incorrect/i, 'errors.currentPasswordWrong'],
  [/does not match this address/i, 'errors.otpEmailMismatch'],
  [/verification code is invalid or expired/i, 'errors.otpInvalid'],
  [/verification ticket is invalid or expired/i, 'errors.otpExpired'],
  [/registration is disabled/i, 'errors.registrationDisabled'],
  [/captcha/i, 'errors.captcha'],
  [/staff must sign in/i, 'errors.badCredentials'],
]

export function authErrorMessage(err: unknown, t: Translate): string {
  // 1. A validation/i18n key passed straight through by a client-side check.
  if (typeof err === 'string' && /^(validation|errors|auth)\./.test(err)) {
    return t(err)
  }

  const e = (err ?? {}) as FetchLikeError
  const status = e.statusCode ?? e.status ?? (typeof e.data === 'object' ? e.data?.status : undefined)
  const detail = typeof e.data === 'string' ? e.data : e.data?.detail

  // 2. No status + fetch/network failure -> the request never completed.
  const looksNetwork = status === undefined
    && (e.name === 'FetchError' || e instanceof TypeError || /network|fetch failed|ECONNREFUSED/i.test(e.message ?? ''))
  if (looksNetwork) return t('errors.network')

  // 3. Rate limiting and server faults, independent of the body.
  if (status === 429) return t('errors.rateLimited')
  if (status !== undefined && status >= 500) return t('errors.server')

  // 4. A backend password-policy key is already an i18n key under validation.*.
  if (detail && /^password\./.test(detail)) return t(`validation.${detail}`)

  // 5. Known backend `detail` phrases.
  if (detail) {
    for (const [pattern, key] of DETAIL_RULES) {
      if (pattern.test(detail)) return t(key)
    }
    // Bean-validation messages ("<field> must be a well-formed email address", …)
    if (status === 400 && /e-?mail/i.test(detail)) return t('errors.invalidEmail')
  }

  // 6. Anything else.
  return status === 400 ? t('errors.validation') : t('errors.generic')
}
