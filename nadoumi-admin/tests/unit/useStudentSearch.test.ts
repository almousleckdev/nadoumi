import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'

const api = vi.hoisted(() => ({ searchStudents: vi.fn() }))
vi.mock('@/api/conversation', () => api)

import { isSearchable, SEARCH_DEBOUNCE_MS, useStudentSearch } from '@/composables/useStudentSearch'

function host(onQuery?: (q: string) => void) {
  let api2!: ReturnType<typeof useStudentSearch>
  mount(defineComponent({
    setup() {
      api2 = useStudentSearch(onQuery)
      return () => h('div')
    },
  }))
  return () => api2
}
const hit = (id: number) => ({ userId: id, studentRef: `STU-${id}`, name: `S${id}`, avatarUrl: null, online: false, matchedApplicationId: null })

beforeEach(() => {
  vi.useFakeTimers()
  api.searchStudents.mockReset().mockResolvedValue([hit(1)])
})
afterEach(() => vi.useRealTimers())

describe('isSearchable', () => {
  it('accepts ids and two characters of a name, nothing less', () => {
    expect(isSearchable('a')).toBe(false)
    expect(isSearchable('  ')).toBe(false)
    expect(isSearchable('am')).toBe(true)
    expect(isSearchable('12')).toBe(true)
    expect(isSearchable('7')).toBe(true)
    expect(isSearchable('STU-7')).toBe(true)
    expect(isSearchable('app #1001')).toBe(true)
  })
})

describe('useStudentSearch', () => {
  it('waits for the typing to pause, then asks the server once', async () => {
    const s = host()
    s().query.value = 'ami'
    await nextTick()
    s().query.value = 'amin'
    await nextTick()
    vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS - 1)
    expect(api.searchStudents).not.toHaveBeenCalled()
    vi.advanceTimersByTime(1)
    await flushPromises()
    expect(api.searchStudents).toHaveBeenCalledTimes(1)
    expect(api.searchStudents).toHaveBeenCalledWith('amin')
    expect(s().results.value).toHaveLength(1)
    expect(s().status.value).toBe('ready')
  })

  it('asks nothing for text too short to search and clears old results', async () => {
    const s = host()
    s().query.value = 'amina'
    await nextTick()
    vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS)
    await flushPromises()
    s().query.value = 'a'
    await nextTick()
    vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS)
    await flushPromises()
    expect(api.searchStudents).toHaveBeenCalledTimes(1)
    expect(s().results.value).toEqual([])
    expect(s().status.value).toBe('idle')
  })

  it('lets the narrowing of the inbox follow the same pause', async () => {
    const onQuery = vi.fn()
    const s = host(onQuery)
    s().query.value = 'omar'
    await nextTick()
    vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS)
    expect(onQuery).toHaveBeenCalledWith('omar')
  })

  it('never lets a slow answer to an old query overwrite the answer to the new one', async () => {
    let resolveOld!: (v: unknown) => void
    api.searchStudents.mockReturnValueOnce(new Promise((r) => { resolveOld = r })).mockResolvedValueOnce([hit(2)])
    const s = host()
    s().query.value = 'old'
    await nextTick()
    vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS)
    s().query.value = 'new'
    await nextTick()
    vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS)
    await flushPromises()
    resolveOld([hit(1)])
    await flushPromises()
    expect(s().results.value.map(r => r.userId)).toEqual([2])
  })

  it('reports a failed search without leaving stale results', async () => {
    api.searchStudents.mockRejectedValue(new Error('down'))
    const s = host()
    s().query.value = 'amina'
    await nextTick()
    vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS)
    await flushPromises()
    expect(s().status.value).toBe('error')
    expect(s().results.value).toEqual([])
  })
})
