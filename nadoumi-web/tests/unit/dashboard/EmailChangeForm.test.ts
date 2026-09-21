import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import EmailChangeForm from '~/components/dashboard/EmailChangeForm.vue'

const requestCode = vi.fn()
const confirm = vi.fn()
const cooldown = ref(0)

vi.mock('~/composables/useEmailChange', () => ({
  useEmailChange: () => ({ requestCode, confirm, cooldown, busy: ref(false) }),
}))

async function mountForm() {
  return mountSuspended(EmailChangeForm, { props: { currentEmail: 'old@example.com' } })
}

beforeEach(() => {
  requestCode.mockReset().mockResolvedValue({ throttled: false, retryAfter: 60 })
  confirm.mockReset().mockResolvedValue(undefined)
  cooldown.value = 0
})

describe('EmailChangeForm', () => {
  it('disables Send code until a different, valid email is entered', async () => {
    const w = await mountForm()
    const send = () => w.findAll('button').find(b => b.text() === 'Send code')!

    expect(send().attributes('disabled')).toBeDefined()

    await w.find('#email-new').setValue('old@example.com')
    expect(send().attributes('disabled')).toBeDefined()

    await w.find('#email-new').setValue('not-an-email')
    expect(send().attributes('disabled')).toBeDefined()

    await w.find('#email-new').setValue('new@example.com')
    expect(send().attributes('disabled')).toBeUndefined()
  })

  it('requests a code and moves to the confirmation stage', async () => {
    const w = await mountForm()
    await w.find('#email-new').setValue('new@example.com')
    await w.findAll('button').find(b => b.text() === 'Send code')!.trigger('click')
    await flushPromises()

    expect(requestCode).toHaveBeenCalledWith('new@example.com')
    expect(w.find('[role="group"]').exists()).toBe(true)
    expect(w.find('#email-change-current-password').exists()).toBe(true)
  })

  it('requires both a complete code and the current password before confirming', async () => {
    const w = await mountForm()
    await w.find('#email-new').setValue('new@example.com')
    await w.findAll('button').find(b => b.text() === 'Send code')!.trigger('click')
    await flushPromises()
    const confirmButton = () => w.findAll('button').find(b => b.text() === 'Confirm email change')!

    expect(confirmButton().attributes('disabled')).toBeDefined()

    for (const [i, input] of w.findAll('[role="group"] input').entries()) {
      await input.setValue(String(i + 1))
    }
    expect(confirmButton().attributes('disabled')).toBeDefined()

    await w.find('#email-change-current-password').setValue('MyPass1!')
    expect(confirmButton().attributes('disabled')).toBeUndefined()
  })

  it('confirms the change and emits changed with the new email', async () => {
    const w = await mountForm()
    await w.find('#email-new').setValue('new@example.com')
    await w.findAll('button').find(b => b.text() === 'Send code')!.trigger('click')
    await flushPromises()
    for (const [i, input] of w.findAll('[role="group"] input').entries()) {
      await input.setValue(String(i + 1))
    }
    await w.find('#email-change-current-password').setValue('MyPass1!')

    await w.findAll('button').find(b => b.text() === 'Confirm email change')!.trigger('click')
    await flushPromises()

    expect(confirm).toHaveBeenCalledWith('new@example.com', '123456', 'MyPass1!')
    expect(w.emitted('changed')).toEqual([['new@example.com']])
  })

  it('shows a server error and clears the code without losing the entered email', async () => {
    confirm.mockRejectedValue({ statusCode: 400, data: { detail: 'verification code is invalid or expired' } })
    const w = await mountForm()
    await w.find('#email-new').setValue('new@example.com')
    await w.findAll('button').find(b => b.text() === 'Send code')!.trigger('click')
    await flushPromises()
    for (const [i, input] of w.findAll('[role="group"] input').entries()) {
      await input.setValue(String(i + 1))
    }
    await w.find('#email-change-current-password').setValue('MyPass1!')
    await w.findAll('button').find(b => b.text() === 'Confirm email change')!.trigger('click')
    await flushPromises()

    expect(w.text()).toContain('That verification code is invalid or has expired')
    expect(w.emitted('changed')).toBeUndefined()
  })

  it('Cancel returns to the idle stage', async () => {
    const w = await mountForm()
    await w.find('#email-new').setValue('new@example.com')
    await w.findAll('button').find(b => b.text() === 'Send code')!.trigger('click')
    await flushPromises()

    await w.findAll('button').find(b => b.text() === 'Cancel')!.trigger('click')

    expect(w.find('#email-new').exists()).toBe(true)
    expect(w.find('[role="group"]').exists()).toBe(false)
  })
})
