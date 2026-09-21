import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import {
  useConversationStream, CONVERSATION_STREAM_URL, STREAM_BACKOFF_START_MS, STREAM_POLL_MS,
} from '~/composables/useConversationStream'

class FakeEventSource {
  static instances: FakeEventSource[] = []
  closed = false
  private listeners = new Map<string, ((e: Event) => void)[]>()
  constructor(public url: string) {
    FakeEventSource.instances.push(this)
  }
  addEventListener(type: string, fn: (e: Event) => void) {
    this.listeners.set(type, [...(this.listeners.get(type) ?? []), fn])
  }
  close() {
    this.closed = true
  }
  emit(type: string, data?: string) {
    const event = data === undefined ? new Event(type) : new MessageEvent(type, { data })
    for (const fn of this.listeners.get(type) ?? []) fn(event)
  }
}

function host(onPing: (id: number) => void, onResync: () => void) {
  let api!: ReturnType<typeof useConversationStream>
  const wrapper = mount(defineComponent({
    setup() {
      api = useConversationStream(onPing, onResync)
      return () => h('div')
    },
  }))
  return { wrapper, api: () => api }
}

beforeEach(() => {
  vi.useFakeTimers()
  FakeEventSource.instances = []
  vi.stubGlobal('EventSource', FakeEventSource)
})
afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('useConversationStream', () => {
  it('opens the BFF stream and turns a conversation event into onPing(refId)', () => {
    const onPing = vi.fn()
    host(onPing, vi.fn())

    const es = FakeEventSource.instances[0]!
    expect(es.url).toBe(CONVERSATION_STREAM_URL)
    es.emit('open')
    es.emit('conversation', '{"refId":42}')

    expect(onPing).toHaveBeenCalledWith(42)
  })

  it('ignores a malformed ping instead of throwing', () => {
    const onPing = vi.fn()
    host(onPing, vi.fn())

    FakeEventSource.instances[0]!.emit('conversation', 'not json')

    expect(onPing).not.toHaveBeenCalled()
  })

  it('reconnects with backoff after a drop and resyncs once the connection is back', () => {
    const onResync = vi.fn()
    const { api } = host(vi.fn(), onResync)
    const first = FakeEventSource.instances[0]!
    first.emit('open')

    first.emit('error')
    expect(first.closed).toBe(true)
    expect(api().reconnecting.value).toBe(true)
    expect(FakeEventSource.instances).toHaveLength(1)

    vi.advanceTimersByTime(STREAM_BACKOFF_START_MS)
    expect(FakeEventSource.instances).toHaveLength(2)

    FakeEventSource.instances[1]!.emit('open')
    expect(api().reconnecting.value).toBe(false)
    expect(onResync).toHaveBeenCalledOnce()
  })

  it('doubles the delay on repeated failures', () => {
    host(vi.fn(), vi.fn())
    FakeEventSource.instances[0]!.emit('error')
    vi.advanceTimersByTime(STREAM_BACKOFF_START_MS)
    FakeEventSource.instances[1]!.emit('error')

    vi.advanceTimersByTime(STREAM_BACKOFF_START_MS)
    expect(FakeEventSource.instances).toHaveLength(2)
    vi.advanceTimersByTime(STREAM_BACKOFF_START_MS)
    expect(FakeEventSource.instances).toHaveLength(3)
  })

  it('closes the stream and cancels pending reconnects on unmount', () => {
    const { wrapper } = host(vi.fn(), vi.fn())
    const es = FakeEventSource.instances[0]!
    es.emit('error')

    wrapper.unmount()
    vi.advanceTimersByTime(60_000)

    expect(es.closed).toBe(true)
    expect(FakeEventSource.instances).toHaveLength(1)
  })

  it('falls back to polling via onResync when EventSource does not exist', () => {
    vi.unstubAllGlobals()
    vi.stubGlobal('EventSource', undefined)
    const onResync = vi.fn()
    host(vi.fn(), onResync)

    vi.advanceTimersByTime(STREAM_POLL_MS * 2)

    expect(onResync).toHaveBeenCalledTimes(2)
  })
})
