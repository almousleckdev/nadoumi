import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import ConsentCheckboxes from '~/components/auth/ConsentCheckboxes.vue'

describe('ConsentCheckboxes', () => {
  it('renders a Terms and a Privacy checkbox', async () => {
    const w = await mountSuspended(ConsentCheckboxes, {
      props: { modelValue: { terms: false, privacy: false } },
    })
    expect(w.find('#accept-terms').exists()).toBe(true)
    expect(w.find('#accept-privacy').exists()).toBe(true)
  })

  it('emits the updated model when a box is toggled', async () => {
    const w = await mountSuspended(ConsentCheckboxes, {
      props: { modelValue: { terms: false, privacy: false } },
    })
    await w.find('#accept-terms').setValue(true)
    expect(w.emitted('update:modelValue')?.[0]?.[0]).toEqual({ terms: true, privacy: false })
  })

  it('shows the consent-required message only when invalid and something is missing', async () => {
    const missing = await mountSuspended(ConsentCheckboxes, {
      props: { modelValue: { terms: true, privacy: false }, invalid: true },
    })
    expect(missing.text()).toContain('accept the Terms and the Privacy Policy')

    const ok = await mountSuspended(ConsentCheckboxes, {
      props: { modelValue: { terms: true, privacy: true }, invalid: true },
    })
    expect(ok.text()).not.toContain('accept the Terms and the Privacy Policy')
  })
})
