import { onBeforeUnmount, ref, watch } from 'vue'
import { searchStudents, type StudentResult } from '@/api/conversation'

export const SEARCH_DEBOUNCE_MS = 300
/** The server needs at least this many characters of a name; an id such as "STU-12" or "12" always qualifies. */
export const MIN_NAME_LENGTH = 2
const ID_LIKE = /^(?:stu|app|student|application)?[\s#:-]*\d{1,18}$/i

export type SearchStatus = 'idle' | 'loading' | 'ready' | 'error'

/** True when the text is worth sending to the server: an id, or at least two characters of a name. */
export function isSearchable(query: string): boolean {
  const text = query.trim()
  return ID_LIKE.test(text) || text.length >= MIN_NAME_LENGTH
}

/**
 * Debounced, race-safe student search. A slow answer to an old query never overwrites the answer to the current
 * one, and nothing is requested for text the server would ignore.
 */
export function useStudentSearch(onQueryChange?: (query: string) => void) {
  const query = ref('')
  const results = ref<StudentResult[]>([])
  const status = ref<SearchStatus>('idle')
  let timer: ReturnType<typeof setTimeout> | undefined
  let sequence = 0

  async function run(text: string) {
    const mine = ++sequence
    if (!isSearchable(text)) {
      results.value = []
      status.value = 'idle'
      return
    }
    status.value = 'loading'
    try {
      const found = await searchStudents(text.trim())
      if (mine !== sequence) return
      results.value = found
      status.value = 'ready'
    }
    catch {
      if (mine !== sequence) return
      results.value = []
      status.value = 'error'
    }
  }

  watch(query, (text) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      onQueryChange?.(text)
      void run(text)
    }, SEARCH_DEBOUNCE_MS)
  })

  function clear() {
    query.value = ''
  }

  onBeforeUnmount(() => timer && clearTimeout(timer))
  return { query, results, status, clear }
}
