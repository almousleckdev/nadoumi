import type { Page, UniversitySummary } from '~/types/catalog'

const CAROUSEL_SIZE = 12
const PARTNER_LOGO_COUNT = 24

export function useHomeFeeds() {
  const { publicGet } = useApi()

  function feed<T>(key: string, resource: string, query: Record<string, unknown>) {
    const { data, pending, error } = useLazyAsyncData(
      key,
      () => publicGet<Page<T>>(resource, query),
      { default: () => null },
    )
    const items = computed(() => data.value?.content ?? [])
    return { items, pending, error }
  }

  return {
    featured: feed<UniversitySummary>('home-featured-universities', 'universities', { featured: true, size: CAROUSEL_SIZE }),
    recommended: feed<UniversitySummary>('home-recommended-universities', 'universities', { recommended: true, size: CAROUSEL_SIZE }),
    partners: feed<UniversitySummary>('home-partner-universities', 'universities', { publicPartner: true, size: PARTNER_LOGO_COUNT }),
  }
}
