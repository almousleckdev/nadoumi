/**
 * Client mirror of the backend `com.ruoyi.common.utils.PasswordPolicy`
 * (spec Revision 2, D-R2-3). Same rules, same evaluation order, i18n-key results.
 * Keep this in lockstep with the Java validator.
 */
const SPECIALS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
const MIN = 8
const MAX = 32

export interface PasswordCheckOptions {
  /** Personal terms (first name, last name, email local-part…) the password must not contain. */
  forbidden?: string[]
  /** When set, an extra "passwords match" rule is evaluated last. */
  confirm?: string
  /** The current password — the new one must differ from it. */
  current?: string
}

export interface PasswordRule {
  key: string
  ok: boolean
}

export interface PasswordCheckResult {
  rules: PasswordRule[]
  /** First failing rule's i18n key, or null when the password is acceptable. */
  firstError: string | null
  /** All policy rules pass (ignores the confirm rule). */
  strong: boolean
}

function containsAny(value: string, terms: string[] | undefined): boolean {
  const lower = value.toLowerCase()
  return (terms ?? [])
    .map(term => term.trim().toLowerCase())
    .filter(term => term.length >= 3)
    .some(term => lower.includes(term))
}

export function passwordChecks(value: string, opts: PasswordCheckOptions = {}): PasswordCheckResult {
  const v = value ?? ''
  const rules: PasswordRule[] = [
    { key: 'validation.password.length', ok: v.length >= MIN && v.length <= MAX },
    { key: 'validation.password.needUpper', ok: /[A-Z]/.test(v) },
    { key: 'validation.password.needLower', ok: /[a-z]/.test(v) },
    { key: 'validation.password.needDigit', ok: /[0-9]/.test(v) },
    { key: 'validation.password.needSpecial', ok: [...v].some(c => SPECIALS.includes(c)) },
  ]
  if ((opts.forbidden ?? []).length) {
    rules.push({ key: 'validation.password.noPersonal', ok: v.length > 0 && !containsAny(v, opts.forbidden) })
  }

  const strong = rules.every(r => r.ok) && !(opts.current && v === opts.current)
  let firstError: string | null = null
  if (v.length < MIN) firstError = 'validation.password.tooShort'
  else if (v.length > MAX) firstError = 'validation.password.tooLong'
  else {
    const failed = rules.find(r => !r.ok)
    if (failed) firstError = failed.key
    else if (opts.current && v === opts.current) firstError = 'validation.password.sameAsCurrent'
    else if (opts.confirm !== undefined && v !== opts.confirm) firstError = 'validation.password.mismatch'
  }

  return { rules, firstError, strong }
}

/** Back-compat single-key result (used where the granular list is not shown). */
export function passwordPolicyKey(value: string, current?: string): string | null {
  return passwordChecks(value, { current }).firstError
}
