import { computed, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import { normalizeUsername, usernameProblem, type UsernameProblem } from '~/utils/usernameRules'

export const USERNAME_CHECK_DEBOUNCE_MS = 400

export type UsernameStatus = 'empty' | 'invalid' | 'checking' | 'available' | 'taken' | 'unknown'

/**
 * Live "is this username free?" check. Shape problems are reported instantly and locally; only a well-formed handle
 * is sent to the server, after the typing pauses, and a slow answer to an older handle never overwrites a newer one.
 * `unknown` (the check itself failed, e.g. rate limited) does not block: the server re-checks on submit.
 */
export function useUsernameCheck(username: Ref<string>) {
  const status = ref<UsernameStatus>('empty')
  const problem = computed<UsernameProblem | null>(() => {
    const value = normalizeUsername(username.value)
    return value ? usernameProblem(value) : null
  })
  let timer: ReturnType<typeof setTimeout> | undefined
  let sequence = 0

  async function check(value: string) {
    const mine = ++sequence
    status.value = 'checking'
    try {
      const res = await $fetch<{ valid?: boolean, available?: boolean }>('/api/student-username', { query: { username: value } })
      if (mine !== sequence) return
      // RuoYi can answer 200 with an error envelope ({ code: 401 }); that is a failed check, not "taken".
      if (typeof res?.valid !== 'boolean' || typeof res.available !== 'boolean') { status.value = 'unknown'; return }
      status.value = !res.valid ? 'invalid' : res.available ? 'available' : 'taken'
    }
    catch {
      if (mine === sequence) status.value = 'unknown'
    }
  }

  watch(username, (raw: string) => {
    if (timer) clearTimeout(timer)
    sequence++
    const value = normalizeUsername(raw)
    if (!value) { status.value = 'empty'; return }
    if (usernameProblem(value)) { status.value = 'invalid'; return }
    status.value = 'checking'
    timer = setTimeout(() => void check(value), USERNAME_CHECK_DEBOUNCE_MS)
  }, { immediate: true })

  onBeforeUnmount(() => timer && clearTimeout(timer))

  /** True when the handle may be submitted: well formed and not known to be taken. */
  const usable = computed(() => problem.value === null && !!normalizeUsername(username.value)
    && (status.value === 'available' || status.value === 'unknown'))
  return { status, problem, usable }
}
