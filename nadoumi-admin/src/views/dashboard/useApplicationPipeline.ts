import { computed, ref } from 'vue'
import { listApplications, type ApplicationRow } from '@/api/application'
import { listPayments } from '@/api/payment'
import { STATUSES } from '@/views/applications/vocabulary'
import type { useDashboardAccess } from './useDashboardAccess'

const LATEST_COUNT = 6
const OFFER_STATUSES = ['OFFER', 'PRE_DEPARTURE', 'CLOSED_SUCCESS']

type Access = ReturnType<typeof useDashboardAccess>

/** Exact per-status totals (one count query each, page size 1) plus the newest applications and the payment queue. */
export function useApplicationPipeline(access: Access) {
  const perStatus = ref<Record<string, number>>({})
  const latest = ref<ApplicationRow[]>([])
  const paymentsToConfirm = ref<number | null>(null)
  const loading = ref(false)
  const failed = ref(false)

  const total = computed(() => Object.values(perStatus.value).reduce((sum, n) => sum + n, 0))
  const inReview = computed(() => perStatus.value.IN_REVIEW ?? 0)
  const offers = computed(() => OFFER_STATUSES.reduce((sum, s) => sum + (perStatus.value[s] ?? 0), 0))
  const slices = computed(() => STATUSES
    .filter(s => (perStatus.value[s] ?? 0) > 0)
    .map(s => ({ key: s, value: perStatus.value[s]! })))

  async function load() {
    loading.value = true
    failed.value = false
    const jobs: Promise<unknown>[] = []
    if (access.canSeeApplications.value) {
      jobs.push(
        Promise.all(STATUSES.map(async s => [s, (await listApplications({ status: s, page: 0, size: 1 })).totalElements ?? 0] as const))
          .then((pairs) => { perStatus.value = Object.fromEntries(pairs) }),
        listApplications({ page: 0, size: LATEST_COUNT }).then((page) => { latest.value = page.content }),
      )
    }
    if (access.canSeePayments.value) {
      jobs.push(listPayments({ status: 'PROOF_SUBMITTED', page: 0, size: 1 }).then((page) => { paymentsToConfirm.value = page.totalElements ?? 0 }))
    }
    try {
      await Promise.all(jobs)
    }
    catch {
      failed.value = true
    }
    finally {
      loading.value = false
    }
  }

  return { perStatus, latest, paymentsToConfirm, total, inReview, offers, slices, loading, failed, load }
}
