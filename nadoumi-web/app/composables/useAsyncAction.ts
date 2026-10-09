/**
 * The busy/error pair every save-action page repeats: reset the error, run the
 * action, announce success as a toast or map the failure via authErrorMessage,
 * always clear busy. `run` returns the action's result (or undefined on failure)
 * so callers can still use it. `notice` is kept (always empty) for pages that
 * still bind it; success is no longer an inline banner.
 */
export function useAsyncAction() {
  const { t } = useI18n()
  const toast = useToast()
  const busy = ref(false)
  const notice = ref('')
  const error = ref('')

  async function run<T>(fn: () => Promise<T>, successMessage?: string): Promise<T | undefined> {
    busy.value = true
    notice.value = ''
    error.value = ''
    try {
      const result = await fn()
      if (successMessage) toast.success(successMessage)
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
