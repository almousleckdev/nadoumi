import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import DocumentPreview from '~/components/onboarding/DocumentPreview.vue'

describe('DocumentPreview', () => {
  it('renders an image when the type is a known image mime', async () => {
    const w = await mountSuspended(DocumentPreview, { props: { src: 'data:image/png;base64,AAAA', type: 'image/png' } })
    expect(w.find('img').exists()).toBe(true)
  })

  it('renders a PDF file card, not an image, when the type is known to be a PDF', async () => {
    const w = await mountSuspended(DocumentPreview, { props: { src: 'https://cdn.example.com/x.pdf', type: 'application/pdf', name: 'x.pdf' } })
    expect(w.find('img').exists()).toBe(false)
    expect(w.text()).toContain('x.pdf')
    expect(w.text()).toContain('PDF')
  })

  it('falls back to a file card when the type is unknown and the image fails to load', async () => {
    const w = await mountSuspended(DocumentPreview, { props: { src: 'https://cdn.example.com/mystery', name: 'mystery-file' } })
    expect(w.find('img').exists()).toBe(true)

    await w.find('img').trigger('error')

    expect(w.find('img').exists()).toBe(false)
    expect(w.text()).toContain('mystery-file')
    const link = w.find('a')
    expect(link.attributes('href')).toBe('https://cdn.example.com/mystery')
  })

  it('tries again as an image when the src changes after a failure', async () => {
    const w = await mountSuspended(DocumentPreview, { props: { src: 'https://cdn.example.com/a' } })
    await w.find('img').trigger('error')
    expect(w.find('img').exists()).toBe(false)

    await w.setProps({ src: 'https://cdn.example.com/b' })

    expect(w.find('img').exists()).toBe(true)
  })
})
