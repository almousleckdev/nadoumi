/**
 * The busy/notice/error triad every dashboard save-action page repeats:
 * reset both messages, run the action, show a success notice or map the
 * error via authErrorMessage, always clear busy. `run` returns the action's
 * result (or undefined on failure) so callers can still use it.
 */
export function useAsyncAction() {
  const { t } = useI18n()
  const busy = ref(false)
  const notice = ref('')
  const error = ref('')

  async function run<T>(fn: () => Promise<T>, successMessage?: string): Promise<T | undefined> {
    busy.value = true
    notice.value = ''
    error.value = ''
    try {
      const result = await fn()
      if (successMessage) notice.value = successMessage
      return result
    }
    catch (e) {
      error.value = authErrorMessage(e, t)
      return undefined
    }
    finally {
      busy.value = false
    }
  }

  return { busy, notice, error, run }
}
