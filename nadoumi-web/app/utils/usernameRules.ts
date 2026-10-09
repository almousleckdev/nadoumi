/**
 * The handle a student picks when registering. Mirrors the server's rules (StudentUsernames) so the form can say
 * what is wrong as they type; the server still has the last word.
 */
export const USERNAME_MIN = 3
export const USERNAME_MAX = 20
const SHAPE = /^[a-z0-9][a-z0-9_.]*$/
const RESERVED = new Set(['admin', 'administrator', 'root', 'nadoumi', 'support', 'staff', 'system'])

export function normalizeUsername(raw: string): string {
  return raw.trim().toLowerCase()
}

export type UsernameProblem = 'tooShort' | 'tooLong' | 'characters' | 'reserved'

/** What is wrong with a (normalised) username, or null when it is acceptable. */
export function usernameProblem(username: string): UsernameProblem | null {
  if (username.length < USERNAME_MIN) return 'tooShort'
  if (username.length > USERNAME_MAX) return 'tooLong'
  if (!SHAPE.test(username)) return 'characters'
  if (RESERVED.has(username)) return 'reserved'
  return null
}
