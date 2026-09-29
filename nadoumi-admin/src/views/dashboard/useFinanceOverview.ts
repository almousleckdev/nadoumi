import { computed, ref } from 'vue'
import { getFinanceSummary, type FinanceSummary } from '@/api/finance'
import { leadingRevenue, summarizeFinance } from './dashboardMath'

export function useFinanceOverview(enabled: () => boolean) {
  const yearToDate = ref<FinanceSummary | null>(null)
  const today = ref<FinanceSummary | null>(null)
  const allTime = ref<FinanceSummary | null>(null)
  const loading = ref(false)

  const state = computed<'ok' | 'loading'>(() => (loading.value && !yearToDate.value ? 'loading' : 'ok'))
  const revenueToday = computed(() => leadingRevenue(today.value))
  const revenueAllTime = computed(() => leadingRevenue(allTime.value))
  const summary = computed(() => summarizeFinance(yearToDate.value))

  async function load() {
    if (!enabled()) return
    loading.value = true
    const day = new Date().toISOString().slice(0, 10)
    try {
      const [ytd, dayTotals, all] = await Promise.all([
        getFinanceSummary({ from: `${new Date().getFullYear()}-01-01` }),
        getFinanceSummary({ from: day, to: day }),
        getFinanceSummary({}),
      ])
      yearToDate.value = ytd
      today.value = dayTotals
      allTime.value = all
    }
    catch {
      yearToDate.value = null
    }
    finally {
      loading.value = false
    }
  }

  return { loading, state, revenueToday, revenueAllTime, summary, load }
}
