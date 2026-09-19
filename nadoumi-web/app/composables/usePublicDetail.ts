/**
 * The record behind a public detail page: the `[slug]` route parameter and its data, fetched
 * through the BFF and cached per slug. A missing record is `null` (the page renders its own
 * not-found state) rather than an error. Programme, scholarship and university pages share it.
 */
export async function usePublicDetail<T>(resource: 'programs' | 'scholarships' | 'universities') {
  const route = useRoute()
  const { publicGet } = useApi()
  const slug = computed(() => String(route.params.slug))

  const { data } = await useAsyncData(
    () => `${resource}-${slug.value}`,
    () => publicGet<T>(`${resource}/${slug.value}`).catch(() => null),
    { watch: [slug] },
  )
  return { slug, data }
}
