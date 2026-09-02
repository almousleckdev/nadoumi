import { ref, type Ref } from 'vue'

/**
 * A small list-resource fetcher with loading / error / loaded state. Used by the
 * applicant detail tabs (education, test scores, contacts, access) so none of
 * them re-implement fetch + spinner + error handling.
 */
export function useResourceList<T>(fetcher: () => Promise<T[]>) {
  const items = ref<T[]>([]) as Ref<T[]>
  const loading = ref(false)
  const error = ref<string | null>(null)
  const loaded = ref(false)

  async function load(force = false) {
    if (loaded.value && !force) return
    loading.value = true
    error.value = null
    try {
      items.value = await fetcher()
      loaded.value = true
    }
    catch (e) {
      error.value = (e as Error)?.message || 'Could not load'
    }
    finally {
      loading.value = false
    }
  }

  return { items, loading, error, loaded, load, reload: () => load(true) }
}
