import type { Ref } from 'vue'

/** The four calls a list-shaped applicant section needs, already bound to that section. */
export interface RecordsApi<T, B> {
  list: (applicantId: number) => Promise<T[]>
  add: (applicantId: number, body: B) => Promise<unknown>
  update: (applicantId: number, recordId: number, body: B) => Promise<unknown>
  remove: (applicantId: number, recordId: number) => Promise<void>
}

/**
 * Load, add, edit and remove the records of one applicant section (education, work, contacts).
 * Every write reloads the list, so what is shown is what the server holds.
 */
export function useApplicantRecords<T, B>(
  applicantId: MaybeRefOrGetter<number>, api: RecordsApi<T, B>, onChanged: () => void = () => undefined,
) {
  const items = ref([]) as Ref<T[]>
  const { busy, error, run } = useAsyncAction()

  async function load() {
    items.value = await api.list(toValue(applicantId)).catch(() => [])
  }

  /** Adds when `recordId` is null, otherwise edits it; true when the server accepted the change. */
  async function save(recordId: number | null, body: B): Promise<boolean> {
    const id = toValue(applicantId)
    const saved = await run(async () => {
      if (recordId === null) await api.add(id, body)
      else await api.update(id, recordId, body)
      await load()
      return true
    })
    if (saved) onChanged()
    return saved === true
  }

  async function remove(recordId: number): Promise<boolean> {
    const removed = await run(async () => {
      await api.remove(toValue(applicantId), recordId)
      await load()
      return true
    })
    if (removed) onChanged()
    return removed === true
  }

  return { items, busy, error, load, save, remove }
}
