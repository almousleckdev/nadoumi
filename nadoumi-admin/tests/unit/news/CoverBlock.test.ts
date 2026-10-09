import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { mountOpts } from '../../helpers'
import CoverBlock from '@/views/news/editor/CoverBlock.vue'

const mountCover = (props: { coverUrl: string, uploading: boolean, disabled?: boolean }) =>
  mount(CoverBlock, { ...mountOpts(), props: { disabled: false, ...props } })

describe('CoverBlock', () => {
  it('shows a loading placeholder, not the Add button, while the first cover uploads', () => {
    const w = mountCover({ coverUrl: '', uploading: true })
    expect(w.find('[data-test="cover-loading"]').exists()).toBe(true)
    expect(w.find('.cover__add').exists()).toBe(false)
  })

  it('dims the current cover and shows a spinner over it while a replacement uploads', () => {
    const w = mountCover({ coverUrl: 'https://x/c.jpg', uploading: true })
    expect(w.find('[data-test="cover-loading"]').exists()).toBe(true)
    expect(w.find('.cover__img').classes()).toContain('cover__img--busy')
    expect(w.find('.cover__btn').attributes('disabled')).toBeDefined()
  })

  it('shows no loading effect once the upload is done', () => {
    const w = mountCover({ coverUrl: 'https://x/c.jpg', uploading: false })
    expect(w.find('[data-test="cover-loading"]').exists()).toBe(false)
    expect(w.find('.cover__img').classes()).not.toContain('cover__img--busy')
  })

  it('offers the Add button when there is no cover and nothing uploading', () => {
    const w = mountCover({ coverUrl: '', uploading: false })
    expect(w.find('.cover__add').exists()).toBe(true)
  })
})
