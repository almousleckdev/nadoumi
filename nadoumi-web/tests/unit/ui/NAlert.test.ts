import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NAlert from '~/components/ui/NAlert.vue'

describe('NAlert', () => {
  it('shows the title and body slot', async () => {
    const w = await mountSuspended(NAlert, { props: { title: 'Heads up' }, slots: { default: () => 'details' } })
    expect(w.text()).toContain('Heads up')
    expect(w.text()).toContain('details')
  })

  it('uses role=alert for danger and role=status for info', async () => {
    const danger = await mountSuspended(NAlert, { props: { tone: 'danger' } })
    expect(danger.attributes('role')).toBe('alert')
    const info = await mountSuspended(NAlert, { props: { tone: 'info' } })
    expect(info.attributes('role')).toBe('status')
  })

  it('emits dismiss when the close button is clicked', async () => {
    const w = await mountSuspended(NAlert, { props: { dismissible: true } })
    await w.find('button[aria-label="Dismiss"]').trigger('click')
    expect(w.emitted('dismiss')).toHaveLength(1)
  })
})
