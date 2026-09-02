import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NLocaleSwitcher from '~/components/ui/NLocaleSwitcher.vue'

describe('NLocaleSwitcher', () => {
  it('lists every configured locale', async () => {
    const w = await mountSuspended(NLocaleSwitcher)
    await w.find('button[aria-haspopup="menu"]').trigger('click')
    const text = w.text()
    for (const label of ['English', 'Français', 'العربية', '中文']) {
      expect(text).toContain(label)
    }
  })
})
