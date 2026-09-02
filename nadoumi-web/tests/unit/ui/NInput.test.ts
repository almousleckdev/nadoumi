import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NInput from '~/components/ui/NInput.vue'

describe('NInput', () => {
  it('emits update:modelValue on input', async () => {
    const w = await mountSuspended(NInput, { props: { id: 'a', modelValue: '' } })
    await w.find('input').setValue('hello')
    expect(w.emitted('update:modelValue')?.[0]).toEqual(['hello'])
  })

  it('wires aria-describedby to both hint and error ids', async () => {
    const w = await mountSuspended(NInput, { props: { id: 'a', modelValue: '' } })
    expect(w.find('input').attributes('aria-describedby')).toBe('a-hint a-error')
  })

  it('marks aria-invalid only when invalid is true', async () => {
    const w = await mountSuspended(NInput, { props: { id: 'a', modelValue: '', invalid: true } })
    expect(w.find('input').attributes('aria-invalid')).toBe('true')
    expect(w.find('input').attributes('aria-describedby')).toContain('a-error')
  })
})
