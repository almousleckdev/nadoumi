import type { ApplicationSummary } from '~/types/catalog'

const NOT_FOUND = 404

export function useApplicationTracker() {
  const { studentFetch } = useApi()

  const appId = ref('')
  const tracking = ref(false)
  const state = ref<'idle' | 'ok' | 'notfound' | 'unavailable'>('idle')
  const application = ref<ApplicationSummary | null>(null)

  async function track() {
    const id = appId.value.trim()
    if (!id || tracking.value) return
    tracking.value = true
    state.value = 'idle'
    application.value = null
    try {
      application.value = await studentFetch<ApplicationSummary>(`applications/${encodeURIComponent(id)}`)
      state.value = 'ok'
    }
    catch (e) {
      const status = (e as { statusCode?: number, status?: number })?.statusCode ?? (e as { status?: number })?.status
      // A real 404 from a present endpoint means "no such application"; anything
      // else (route not deployed yet, 5xx) falls back to the "coming soon" guidance.
      state.value = status === NOT_FOUND ? 'notfound' : 'unavailable'
    }
    finally {
      tracking.value = false
    }
  }

  return { appId, tracking, state, application, track }
}
