/** Small helper so every public page sets a consistent title + description. */
export function useSeo(title: string, description: string) {
  useHead({
    title,
    meta: [
      { name: 'description', content: description },
      { property: 'og:title', content: `${title} · Nadoumi` },
      { property: 'og:description', content: description },
      { property: 'og:type', content: 'website' },
    ],
  })
}
