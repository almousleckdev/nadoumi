/**
 * Labels for what a student entered: ISO country and language codes in the staff member's language, and the
 * stored enum codes (HIGH_SCHOOL, EN_ZH...) as readable text. Nothing here is shown to students.
 */
function names(locale: string, type: 'region' | 'language'): Intl.DisplayNames | null {
  try {
    return new Intl.DisplayNames([locale], { type, fallback: 'none' })
  }
  catch {
    return null
  }
}

/** "CN" -> "China" (or the code itself when the browser has no name for it). */
export function countryName(code: string | null | undefined, locale: string): string {
  if (!code) return ''
  return names(locale, 'region')?.of(code.toUpperCase()) ?? code
}

/** "ar" -> "Arabic" (or the code itself when the browser has no name for it). */
export function languageName(code: string | null | undefined, locale: string): string {
  if (!code) return ''
  return names(locale, 'language')?.of(code.toLowerCase()) ?? code
}

/** A stored enum code as readable text: "HIGH_SCHOOL" -> "High school". */
export function codeLabel(code: string | null | undefined): string {
  if (!code) return ''
  const text = code.toLowerCase().replace(/_/g, ' ')
  return text.charAt(0).toUpperCase() + text.slice(1)
}

/**
 * What to call an applicant on screen. A student who has registered but not yet entered their passport names has
 * no name, so fall back to their email (then to a placeholder) rather than showing an empty row.
 */
export function applicantDisplayName(
  a: { givenName?: string | null, familyName?: string | null, email?: string | null },
  placeholder: string,
): string {
  const full = [a.givenName, a.familyName].filter(Boolean).join(' ').trim()
  return full || a.email || placeholder
}
