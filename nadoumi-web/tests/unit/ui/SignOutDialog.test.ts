import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import SignOutDialog from '~/components/ui/SignOutDialog.vue'

const signOut = vi.fn()
vi.mock('~/composables/useSession', () => ({ useSession: () => ({ signOut }) }))

const inBody = (selector: string) => document.body.querySelector(selector) as HTMLElement | null

describe('SignOutDialog', () => {
  beforeEach(() => {
    signOut.mockReset().mockResolvedValue(undefined)
    useSignOutConfirm().dismiss()
  })

  it('shows nothing until a sign-out is requested', async () => {
    const w = await mountSuspended(SignOutDialog)
    expect(inBody('[data-test="sign-out-confirm"]')).toBeNull()
    w.unmount()
  })

  it('asks first, and signs out only when the student confirms', async () => {
    const w = await mountSuspended(SignOutDialog)
    useSignOutConfirm().ask()
    await nextTick()

    expect(inBody('[data-test="sign-out-confirm"]')).not.toBeNull()
    expect(signOut).not.toHaveBeenCalled()

    inBody('[data-test="sign-out-confirm"]')!.click()
    await vi.waitFor(() => expect(signOut).toHaveBeenCalledTimes(1))
    expect(useSignOutConfirm().open.value).toBe(false)
    w.unmount()
  })

  it('keeps the session when the student chooses to stay signed in', async () => {
    const w = await mountSuspended(SignOutDialog)
    useSignOutConfirm().ask()
    await nextTick()

    inBody('[data-test="sign-out-stay"]')!.click()
    await nextTick()

    expect(signOut).not.toHaveBeenCalled()
    expect(useSignOutConfirm().open.value).toBe(false)
    w.unmount()
  })
})
