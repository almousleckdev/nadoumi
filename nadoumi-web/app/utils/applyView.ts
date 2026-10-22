import type { ApplyTarget, StartApplicationBody } from '~/types/apply'
import type { StudentApplicationDto } from '~/types/catalog'

export type ApplyQuery = { kind: ApplyTarget['kind'], slug: string }

const SLUG = /^[A-Za-z0-9][A-Za-z0-9_-]{0,200}$/

function single(value: unknown): string | null {
  const v = Array.isArray(value) ? value[0] : value
  return typeof v === 'string' && SLUG.test(v) ? v : null
}

/** Reads `?programme=<slug>` or `?scholarship=<slug>`; anything else (or both) is not a valid target. */
export function parseApplyQuery(query: Record<string, unknown>): ApplyQuery | null {
  const programme = single(query.programme)
  const scholarship = single(query.scholarship)
  if (programme && !scholarship) return { kind: 'programme', slug: programme }
  if (scholarship && !programme) return { kind: 'scholarship', slug: scholarship }
  return null
}

/** The request for this target. A programme needs an intake; a scholarship has none. */
export function startBody(target: ApplyTarget, applicantId: number, intakeId: number | null): StartApplicationBody {
  if (target.kind === 'scholarship') {
    return { applicantId, applicationType: 'SCHOLARSHIP_ONLY', scholarshipId: target.id }
  }
  return {
    applicantId,
    applicationType: 'PROGRAM_ONLY',
    programId: target.id,
    ...(intakeId === null ? {} : { intakeId }),
  }
}

/** An unsubmitted application of this student for this opportunity: continue it rather than start a duplicate. */
export function findDraft(
  applications: StudentApplicationDto[],
  target: ApplyTarget,
  applicantId: number,
): StudentApplicationDto | undefined {
  return applications.find(a =>
    a.currentStatus === 'DRAFT'
    && a.applicantId === applicantId
    && (target.kind === 'programme' ? a.programId === target.id : a.scholarshipId === target.id && a.programId === null))
}

/** The apply page route for an opportunity (pass it through `localePath`). */
export function applyRoute(kind: ApplyTarget['kind'], slug: string) {
  return { path: '/dashboard/apply', query: { [kind]: slug } }
}
