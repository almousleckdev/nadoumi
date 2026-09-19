import { parse } from 'mrz'
import type { PassportReading } from './types'

const TD3_LINE_LENGTH = 44
const MIN_LINE = TD3_LINE_LENGTH - 2
const MAX_LINE = TD3_LINE_LENGTH + 2
const MRZ_CHARS = /[^A-Z0-9<]/g
/** OCR often reads the `<` filler as one of these. */
const FILLER_LOOKALIKES = /[«‹〈《]/g

/** The century is chosen so a birth date is never in the future; an expiry date is always this century. */
export function expandMrzDate(yymmdd: string, kind: 'birth' | 'expiry', today: Date = new Date()): string {
  const yy = Number(yymmdd.slice(0, 2))
  const century = kind === 'expiry' || 2000 + yy <= today.getFullYear() ? 2000 : 1900
  return `${century + yy}-${yymmdd.slice(2, 4)}-${yymmdd.slice(4, 6)}`
}

/** The two candidate MRZ lines: the last two lines of MRZ-like length, cleaned and normalised to 44. */
function candidateLines(text: string): string[] {
  return text
    .toUpperCase()
    .replace(FILLER_LOOKALIKES, '<')
    .split(/\r?\n/)
    .map(line => line.replace(/\s+/g, '').replace(MRZ_CHARS, ''))
    .filter(line => line.length >= MIN_LINE && line.length <= MAX_LINE)
    .map(line => line.padEnd(TD3_LINE_LENGTH, '<').slice(0, TD3_LINE_LENGTH))
    .slice(-2)
}

/**
 * Parses passport (TD3) MRZ text, e.g. straight from OCR. Accepts a result only when
 * every check digit and both dates validate, so a misread returns `null` instead of
 * plausible but wrong data. Country codes are not required to be valid ISO codes.
 */
export function parsePassportMrz(text: string, today: Date = new Date()): PassportReading | null {
  const lines = candidateLines(text)
  if (lines.length !== 2) return null

  let result: ReturnType<typeof parse>
  try {
    result = parse(lines, { autocorrect: true })
  }
  catch {
    return null
  }
  if (result.format !== 'TD3') return null

  const checked = result.details.filter(d => /CheckDigit$/.test(d.field ?? '') || d.field === 'birthDate' || d.field === 'expirationDate')
  if (checked.length === 0 || checked.some(d => !d.valid)) return null

  const f = result.fields
  if (!f.documentNumber || !f.lastName || !f.firstName || !f.birthDate || !f.expirationDate) return null
  return {
    documentNumber: f.documentNumber.replace(/</g, ''),
    surname: f.lastName,
    givenNames: f.firstName,
    dateOfBirth: expandMrzDate(f.birthDate, 'birth', today),
    expiryDate: expandMrzDate(f.expirationDate, 'expiry', today),
    issuingCountry: f.issuingState ?? null,
    nationality: f.nationality ?? null,
  }
}
