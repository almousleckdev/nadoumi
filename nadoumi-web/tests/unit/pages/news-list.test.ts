import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import NewsList from '~/pages/news/index.vue'
import type { Page } from '~/types/catalog'
import type { ArticleSummary } from '~/types/news'

const publicGet = vi.fn<(p: string, q?: Record<string, unknown>) => Promise<unknown>>()
mockNuxtImport('useApi', () => () => ({ publicGet, publicPost: vi.fn(), studentFetch: vi.fn() }))

beforeEach(() => publicGet.mockReset())

function page(content: ArticleSummary[]): Page<ArticleSummary> {
  return { content, page: 0, size: 10, totalElements: content.length, totalPages: 1 }
}

describe('news feed', () => {
  it('renders article cards with title, subtitle and cover from the public API', async () => {
    publicGet.mockResolvedValue(page([{
      slug: 'visa-tips', title: 'Visa tips for China', subtitle: 'What to prepare', language: 'en',
      coverUrl: 'https://res.cloudinary.com/demo/image/upload/v1/visa.jpg',
      authorName: 'Amina', publishedAt: '2026-09-01T10:00:00', commentCount: 3, readMinutes: 4,
    }]))
    const w = await mountSuspended(NewsList)
    await flushPromises()

    expect(publicGet).toHaveBeenCalledWith('news', expect.objectContaining({ page: 0, size: 10 }))
    expect(w.text()).toContain('Visa tips for China')
    expect(w.text()).toContain('What to prepare')
    expect(w.text()).toContain('4 min read')
    expect(w.find('a[href="/news/visa-tips"]').exists()).toBe(true)
    expect(w.findAll('img').map(i => i.attributes('src'))).toContain('https://res.cloudinary.com/demo/image/upload/v1/visa.jpg')
  })

  it('shows the empty state when nothing is published', async () => {
    publicGet.mockResolvedValue(page([]))
    const w = await mountSuspended(NewsList)
    await flushPromises()

    expect(w.text()).toContain('No articles yet')
  })
})
