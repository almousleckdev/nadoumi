import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { nextTick } from 'vue'
import NAvatar from '~/components/ui/NAvatar.vue'

describe('NAvatar', () => {
  it('shows initials when there is no photo', async () => {
    const w = await mountSuspended(NAvatar, { props: { name: 'Ava Chen' } })

    expect(w.find('img').exists()).toBe(false)
    expect(w.text()).toBe('AC')
  })

  it('shows the photo when there is one', async () => {
    const w = await mountSuspended(NAvatar, { props: { name: 'Jane Smith', src: 'https://cdn.example/j.jpg' } })

    expect(w.find('img').attributes('src')).toBe('https://cdn.example/j.jpg')
    expect(w.find('img').attributes('loading')).toBe('lazy')
  })

  it('falls back to initials when the photo cannot be loaded', async () => {
    const w = await mountSuspended(NAvatar, { props: { name: 'Jane Smith', src: 'https://cdn.example/gone.jpg' } })

    await w.find('img').trigger('error')
    await nextTick()

    expect(w.find('img').exists()).toBe(false)
    expect(w.text()).toBe('JS')
  })

  it('tries a new photo after the old one failed', async () => {
    const w = await mountSuspended(NAvatar, { props: { name: 'Jane Smith', src: 'https://cdn.example/gone.jpg' } })
    await w.find('img').trigger('error')

    await w.setProps({ src: 'https://cdn.example/new.jpg' })

    expect(w.find('img').attributes('src')).toBe('https://cdn.example/new.jpg')
  })
})
