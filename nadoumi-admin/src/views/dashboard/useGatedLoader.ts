import { ref, type Ref } from 'vue'

export function useGatedLoader<T>(initial: T, enabled: () => boolean, fetch: () => Promise<T>) {
  const data = ref(initial) as Ref<T>
  const loading = ref(false)

  async function load() {
    if (!enabled()) return
    loading.value = true
    try {
      data.value = await fetch()
    }
    catch {
      data.value = initial
    }
    finally {
      loading.value = false
    }
  }

  return { data, loading, load }
}
