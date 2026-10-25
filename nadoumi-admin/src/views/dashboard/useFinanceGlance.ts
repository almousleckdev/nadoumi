import { ref } from 'vue'
import { getFinanceOverview, type FinanceOverview } from '@/api/finance'
import type { useDashboardAccess } from './useDashboardAccess'

type Access = ReturnType<typeof useDashboardAccess>

/** The finance overview for the dashboard: only fetched for staff who may see finance. */
export function useFinanceGlance(access: Access) {
  const overview = ref<FinanceOverview | null>(null)
  const loading = ref(false)
  const failed = ref(false)

  async function load() {
    if (!access.canSeeFinance.value) return
    loading.value = true
    failed.value = false
    try {
      overview.value = await getFinanceOverview()
    }
    catch {
      failed.value = true
    }
    finally {
      loading.value = false
    }
  }

  return { overview, loading, failed, load }
}
