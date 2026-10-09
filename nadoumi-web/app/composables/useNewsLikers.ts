import type { Liker, LikersPage } from '~/types/news'

const LIKERS_LIMIT = 30

/** The "liked by" list of one article (signed-in readers only; the API refuses guests). */
export function useNewsLikers(slug: string) {
  const { studentFetch } = useApi()

  const likers = ref<Liker[]>([])
  const total = ref(0)
  const loading = ref(false)
  const failed = ref(false)
  const loaded = ref(false)

  async function load(): Promise<void> {
    if (loading.value) return
    loading.value = true
    failed.value = false
    try {
      const page = await studentFetch<LikersPage>(`news/${slug}/likes`, { query: { limit: LIKERS_LIMIT } })
      likers.value = page.likers
      total.value = page.total
      loaded.value = true
    }
    catch {
      failed.value = true
    }
    finally {
      loading.value = false
    }
  }

  return { likers, total, loading, failed, loaded, load }
}
