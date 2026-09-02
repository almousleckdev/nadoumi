import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Avatar from '@/components/ui/Avatar.vue'

describe('Avatar', () => {
  it('shows two-letter initials from a full name', () => {
    expect(mount(Avatar, { props: { name: 'Amina Diallo' } }).text()).toBe('AD')
  })

  it('shows one letter for a single-word name and ? for blank', () => {
    expect(mount(Avatar, { props: { name: 'Cher' } }).text()).toBe('C')
    expect(mount(Avatar, { props: { name: '' } }).text()).toBe('?')
  })

  it('renders an image when src is given', () => {
    const w = mount(Avatar, { props: { name: 'A B', src: 'http://x/y.png' } })
    expect(w.find('img').attributes('src')).toBe('http://x/y.png')
    expect(w.find('img').attributes('alt')).toBe('A B')
  })

  it('sizes from the size prop', () => {
    const w = mount(Avatar, { props: { name: 'A B', size: 48 } })
    expect(w.attributes('style')).toContain('width: 48px')
  })
})
