// BFF relay for the student's realtime stream (`text/event-stream`).
//
// The browser's EventSource cannot send an Authorization header and never sees the
// httpOnly JWT, so this route attaches the bearer server-side and relays the
// upstream body chunk by chunk. Unlike the generic `/api/student/**` proxy it
//  - aborts the upstream request as soon as the browser disconnects (the Spring
//    emitter lives 30 minutes; without this every closed tab would pin a connection),
//  - sends anti-buffering headers so proxies/CDNs flush events immediately,
//  - writes an SSE comment heartbeat so intermediaries do not idle-close the socket.
const HEARTBEAT_MS = 25_000

export default defineEventHandler(async (event) => {
  const token = studentToken(event)
  if (!token) {
    throw createError({ statusCode: 401, statusMessage: 'Not signed in' })
  }

  const abort = new AbortController()
  const res = event.node.res
  event.node.req.on('close', () => abort.abort())

  let upstream: Response
  try {
    upstream = await fetch(`${backendBaseUrl(event)}/api/student/stream`, {
      headers: { authorization: `Bearer ${token}`, accept: 'text/event-stream' },
      signal: abort.signal,
    })
  }
  catch {
    throw createError({ statusCode: 502, statusMessage: 'Bad Gateway' })
  }
  if (!upstream.ok || !upstream.body) {
    throw createError({ statusCode: upstream.ok ? 502 : upstream.status, statusMessage: 'Stream unavailable' })
  }

  res.writeHead(200, {
    'Content-Type': 'text/event-stream; charset=utf-8',
    'Cache-Control': 'no-cache, no-transform',
    Connection: 'keep-alive',
    'X-Accel-Buffering': 'no',
  })
  res.flushHeaders?.()

  const heartbeat = setInterval(() => res.write(': keep-alive\n\n'), HEARTBEAT_MS)
  const reader = upstream.body.getReader()
  try {
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      res.write(value)
    }
  }
  catch {
    // client went away (abort) or the upstream dropped: the browser reconnects itself
  }
  finally {
    clearInterval(heartbeat)
    res.end()
  }
})
