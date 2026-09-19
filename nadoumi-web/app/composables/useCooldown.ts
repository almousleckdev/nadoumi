/** A resend countdown: `start(seconds)` counts down to 0 and stops with the owning scope. */
export function useCooldown() {
  const seconds = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function start(total = 60) {
    seconds.value = total
    stop()
    timer = setInterval(() => {
      seconds.value -= 1
      if (seconds.value <= 0) stop()
    }, 1000)
  }

  if (getCurrentScope()) onScopeDispose(stop)

  return { seconds, start }
}
