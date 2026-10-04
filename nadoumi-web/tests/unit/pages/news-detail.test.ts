import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import NewsDetail from '~/pages/news/[slug].vue'
import type { PublicArticleDetail } from '~/types/news'

const detail: PublicArticleDetail = {
  article: {
    slug: 'visa-tips', title: 'Visa tips for China', subtitle: 'What to prepare', language: 'en',
    coverUrl: 'https://res.cloudinary.com/demo/image/upload/v1/visa.jpg',
    authorName: 'Amina', publishedAt: '2026-09-01T10:00:00', commentCount: 2, readMinutes: 4,
  },
  bodyMd: '## Documents\n\nBring your **passport**.\n\n![Office](https://res.cloudinary.com/demo/image/upload/v1/office.jpg)\n\n<script>alert(1)</script>',
  comments: [
    {
      id: 1, authorName: 'Sara', body: 'Very helpful', deleted: false, createTime: '2026-09-02T08:00:00',
      replies: [
        { id: 2, parentId: 1, authorName: null, body: null, deleted: true, createTime: '2026-09-02T09:00:00',
          replies: [
            { id: 3, parentId: 2, authorName: 'Omar', body: 'Reply under a deleted one', deleted: false, createTime: '2026-09-02T10:00:00', replies: [] },
          ] },
      ],
    },
  ],
}

const publicGet = vi.fn()
const studentFetch = vi.fn()
mockNuxtImport('useApi', () => () => ({ publicGet, publicPost: vi.fn(), studentFetch }))

beforeEach(() => {
  publicGet.mockReset().mockResolvedValue(detail)
  studentFetch.mockReset().mockResolvedValue({ id: 9 })
  useSession().status.value = 'guest'
})

describe('news article page', () => {
  it('renders the markdown body with inline images and escapes raw HTML', async () => {
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    expect(w.text()).toContain('Visa tips for China')
    expect(w.find('.nad-article-body h2').text()).toBe('Documents')
    expect(w.find('.nad-article-body strong').text()).toBe('passport')
    expect(w.find('.nad-article-body img').attributes('src')).toBe('https://res.cloudinary.com/demo/image/upload/v1/office.jpg')
    expect(w.find('.nad-article-body script').exists()).toBe(false)
  })

  it('shows nested replies, keeps replies under a deleted comment, and hides its content', async () => {
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    expect(w.text()).toContain('Very helpful')
    expect(w.text()).toContain('This comment was deleted.')
    expect(w.text()).toContain('Reply under a deleted one')
  })

  it('asks guests to sign in instead of showing the comment form', async () => {
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    expect(w.text()).toContain('Sign in to join the discussion.')
    expect(w.find('textarea').exists()).toBe(false)
  })

  it('lets a signed-in user reply to a comment, posting its parent id', async () => {
    useSession().status.value = 'authed'
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    const replyButton = w.findAll('button').find(b => b.text() === 'Reply')!
    await replyButton.trigger('click')
    const boxes = w.findAll('textarea')
    await boxes[boxes.length - 1]!.setValue('Thanks Sara!')
    await w.findAll('form').at(-1)!.trigger('submit')
    await flushPromises()

    expect(studentFetch).toHaveBeenCalledWith('news/visa-tips/comments', {
      method: 'POST',
      body: { parentId: 1, body: 'Thanks Sara!' },
    })
  })

  it('renders a not-found state when the article is unpublished or missing', async () => {
    publicGet.mockRejectedValue(new Error('404'))
    const w = await mountSuspended(NewsDetail, { route: '/news/gone' })
    await flushPromises()

    expect(w.text()).toContain('Article not found')
  })
})
