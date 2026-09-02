import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NDropdown from '~/components/ui/NDropdown.vue'

describe('NDropdown', () => {
  it('toggles the menu and sets aria-expanded', async () => {
    const w = await mountSuspended(NDropdown, { props: { label: 'Account' }, slots: { default: () => '<a href="#">Item</a>' } })
    const trigger = w.find('button[aria-haspopup="menu"]')
    expect(trigger.attributes('aria-expanded')).toBe('false')
    await trigger.trigger('click')
    expect(trigger.attributes('aria-expanded')).toBe('true')
    expect(w.find('[role="menu"]').exists()).toBe(true)
  })
})
