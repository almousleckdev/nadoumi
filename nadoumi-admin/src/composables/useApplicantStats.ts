import { ref } from 'vue'
import { listApplicantsPage } from '@/api/dashboard'
import type { ApplicantRow } from '@/api/applicant'

export interface ApplicantStats {
  total: number
  new30d: number
  active: number
  draft: number
  /** rows inspected for profile completeness (bounded by SAMPLE_SIZE) */
  sampledCount: number
  /** of `sampledCount`, how many are missing a required field */
  incomplete: number
  /** of `sampledCount`, how many have every required field */
  complete: number
  recent: ApplicantRow[]
  /** true when the real total exceeds the inspected sample */
  sampled: boolean
}

const REQUIRED_FIELDS: (keyof ApplicantRow)[] = ['dob', 'nationality', 'passportNo', 'phone']
const SAMPLE_SIZE = 200

function isoDaysAgo(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString().slice(0, 19)
}

export function useApplicantStats() {
  const data = ref<ApplicantStats | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function refresh() {
    loading.value = true
    error.value = null
    try {
      const [totalPage, new30Page, activePage, draftPage, sample] = await Promise.all([
        listApplicantsPage({ size: 1 }),
        listApplicantsPage({ size: 1, createdAfter: isoDaysAgo(30) }),
        listApplicantsPage({ size: 1, status: 'ACTIVE' }),
        listApplicantsPage({ size: 1, status: 'DRAFT' }),
        listApplicantsPage({ size: SAMPLE_SIZE }),
      ])
      const sampledCount = sample.content.length
      const incomplete = sample.content.filter(
        r => REQUIRED_FIELDS.some(f => !r[f]),
      ).length
      data.value = {
        total: totalPage.totalElements,
        new30d: new30Page.totalElements,
        active: activePage.totalElements,
        draft: draftPage.totalElements,
        sampledCount,
        incomplete,
        complete: sampledCount - incomplete,
        recent: sample.content.slice(0, 8),
        sampled: sample.totalElements > SAMPLE_SIZE,
      }
    }
    catch (e) {
      error.value = (e as Error)?.message || 'Could not load applicant metrics'
    }
    finally {
      loading.value = false
    }
  }

  return { data, loading, error, refresh }
}
