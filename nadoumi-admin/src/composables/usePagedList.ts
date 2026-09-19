import { computed, reactive, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'

/** The two list shapes the backend returns: RuoYi `{ rows, total }` and Nadoumi `{ content, totalElements }`. */
type ListResponse<T> = { rows: T[], total: number } | { content: T[], totalElements: number }

export interface PagedListOptions<T, F extends object> {
  /** Fresh, empty filter values: the starting point and what "clear" resets to. */
  emptyFilters: () => F
  /** Fetch one page for the current filters. `page` starts at `firstPage`; `index` is always 0-based. */
  fetch: (filters: F, paging: { page: number, size: number, index: number }) => Promise<ListResponse<T>>
  /** 1 for RuoYi endpoints, 0 for Nadoumi endpoints. */
  firstPage: 0 | 1
  size: number
}

/**
 * The state every list screen keeps: rows, total, loading and error, the filters, the page and its
 * size, and reloading from the first page when a filter changes. A screen supplies only how to
 * call its endpoint; none re-implements the fetch/loading/error skeleton.
 */
export function usePagedList<T, F extends object>(options: PagedListOptions<T, F>) {
  const { t } = useI18n()
  const rows = ref<T[]>([]) as Ref<T[]>
  const total = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const filters = reactive(options.emptyFilters()) as F
  const page = ref<number>(options.firstPage)
  const size = ref(options.size)

  /** True when any filter has a value (a `false` toggle counts as unset), so a screen can offer "clear". */
  const dirty = computed(() =>
    Object.values(filters).some(v => v !== undefined && v !== null && v !== '' && v !== false))

  async function load() {
    loading.value = true
    error.value = null
    try {
      const res = await options.fetch(filters, {
        page: page.value,
        size: size.value,
        index: page.value - options.firstPage,
      })
      const isRuoyi = 'rows' in res
      rows.value = isRuoyi ? res.rows : res.content
      total.value = isRuoyi ? res.total : res.totalElements
    }
    catch (e) {
      error.value = (e as Error)?.message || t('state.errorTitle')
    }
    finally {
      loading.value = false
    }
  }

  /** Reload from the first page; use after a filter changes. */
  function reload() {
    page.value = options.firstPage
    return load()
  }

  function clearFilters() {
    Object.assign(filters, options.emptyFilters())
    return reload()
  }

  return { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters }
}
