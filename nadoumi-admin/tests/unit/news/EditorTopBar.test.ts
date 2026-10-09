import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { mountOpts } from '../../helpers'
import EditorTopBar from '@/views/news/editor/EditorTopBar.vue'

const base = {
  label: 'Draft', status: 'Saved', statusTone: 'normal' as const, userName: 'Almousleck',
  primaryVisible: true, primaryLabel: 'Publish', primaryIdle: false, primaryBusy: false, menu: [],
}

describe('EditorTopBar', () => {
  it("shows the writer's profile photo when they have one, not just their initial", () => {
    const w = mount(EditorTopBar, { ...mountOpts(), props: { ...base, userAvatar: 'https://cdn.example/me.jpg' } })
    expect(w.findComponent({ name: 'Avatar' }).props('src')).toBe('https://cdn.example/me.jpg')
    expect(w.find('img').attributes('src')).toBe('https://cdn.example/me.jpg')
  })

  it('falls back to the initial when the writer has no photo', () => {
    const w = mount(EditorTopBar, { ...mountOpts(), props: base })
    expect(w.findComponent({ name: 'Avatar' }).props('src')).toBeUndefined()
    expect(w.find('img').exists()).toBe(false)
  })
})
