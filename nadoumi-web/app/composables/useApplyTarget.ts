import type { ApplyTarget } from '~/types/apply'
import type { ProgramDetail, ScholarshipDetail } from '~/types/catalog'
import type { ApplyQuery } from '~/utils/applyView'

/**
 * Loads what the student is applying for through the public, student-safe catalog API. A scholarship carries no
 * university, so none is ever shown for it. An unknown or unpublished slug resolves to `null`.
 */
export function useApplyTarget() {
  const { publicGet } = useApi()

  async function load(query: ApplyQuery): Promise<ApplyTarget | null> {
    try {
      if (query.kind === 'scholarship') {
        const s = await publicGet<ScholarshipDetail>(`scholarships/${query.slug}`)
        return { kind: 'scholarship', id: s.id, slug: query.slug, title: s.title }
      }
      const p = await publicGet<ProgramDetail>(`programs/${query.slug}`)
      return {
        kind: 'programme',
        id: p.id,
        slug: query.slug,
        title: p.name,
        universityName: p.universityName ?? null,
        intakes: (p.intakes ?? []).map(i => ({ id: i.id, term: i.term })),
      }
    }
    catch {
      return null
    }
  }

  return { load }
}
