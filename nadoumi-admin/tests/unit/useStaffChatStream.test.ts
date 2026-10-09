import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { useStaffChatStream, STAFF_STREAM_URL, STREAM_BACKOFF_START_MS, STREAM_POLL_MS } from '@/composables/useStaffChatStream'

class FakeEventSource {
  static instances: FakeEventSource[] = []
  closed = false
  private listeners = new Map<string, ((e: Event) => void)[]>()
  constructor(public url: string, public init?: EventSourceInit) {
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

const handlers = () => ({ message: vi.fn(), delivered: vi.fn(), read: vi.fn(), presence: vi.fn(), removed: vi.fn(), resync: vi.fn() })

function host(h0: ReturnType<typeof handlers>) {
  let api!: ReturnType<typeof useStaffChatStream>
  const wrapper = mount(defineComponent({
    setup() {
      api = useStaffChatStream(h0)
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

describe('useStaffChatStream', () => {
  it('opens one credentialed stream on the staff endpoint', () => {
    host(handlers())
    expect(FakeEventSource.instances).toHaveLength(1)
    expect(FakeEventSource.instances[0]!.url).toBe(STAFF_STREAM_URL)
    expect(FakeEventSource.instances[0]!.init).toEqual({ withCredentials: true })
  })

  it('hands each event\'s data to its own handler', () => {
    const h0 = handlers()
    host(h0)
    const es = FakeEventSource.instances[0]!
    es.emit('message', JSON.stringify({ id: 4, conversationId: 2, body: 'hi' }))
    es.emit('delivered', JSON.stringify({ conversationId: 2, userId: 9, messageId: 4 }))
    es.emit('read', JSON.stringify({ conversationId: 2, userId: 9, messageId: 4 }))
    es.emit('presence', JSON.stringify({ userId: 9, online: false, lastSeenAt: '2026-10-09T08:00:00Z' }))
    expect(h0.message).toHaveBeenCalledWith({ id: 4, conversationId: 2, body: 'hi' })
    expect(h0.delivered).toHaveBeenCalledOnce()
    expect(h0.read).toHaveBeenCalledOnce()
    expect(h0.presence).toHaveBeenCalledWith({ userId: 9, online: false, lastSeenAt: '2026-10-09T08:00:00Z' })
  })

  it('ignores a malformed event', () => {
    const h0 = handlers()
    host(h0)
    expect(() => FakeEventSource.instances[0]!.emit('message', '{nope')).not.toThrow()
    expect(h0.message).not.toHaveBeenCalled()
  })

  it('reconnects with backoff and resyncs once the link is back', () => {
    const h0 = handlers()
    const { api } = host(h0)
    FakeEventSource.instances[0]!.emit('open')
    FakeEventSource.instances[0]!.emit('error')
    expect(api().reconnecting.value).toBe(true)
    vi.advanceTimersByTime(STREAM_BACKOFF_START_MS)
    expect(FakeEventSource.instances).toHaveLength(2)
    FakeEventSource.instances[1]!.emit('open')
    expect(api().reconnecting.value).toBe(false)
    expect(h0.resync).toHaveBeenCalledOnce()
  })

  it('polls for resync where EventSource does not exist', () => {
    vi.stubGlobal('EventSource', undefined)
    const h0 = handlers()
    host(h0)
    vi.advanceTimersByTime(STREAM_POLL_MS * 2)
    expect(h0.resync).toHaveBeenCalledTimes(2)
  })

  it('stops for good when unmounted', () => {
    const { wrapper } = host(handlers())
    const es = FakeEventSource.instances[0]!
    wrapper.unmount()
    expect(es.closed).toBe(true)
    es.emit('error')
    vi.advanceTimersByTime(STREAM_BACKOFF_START_MS * 4)
    expect(FakeEventSource.instances).toHaveLength(1)
  })
})
