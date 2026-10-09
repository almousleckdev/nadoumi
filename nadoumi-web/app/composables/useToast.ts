export type ToastKind = 'success' | 'error' | 'info'
export interface Toast { id: number, kind: ToastKind, message: string }

const DURATION_MS: Record<ToastKind, number> = { success: 3500, info: 4500, error: 6000 }
const MAX_VISIBLE = 4
/** The same message shown again inside this window is the same event, not a new toast. */
const DUPLICATE_WINDOW_MS = 800

let nextId = 1
const timers = new Map<number, ReturnType<typeof setTimeout>>()
const lastShown = new Map<string, number>()

/**
 * Short-lived notifications ("Saved", "Photo uploaded"). State is shared app-wide, rendered once by `ToastHost`
 * in the layouts, and every toast dismisses itself. Client-side timers only: nothing is scheduled during SSR.
 */
export function useToast() {
  const toasts = useState<Toast[]>('nad-toasts', () => [])

  function dismiss(id: number) {
    const timer = timers.get(id)
    if (timer) clearTimeout(timer)
    timers.delete(id)
    toasts.value = toasts.value.filter(t => t.id !== id)
  }

  function show(kind: ToastKind, message: string) {
    if (!message) return
    const now = Date.now()
    const key = `${kind}:${message}`
    if (now - (lastShown.get(key) ?? 0) < DUPLICATE_WINDOW_MS) return
    lastShown.set(key, now)
    const toast: Toast = { id: nextId++, kind, message }
    toasts.value = [...toasts.value, toast].slice(-MAX_VISIBLE)
    if (import.meta.client) timers.set(toast.id, setTimeout(() => dismiss(toast.id), DURATION_MS[kind]))
  }

  return {
    toasts,
    dismiss,
    success: (message: string) => show('success', message),
    error: (message: string) => show('error', message),
    info: (message: string) => show('info', message),
  }
}
