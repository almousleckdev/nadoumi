import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises, type VueWrapper } from '@vue/test-utils'
import NewsDetail from '~/pages/news/[slug].vue'
import type { ArticleSummary, PublicArticleDetail } from '~/types/news'

const detail: PublicArticleDetail = {
  article: {
    slug: 'visa-tips', title: 'Visa tips for China', subtitle: 'What to prepare', language: 'en',
    coverUrl: 'https://res.cloudinary.com/demo/image/upload/v1/visa.jpg',
    authorName: 'Amina', authorAvatarUrl: 'https://cdn.example/amina.jpg',
    publishedAt: '2026-09-01T10:00:00', commentCount: 2, likeCount: 5, readMinutes: 4,
  },
  bodyMd: '## Documents\n\nBring your **passport**.\n\n![Office](https://res.cloudinary.com/demo/image/upload/v1/office.jpg)\n\n<script>alert(1)</script>',
  comments: [
    {
      id: 1, authorName: 'Sara', body: 'Very helpful', deleted: false, likeCount: 3, createTime: '2026-09-02T08:00:00',
      replies: [
        { id: 2, parentId: 1, authorName: null, body: null, deleted: true, likeCount: 0, createTime: '2026-09-02T09:00:00',
          replies: [
            { id: 3, parentId: 2, authorName: 'Omar', body: 'Reply under a deleted one', deleted: false, likeCount: 0, createTime: '2026-09-02T10:00:00', replies: [] },
          ] },
      ],
    },
    { id: 4, parentId: null, authorName: null, authorAvatarUrl: null, body: 'From a departed account', deleted: false, likeCount: 0, createTime: '2026-09-03T08:00:00', replies: [] },
    { id: 5, parentId: null, authorName: 'Giulia', authorAvatarUrl: 'https://cdn.example/giulia.jpg', body: 'Staff reply', deleted: false, likeCount: 0, createTime: '2026-09-03T09:00:00', replies: [] },
  ],
}

const related: ArticleSummary[] = [
  {
    slug: 'student-housing', title: 'Student housing in Chengdu', language: 'en', coverUrl: 'https://cdn.example/housing.jpg',
    authorName: 'Jane Smith', authorAvatarUrl: 'https://cdn.example/jane.jpg',
    publishedAt: '2026-08-20T10:00:00', commentCount: 1, likeCount: 7, readMinutes: 3,
  },
  {
    slug: 'visa-checklist', title: 'Visa checklist', language: 'en', coverUrl: null,
    authorName: 'Jane Smith', authorAvatarUrl: null, publishedAt: '2026-08-10T10:00:00', commentCount: 0, likeCount: 0, readMinutes: 2,
  },
]

const publicGet = vi.fn()
const studentFetch = vi.fn()
const navigateToMock = vi.hoisted(() => vi.fn())
mockNuxtImport('useApi', () => () => ({ publicGet, publicPost: vi.fn(), studentFetch }))
mockNuxtImport('navigateTo', () => navigateToMock)

// what the BFF answers for the signed-in reader: nothing liked yet, and a generic ok for writes
function answerStudentCalls(reactions = { articleLiked: false, likedCommentIds: [] as number[] }) {
  studentFetch.mockReset().mockImplementation(async (path: string, opts?: { method?: string }) => {
    if (path.endsWith('/reactions')) return reactions
    if (path.endsWith('/like')) return { liked: opts?.method === 'PUT', likeCount: opts?.method === 'PUT' ? 6 : 5 }
    return { id: 9 }
  })
}

