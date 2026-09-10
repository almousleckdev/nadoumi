import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import GalleryLightbox from '~/components/catalog/GalleryLightbox.vue'
import type { UniversityGalleryImage } from '~/types/catalog'

const images: UniversityGalleryImage[] = [
  { id: 1, imageUrl: '/x/a.png', url: 'https://cdn/a.jpg', caption: 'Alpha' },
  { id: 2, imageUrl: '/x/b.png', url: 'https://cdn/b.jpg', caption: 'Beta' },
  { id: 3, imageUrl: '/x/c.png', url: 'https://cdn/c.jpg', caption: null },
]

describe('GalleryLightbox', () => {
  it('renders nothing when modelValue is null', async () => {
    const w = await mountSuspended(GalleryLightbox, { props: { images, modelValue: null } })
    expect(document.querySelector('[role="dialog"]')).toBeNull()
    w.unmount()
  })

  it('shows the image at the given index with its caption and counter', async () => {
    const w = await mountSuspended(GalleryLightbox, { props: { images, modelValue: null } })
    await w.setProps({ modelValue: 1 })
    await w.vm.$nextTick()

    const dialog = document.querySelector('[role="dialog"]')
    expect(dialog).not.toBeNull()
    expect(dialog?.querySelector('img')?.getAttribute('src')).toBe('https://cdn/b.jpg')
    expect(dialog?.textContent).toContain('Beta')
    expect(dialog?.textContent).toContain('2 / 3')
    w.unmount()
  })

  it('wraps around with the arrow keys', async () => {
    const w = await mountSuspended(GalleryLightbox, { props: { images, modelValue: null } })
    await w.setProps({ modelValue: 2 })
    await w.vm.$nextTick()

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }))
    expect(w.emitted('update:modelValue')?.at(-1)).toEqual([0])

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft' }))
    expect(w.emitted('update:modelValue')?.at(-1)).toEqual([1])
    w.unmount()
  })

  it('closes on Escape', async () => {
    const w = await mountSuspended(GalleryLightbox, { props: { images, modelValue: null } })
    await w.setProps({ modelValue: 0 })
    await w.vm.$nextTick()

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(w.emitted('update:modelValue')?.at(-1)).toEqual([null])
    w.unmount()
  })
})
