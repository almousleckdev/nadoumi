import { FORM_ERROR_KEYS, type FieldErrors } from '~/constants/formErrors'

/**
 * The validation loop every form shares: nothing is flagged until the first submit, errors are
 * then shown as translated copy per field, and the action runs only when there are none.
 * `validate` is a pure function of the form (read inside it, so it re-runs as the form changes).
 */
export function useValidatedForm(validate: () => FieldErrors) {
  const { t } = useI18n()
  const submitted = ref(false)

  const errors = computed<Record<string, string>>(() => {
    if (!submitted.value) return {}
    return Object.fromEntries(Object.entries(validate()).map(([field, code]) => [field, t(FORM_ERROR_KEYS[code!])]))
  })

  function submit(onValid: () => void) {
    submitted.value = true
    if (Object.keys(errors.value).length === 0) onValid()
  }

  /** Forget that the form was submitted, e.g. when it is reused for the next record. */
  function reset() {
    submitted.value = false
  }

  return { errors, submit, reset }
}
