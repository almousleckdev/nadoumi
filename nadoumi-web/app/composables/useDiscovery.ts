import type { LocationQueryRaw } from 'vue-router'
import type { Page } from '~/types/catalog'

export type FilterValue = string | string[] | undefined
export type Filters = Record<string, FilterValue>

export interface DiscoveryOptions {
  /** Public resource path, e.g. `universities` → `/api/public/universities`. */
  resource: string
  /** Filter keys this discovery understands. Anything else in the URL is ignored. */
  filterKeys: string[]
  /** Keys whose value is a comma-joined list. */
  arrayKeys?: string[]
  defaultSort?: string
  pageSize?: number
  /** Mirror filters + sort + page into the route query (default true). */
  syncQuery?: boolean
}

export interface ActiveChip {
  key: string
  value: string
  label: string
}

/**
 * Shared engine for every public discovery surface: owns filter + sort + page
 * state, keeps it in the URL, fetches one page via `useApi().publicGet`, and
 * exposes the loading / empty / error machine. Pages render `ResultGrid` +
 * `FilterBar` on top of this — they never re-implement the fetch/paging loop.
 */
export function useDiscovery<T>(opts: DiscoveryOptions) {
  const { publicGet } = useApi()
  const route = useRoute()
  const router = useRouter()

  const arrayKeys = new Set(opts.arrayKeys ?? [])
  const size = opts.pageSize ?? 12
  const sync = opts.syncQuery ?? true

  function readFilters(): Filters {
    const out: Filters = {}
    for (const key of opts.filterKeys) {
      const raw = route.query[key]
      if (raw == null) continue
      const str = Array.isArray(raw) ? raw.join(',') : String(raw)
      if (!str) continue
      out[key] = arrayKeys.has(key) ? str.split(',').filter(Boolean) : str
    }
    return out
  }

  const filters = reactive<Filters>(sync ? readFilters() : {})
  const sort = ref<string>(sync ? String(route.query.sort ?? opts.defaultSort ?? '') : (opts.defaultSort ?? ''))
  const page = ref<number>(sync ? Math.max(0, Number(route.query.page ?? 0) || 0) : 0)

  const apiQuery = computed<Record<string, unknown>>(() => {
    const q: Record<string, unknown> = { page: page.value, size }
    if (sort.value) q.sort = sort.value
    for (const [k, v] of Object.entries(filters)) {
      if (v == null || (Array.isArray(v) && !v.length) || v === '') continue
      q[k] = Array.isArray(v) ? v.join(',') : v
    }
    return q
  })

  const { data, pending, error, refresh } = useAsyncData<Page<T> | null>(
    () => `discovery:${opts.resource}:${JSON.stringify(apiQuery.value)}`,
    () => publicGet<Page<T>>(opts.resource, apiQuery.value).catch((e) => { throw e }),
    { watch: [apiQuery], default: () => null },
  )

  const items = computed(() => data.value?.content ?? [])
  const total = computed(() => data.value?.totalElements ?? 0)
  const pageCount = computed(() => data.value?.totalPages ?? 0)
  const isEmpty = computed(() => !pending.value && !error.value && items.value.length === 0)

  const hasActiveFilters = computed(() =>
    Object.values(filters).some(v => (Array.isArray(v) ? v.length > 0 : Boolean(v))),
  )

  function setFilter(key: string, value: FilterValue) {
    if (value == null || value === '' || (Array.isArray(value) && !value.length)) filters[key] = undefined
    else filters[key] = value
    page.value = 0
  }
  function toggleValue(key: string, value: string) {
    const cur = Array.isArray(filters[key]) ? [...(filters[key] as string[])] : []
    const i = cur.indexOf(value)
    if (i === -1) cur.push(value)
    else cur.splice(i, 1)
    setFilter(key, cur)
  }
  function setSort(value: string) { sort.value = value; page.value = 0 }
  function setPage(value: number) { page.value = Math.max(0, value) }
  function clearFilters() {
    for (const k of Object.keys(filters)) filters[k] = undefined
    page.value = 0
  }

  if (sync && import.meta.client) {
    watch(apiQuery, (q: Record<string, unknown>) => {
      const query: LocationQueryRaw = {}
      for (const [k, v] of Object.entries(q)) {
        if (k === 'size') continue
        if (k === 'page' && v === 0) continue
        query[k] = String(v)
      }
      router.replace({ query }).catch(() => {})
    })
  }

  return {
    filters, sort, page, size,
    items, total, pageCount,
    pending, error, isEmpty, hasActiveFilters,
    setFilter, toggleValue, setSort, setPage, clearFilters, refresh,
  }
}
