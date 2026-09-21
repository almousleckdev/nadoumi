import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { EventEmitter } from 'node:events'

// Same harness as student-proxy.test.ts: Nitro auto-imports are installed as globals
// inside vi.hoisted so they exist before the route module is imported.
const state = vi.hoisted(() => {
  const s = { token: undefined as string | undefined }
  const g = globalThis as Record<string, unknown>
  g.defineEventHandler = (fn: unknown) => fn
  g.backendBaseUrl = () => 'http://backend'
  g.studentToken = () => s.token
  g.createError = (opts: { statusCode: number, statusMessage?: string }) => {
    const e = new Error(opts.statusMessage ?? 'error') as Error & { statusCode?: number }
    e.statusCode = opts.statusCode
    return e
  }
  return s
})

const { default: stream } = await import('~~/server/api/student-stream.get')

function fakeEvent() {
  const req = new EventEmitter()
  const res = {
    writeHead: vi.fn(),
    flushHeaders: vi.fn(),
    write: vi.fn(),
    end: vi.fn(),
  }
  return { event: { node: { req, res } }, req, res }
}

const decode = (v: unknown) => new TextDecoder().decode(v as Uint8Array)
const tick = () => new Promise(r => setTimeout(r, 0))

let fetchMock: ReturnType<typeof vi.fn>

beforeEach(() => {
  state.token = 'jwt-abc'
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
})
afterEach(() => vi.unstubAllGlobals())

describe('student stream relay', () => {
  it('rejects a caller who is not signed in with 401 and never calls the backend', async () => {
    state.token = undefined
    const { event } = fakeEvent()

    await expect(stream(event as never)).rejects.toMatchObject({ statusCode: 401 })
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('attaches the bearer server-side and relays each chunk as it arrives, not at the end', async () => {
    let controller!: ReadableStreamDefaultController<Uint8Array>
    const body = new ReadableStream<Uint8Array>({ start: (c) => { controller = c } })
    fetchMock.mockResolvedValue(new Response(body, { status: 200 }))
    const { event, res } = fakeEvent()

    const done = stream(event as never)
    await tick()

    expect(fetchMock).toHaveBeenCalledWith('http://backend/api/student/stream', expect.objectContaining({
      headers: { authorization: 'Bearer jwt-abc', accept: 'text/event-stream' },
    }))

    controller.enqueue(new TextEncoder().encode('event: conversation\ndata: {"refId":9}\n\n'))
    await tick()
    // relayed while the upstream is still open: this is what "unbuffered" means
    expect(res.write).toHaveBeenCalledTimes(1)
    expect(decode(res.write.mock.calls[0]![0])).toContain('"refId":9')
    expect(res.end).not.toHaveBeenCalled()

    controller.close()
    await done
    expect(res.end).toHaveBeenCalledOnce()
  })

  it('sends SSE headers that stop proxies from buffering or transforming the stream', async () => {
    fetchMock.mockResolvedValue(new Response(new ReadableStream({ start: c => c.close() }), { status: 200 }))
    const { event, res } = fakeEvent()

    await stream(event as never)

    expect(res.writeHead).toHaveBeenCalledWith(200, expect.objectContaining({
      'Content-Type': 'text/event-stream; charset=utf-8',
      'Cache-Control': 'no-cache, no-transform',
      'X-Accel-Buffering': 'no',
    }))
    expect(res.flushHeaders).toHaveBeenCalled()
  })

  it('aborts the upstream request when the browser disconnects', async () => {
    let signal: AbortSignal | undefined
    fetchMock.mockImplementation((_url: string, init: { signal: AbortSignal }) => {
      signal = init.signal
      // like a real fetch body: errors as soon as the request is aborted
      const body = new ReadableStream({
        start: c => init.signal.addEventListener('abort', () => c.error(new DOMException('aborted', 'AbortError'))),
      })
      return Promise.resolve(new Response(body, { status: 200 }))
    })
    const { event, req, res } = fakeEvent()

    const done = stream(event as never)
    await tick()
    expect(signal!.aborted).toBe(false)

    req.emit('close')
    await done
    expect(signal!.aborted).toBe(true)
    expect(res.end).toHaveBeenCalledOnce()
  })

  it('surfaces an upstream refusal (e.g. expired token) with its status instead of an empty stream', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }))
    const { event, res } = fakeEvent()

    await expect(stream(event as never)).rejects.toMatchObject({ statusCode: 401 })
    expect(res.writeHead).not.toHaveBeenCalled()
  })

  it('answers 502 when the backend is unreachable', async () => {
    fetchMock.mockRejectedValue(new TypeError('fetch failed'))
    const { event } = fakeEvent()

    await expect(stream(event as never)).rejects.toMatchObject({ statusCode: 502 })
  })
})
