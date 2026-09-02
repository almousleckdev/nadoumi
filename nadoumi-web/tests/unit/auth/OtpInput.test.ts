import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import OtpInput from '~/components/auth/OtpInput.vue'

describe('OtpInput', () => {
  it('exposes an accessible group with per-digit labels', async () => {
    const w = await mountSuspended(OtpInput, { props: { modelValue: '', length: 6 } })
    const group = w.find('[role="group"]')
    expect(group.exists()).toBe(true)
    expect(group.attributes('aria-label')).toBeTruthy()
    const inputs = w.findAll('input')
    expect(inputs).toHaveLength(6)
    expect(inputs[0]!.attributes('aria-label')).toMatch(/1/)
  })

  it('fills every box from a pasted code and emits update + complete', async () => {
    const w = await mountSuspended(OtpInput, { props: { modelValue: '', length: 6 } })
    await w.findAll('input')[0]!.trigger('paste', {
      clipboardData: { getData: () => '482913' },
    } as unknown as ClipboardEvent)
    expect(w.emitted('update:modelValue')?.at(-1)).toEqual(['482913'])
    expect(w.emitted('complete')?.at(-1)).toEqual(['482913'])
  })

  it('keeps only the last digit typed and ignores non-digits', async () => {
    const w = await mountSuspended(OtpInput, { props: { modelValue: '', length: 6 } })
    const first = w.findAll('input')[0]!
    await first.setValue('a')
    await first.setValue('7')
    expect(w.emitted('update:modelValue')?.at(-1)?.[0] ?? '').toBe('7')
  })

  it('locks the boxes while busy', async () => {
    const w = await mountSuspended(OtpInput, { props: { modelValue: '123456', length: 6, busy: true } })
    expect(w.find('[role="group"]').attributes('aria-busy')).toBe('true')
    expect(w.findAll('input')[0]!.attributes('disabled')).toBeDefined()
  })
})
