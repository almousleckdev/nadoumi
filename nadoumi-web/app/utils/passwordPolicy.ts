/**
 * Client mirror of the backend `com.ruoyi.common.utils.PasswordPolicy`
 * (spec Revision 2, D-R2-3). Same rules, same evaluation order, i18n-key results.
 * Keep this in lockstep with the Java validator.
 */
const SPECIALS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"

export function passwordPolicyKey(value: string, current?: string): string | null {
  if (!value || value.length < 8) return 'validation.password.tooShort'
  if (value.length > 32) return 'validation.password.tooLong'
  if (!/[A-Z]/.test(value)) return 'validation.password.needUpper'
  if (!/[a-z]/.test(value)) return 'validation.password.needLower'
  if (!/[0-9]/.test(value)) return 'validation.password.needDigit'
  if (![...value].some(c => SPECIALS.includes(c))) return 'validation.password.needSpecial'
  if (current && value === current) return 'validation.password.sameAsCurrent'
  return null
}
