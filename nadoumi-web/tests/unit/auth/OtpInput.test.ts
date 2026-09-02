import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import OtpInput from '~/components/auth/OtpInput.vue'

describe('OtpInput', () => {
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
    expect((w.emitted('update:modelValue')?.at(-1)?.[0] ?? '')).toBe('7')
  })

  it('does not emit complete until all boxes are filled', async () => {
    const w = await mountSuspended(OtpInput, { props: { modelValue: '', length: 6 } })
    const inputs = w.findAll('input')
    for (let i = 0; i < 5; i++) await inputs[i]!.setValue(String(i))
    expect(w.emitted('complete')).toBeFalsy()
    await inputs[5]!.setValue('5')
    expect(w.emitted('complete')?.at(-1)).toEqual(['012345'])
  })
})
