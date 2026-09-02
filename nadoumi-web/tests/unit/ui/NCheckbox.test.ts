import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NCheckbox from '~/components/ui/NCheckbox.vue'

describe('NCheckbox', () => {
  it('emits update:modelValue as boolean on change', async () => {
    const w = await mountSuspended(NCheckbox, { props: { id: 'x', modelValue: false } })
    const input = w.find('input').element as HTMLInputElement
    input.checked = true
    await w.find('input').trigger('change')
    expect(w.emitted('update:modelValue')?.[0]).toEqual([true])
  })
})