beforeEach(() => {
  publicGet.mockReset().mockImplementation(async (path: string) => (path.endsWith('/related') ? related : detail))
  answerStudentCalls()
  navigateToMock.mockReset()
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

describe('news article engagement', () => {
  const likeCountButtons = (w: VueWrapper) => w.findAll('button').filter(b => /^\d+ likes?$/.test(b.text()))
  const likeButtons = (w: VueWrapper) =>
    w.findAll('button[aria-pressed]').filter(b => /Like this (article|comment)|Remove your like/.test(b.attributes('aria-label') ?? ''))

  it('shows how many readers liked the article and each comment', async () => {
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    const articleButtons = likeButtons(w).filter(b => b.attributes('aria-label') === 'Like this article')
    expect(articleButtons).toHaveLength(2) // under the byline and again after the body, both in step
    expect(likeCountButtons(w).map(b => b.text())).toEqual(['5 likes', '5 likes'])
    const commentLike = likeButtons(w).find(b => b.attributes('aria-label') === 'Like this comment')!
    expect(commentLike.text()).toBe('3')
  })

  it('lets a signed-in reader like the article, showing the server total', async () => {
    useSession().status.value = 'authed'
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    await likeButtons(w)[0]!.trigger('click')
    await flushPromises()

    expect(studentFetch).toHaveBeenCalledWith('news/visa-tips/like', { method: 'PUT' })
    expect(likeButtons(w)[0]!.attributes('aria-pressed')).toBe('true')
    expect(likeCountButtons(w).map(b => b.text())).toEqual(['6 likes', '6 likes'])
  })

  it('lets a signed-in reader like a comment', async () => {
    useSession().status.value = 'authed'
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    const commentLike = likeButtons(w).find(b => b.attributes('aria-label') === 'Like this comment')!
    await commentLike.trigger('click')
    await flushPromises()

    expect(studentFetch).toHaveBeenCalledWith('news/visa-tips/comments/1/like', { method: 'PUT' })
  })

  it('restores what the reader already liked when the page opens', async () => {
    answerStudentCalls({ articleLiked: true, likedCommentIds: [1] })
    useSession().status.value = 'authed'
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    expect(studentFetch).toHaveBeenCalledWith('news/visa-tips/reactions')
    expect(likeButtons(w)[0]!.attributes('aria-pressed')).toBe('true')
    const commentLike = likeButtons(w).find(b => b.attributes('aria-label') === 'Remove your like from this comment')
    expect(commentLike?.attributes('aria-pressed')).toBe('true')
  })

  it('sends a guest to sign in, and back to this article, instead of liking', async () => {
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    await likeButtons(w)[0]!.trigger('click')

    expect(studentFetch).not.toHaveBeenCalled()
    expect(navigateToMock).toHaveBeenCalledWith({ path: '/login', query: { redirect: '/news/visa-tips' } })
  })

  it('puts the like back and says so when saving fails', async () => {
    useSession().status.value = 'authed'
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()
    studentFetch.mockRejectedValueOnce(new Error('offline'))

    await likeButtons(w)[0]!.trigger('click')
    await flushPromises()

    expect(likeButtons(w)[0]!.attributes('aria-pressed')).toBe('false')
    expect(likeCountButtons(w).map(b => b.text())).toEqual(['5 likes', '5 likes'])
    expect(w.text()).toContain('We could not save your like.')
  })

  it('offers copy-link and the social networks from the share menu', async () => {
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    await w.find('button[aria-haspopup="menu"]').trigger('click')

    const links = w.findAll('[role="menu"] a').map(a => a.attributes('href')!)
    expect(links).toHaveLength(4)
    expect(links.every(href => decodeURIComponent(href).includes('/news/visa-tips'))).toBe(true)
    expect(w.text()).toContain('Copy link')
    expect(w.text()).toContain('Share on WhatsApp')
  })
})

describe('news article people and recommendations', () => {
  it('shows the author photo, and initials when an author has none', async () => {
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    expect(w.find('header img').attributes('src')).toBe('https://cdn.example/amina.jpg')
  })

  it('shows a commenter by name with their photo when they have one', async () => {
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    const staff = w.findAll('li').find(li => li.text().includes('Staff reply'))!
    expect(staff.find('img').attributes('src')).toBe('https://cdn.example/giulia.jpg')
    expect(staff.text()).toContain('Giulia')
  })

  it('labels a comment whose author no longer exists instead of leaving the name blank', async () => {
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    const orphan = w.findAll('li').find(li => li.text().includes('From a departed account'))!
    expect(orphan.text()).toContain('Former member')
  })

  it('recommends related articles beside the article, linking to each', async () => {
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    const aside = w.find('aside')
    expect(aside.text()).toContain('More to read')
    expect(aside.text()).toContain('Student housing in Chengdu')
    expect(aside.text()).toContain('7 likes')
    expect(aside.findAll('a').map(a => a.attributes('href'))).toEqual(['/news/student-housing', '/news/visa-checklist'])
    expect(publicGet).toHaveBeenCalledWith('news/visa-tips/related', { limit: 4 })
  })

  it('shows no recommendations column when there is nothing to recommend, or the lookup fails', async () => {
    publicGet.mockImplementation(async (path: string) => {
      if (path.endsWith('/related')) throw new Error('boom')
      return detail
    })
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    expect(w.find('aside').exists()).toBe(false)
    expect(w.text()).toContain('Visa tips for China') // the article itself is unaffected
  })

  it('opens the liked-by list for a signed-in reader, with first names', async () => {
    studentFetch.mockImplementation(async (path: string) => {
      if (path.endsWith('/reactions')) return { articleLiked: false, likedCommentIds: [] }
      if (path.endsWith('/likes')) return { total: 5, likers: [{ displayName: 'Amina', avatarUrl: null }, { displayName: 'Luc', avatarUrl: null }] }
      return { id: 9 }
    })
    useSession().status.value = 'authed'
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    await w.findAll('button').find(b => b.text() === '5 likes')!.trigger('click')
    await flushPromises()

    expect(studentFetch).toHaveBeenCalledWith('news/visa-tips/likes', { query: { limit: 30 } })
    const dialog = document.body.querySelector('[role="dialog"]')!
    expect(dialog.textContent).toContain('Liked by')
    expect(dialog.textContent).toContain('Amina')
    expect(dialog.textContent).toContain('Luc')
    expect(dialog.textContent).toContain('and 3 more')
  })

  it('asks a guest to sign in instead of listing who liked', async () => {
    const w = await mountSuspended(NewsDetail, { route: '/news/visa-tips' })
    await flushPromises()

    await w.findAll('button').find(b => b.text() === '5 likes')!.trigger('click')
    await flushPromises()

    const dialog = document.body.querySelector('[role="dialog"]')!
    expect(dialog.textContent).toContain('Sign in to see who liked this article.')
    expect(studentFetch).not.toHaveBeenCalledWith('news/visa-tips/likes', expect.anything())
  })
})
