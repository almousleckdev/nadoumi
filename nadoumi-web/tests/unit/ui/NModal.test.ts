import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NModal from '~/components/ui/NModal.vue'

describe('NModal', () => {
  it('renders nothing when closed', async () => {
    const w = await mountSuspended(NModal, { props: { modelValue: false } })
    expect(document.querySelector('[role="dialog"]')).toBeNull()
    w.unmount()
  })

  it('renders a dialog with the title when open', async () => {
    const w = await mountSuspended(NModal, { props: { modelValue: true, title: 'Confirm' } })
    const dialog = document.querySelector('[role="dialog"]')
    expect(dialog).not.toBeNull()
    expect(dialog?.textContent).toContain('Confirm')
    w.unmount()
  })

  it('emits close on Escape', async () => {
    // Mount closed then open so the real false->true transition runs
    // (the component's watch has no `immediate`, matching real app usage).
    const w = await mountSuspended(NModal, { props: { modelValue: false } })
    await w.setProps({ modelValue: true })
    await w.vm.$nextTick()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await w.vm.$nextTick()
    expect(w.emitted('update:modelValue')?.at(-1)).toEqual([false])
    w.unmount()
  })
})
