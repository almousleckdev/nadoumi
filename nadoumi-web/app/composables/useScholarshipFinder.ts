import type { Page, ScholarshipCard } from '~/types/catalog'

const RESULT_LIMIT = 5

export function useScholarshipFinder() {
  const { publicGet } = useApi()

  const query = ref('')
  const funding = ref('')
  const finding = ref(false)
  const failed = ref(false)
  const searched = ref(false)
  const results = ref<ScholarshipCard[]>([])

  const resultsQuery = computed(() => {
    const params = new URLSearchParams()
    if (query.value.trim()) params.set('q', query.value.trim())
    if (funding.value) params.set('funding', funding.value)
    const qs = params.toString()
    return qs ? `?${qs}` : ''
  })

  async function search() {
    if (finding.value) return
    finding.value = true
    failed.value = false
    searched.value = true
    try {
      const params: Record<string, unknown> = { size: RESULT_LIMIT }
      if (query.value.trim()) params.q = query.value.trim()
      if (funding.value) params.funding = funding.value
      const page = await publicGet<Page<ScholarshipCard>>('scholarships', params)
      results.value = page?.content ?? []
    }
    catch {
      failed.value = true
      results.value = []
    }
    finally {
      finding.value = false
    }
  }

  return { query, funding, finding, failed, searched, results, resultsQuery, search }
}
