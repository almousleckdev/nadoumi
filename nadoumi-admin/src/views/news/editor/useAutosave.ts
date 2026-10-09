import { onScopeDispose, ref, type Ref } from 'vue'

export type SaveState = 'idle' | 'dirty' | 'saving' | 'saved' | 'error'

interface Options {
  /** Persists the current document. Called with no arguments; read the latest state inside. */
  save: () => Promise<void>
  /** Quiet period after the last edit before a save starts. */
  delayMs?: number
}

export interface Autosave {
  state: Ref<SaveState>
  /** Call on every edit. */
  markDirty: () => void
  /** Saves now (cancelling the timer) and resolves once nothing is pending. Rethrows a failed save. */
  flush: () => Promise<void>
}

const DEFAULT_DELAY_MS = 1500

/**
 * Debounced autosave with a single save in flight. Edits made during a save are picked up by a
 * follow-up save, a failed save leaves the document dirty so the next edit retries it.
 */
export function useAutosave({ save, delayMs = DEFAULT_DELAY_MS }: Options): Autosave {
  const state = ref<SaveState>('idle')
  let timer: ReturnType<typeof setTimeout> | undefined
  let running: Promise<void> | null = null
  let dirty = false

  function clearTimer() {
    if (timer !== undefined) clearTimeout(timer)
    timer = undefined
  }

  async function run(): Promise<void> {
    while (dirty) {
      dirty = false
      state.value = 'saving'
      try {
        await save()
      }
      catch (error) {
        dirty = true
        state.value = 'error'
        throw error
      }
    }
    state.value = 'saved'
  }

  function start(): Promise<void> {
    clearTimer()
    running ??= run().finally(() => { running = null })
    return running
  }

  function markDirty() {
    dirty = true
    if (running) return // the running loop saves again once it has finished the current write
    state.value = 'dirty'
    clearTimer()
    // errors surface through `state`; nothing awaits a timer-driven save
    timer = setTimeout(() => { void start().catch(() => undefined) }, delayMs)
  }

  async function flush(): Promise<void> {
    if (running) await running.catch(() => undefined)
    if (!dirty) return
    await start()
  }

  onScopeDispose(() => {
    clearTimer()
    dirty = false
  })

  return { state, markDirty, flush }
}
