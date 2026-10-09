import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { useChatStream, CHAT_STREAM_URL, STREAM_BACKOFF_START_MS, STREAM_POLL_MS } from '~/composables/useChatStream'

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

const handlers = () => ({ message: vi.fn(), delivered: vi.fn(), read: vi.fn(), presence: vi.fn(), resync: vi.fn() })

function host(h0: ReturnType<typeof handlers>) {
  let api!: ReturnType<typeof useChatStream>
  const wrapper = mount(defineComponent({
    setup() {
      api = useChatStream(h0)
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

describe('useChatStream', () => {
  it('opens one stream on the BFF relay when mounted', () => {
    host(handlers())
    expect(FakeEventSource.instances).toHaveLength(1)
    expect(FakeEventSource.instances[0]!.url).toBe(CHAT_STREAM_URL)
  })

  it('hands each event\'s data to its own handler, without re-fetching anything', () => {
    const h0 = handlers()
    host(h0)
    const es = FakeEventSource.instances[0]!
    es.emit('message', JSON.stringify({ id: 9, conversationId: 3, body: 'hi' }))
    es.emit('delivered', JSON.stringify({ conversationId: 3, userId: 2, messageId: 9 }))
    es.emit('read', JSON.stringify({ conversationId: 3, userId: 2, messageId: 9 }))
    es.emit('presence', JSON.stringify({ userId: 2, online: true, lastSeenAt: null }))
    expect(h0.message).toHaveBeenCalledWith({ id: 9, conversationId: 3, body: 'hi' })
    expect(h0.delivered).toHaveBeenCalledWith({ conversationId: 3, userId: 2, messageId: 9 })
    expect(h0.read).toHaveBeenCalledOnce()
    expect(h0.presence).toHaveBeenCalledWith({ userId: 2, online: true, lastSeenAt: null })
  })

  it('ignores a malformed event instead of throwing', () => {
    const h0 = handlers()
    host(h0)
    expect(() => FakeEventSource.instances[0]!.emit('message', '{not json')).not.toThrow()
    expect(h0.message).not.toHaveBeenCalled()
  })

  it('reconnects with backoff after an error, reports it, and resyncs once the link is back', () => {
    const h0 = handlers()
    const { api } = host(h0)
    FakeEventSource.instances[0]!.emit('open')
    expect(api().connected.value).toBe(true)
    FakeEventSource.instances[0]!.emit('error')
    expect(api().reconnecting.value).toBe(true)
    expect(FakeEventSource.instances[0]!.closed).toBe(true)

    vi.advanceTimersByTime(STREAM_BACKOFF_START_MS)
    expect(FakeEventSource.instances).toHaveLength(2)
    FakeEventSource.instances[1]!.emit('open')
    expect(api().reconnecting.value).toBe(false)
    expect(h0.resync).toHaveBeenCalledOnce()
  })

  it('does not resync on the very first open', () => {
    const h0 = handlers()
    host(h0)
    FakeEventSource.instances[0]!.emit('open')
    expect(h0.resync).not.toHaveBeenCalled()
  })

  it('polls for resync where EventSource does not exist', () => {
    vi.stubGlobal('EventSource', undefined)
    const h0 = handlers()
    host(h0)
    vi.advanceTimersByTime(STREAM_POLL_MS * 2)
    expect(h0.resync).toHaveBeenCalledTimes(2)
  })

  it('closes the stream and stops retrying when the component unmounts', () => {
    const { wrapper } = host(handlers())
    const es = FakeEventSource.instances[0]!
    wrapper.unmount()
    expect(es.closed).toBe(true)
    es.emit('error')
    vi.advanceTimersByTime(STREAM_BACKOFF_START_MS * 4)
    expect(FakeEventSource.instances).toHaveLength(1)
  })
})
