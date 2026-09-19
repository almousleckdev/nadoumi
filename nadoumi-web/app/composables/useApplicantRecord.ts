import type { Ref } from 'vue'

/** The two calls a single-record applicant section needs (study interests, current location). */
export interface RecordApi<T, B> {
  get: (applicantId: number) => Promise<T | null>
  save: (applicantId: number, body: B) => Promise<T>
}

/** Load and save the one record an applicant section holds, with the shared busy/error/notice state. */
export function useApplicantRecord<T, B>(
  applicantId: MaybeRefOrGetter<number>, api: RecordApi<T, B>, onChanged: () => void = () => undefined,
) {
  const { t } = useI18n()
  const record = ref(null) as Ref<T | null>
  const { busy, error, notice, run } = useAsyncAction()

  async function load() {
    record.value = await api.get(toValue(applicantId)).catch(() => null)
  }

  async function save(body: B): Promise<boolean> {
    const saved = await run(() => api.save(toValue(applicantId), body), t('dashboard.savedOk'))
    if (!saved) return false
    record.value = saved
    onChanged()
    return true
  }

  return { record, busy, error, notice, load, save }
}
