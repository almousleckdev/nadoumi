const TICK_MS = 1000

/**
 * Counts down from the server's `secondsLeft` and calls `onElapsed` once at zero. The server owns the window;
 * this only ticks the display, and a fresh `secondsLeft` (after a refresh) restarts it, so a wrong device clock
 * cannot extend the time.
 */
export function usePaymentCountdown(secondsLeft: Ref<number>, onElapsed: () => void) {
  const remaining = ref(secondsLeft.value)
  let timer: ReturnType<typeof setInterval> | null = null

  function stop() {
    if (timer !== null) {
      clearInterval(timer)
      timer = null
    }
  }

  function start() {
    stop()
    remaining.value = secondsLeft.value
    if (remaining.value <= 0) return
    timer = setInterval(() => {
      remaining.value = Math.max(0, remaining.value - 1)
      if (remaining.value === 0) {
        stop()
        onElapsed()
      }
    }, TICK_MS)
  }

  watch(secondsLeft, start, { immediate: true })
  onBeforeUnmount(stop)

  return { remaining }
}
