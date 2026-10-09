import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useNewsReactions } from '~/composables/useNewsReactions'

const studentFetch = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ studentFetch, publicGet: vi.fn() }),
}))

const SLUG = 'studying-in-chengdu'

function setup(initial = { likeCount: 4, commentLikes: { 11: 2, 12: 0 } as Record<number, number> }) {
  return useNewsReactions(SLUG, initial)
}

beforeEach(() => studentFetch.mockReset())

describe('useNewsReactions: loading what the reader already liked', () => {
  it('marks the article and comments the reader liked', async () => {
    studentFetch.mockResolvedValueOnce({ articleLiked: true, likedCommentIds: [11] })
    const r = setup()

    await r.load()

    expect(studentFetch).toHaveBeenCalledWith(`news/${SLUG}/reactions`)
    expect(r.articleLiked.value).toBe(true)
    expect(r.isCommentLiked(11)).toBe(true)
    expect(r.isCommentLiked(12)).toBe(false)
  })

  it('stays quiet for a reader who is not signed in', async () => {
    studentFetch.mockRejectedValueOnce({ statusCode: 401 })
    const r = setup()

    await expect(r.load()).resolves.toBeUndefined()
    expect(r.articleLiked.value).toBe(false)
  })
})

describe('useNewsReactions: liking the article', () => {
  it('shows the like at once and settles on the server total', async () => {
    let resolve!: (v: unknown) => void
    studentFetch.mockReturnValueOnce(new Promise(r => { resolve = r }))
    const r = setup()

    const pending = r.toggleArticle()
    expect(r.articleLiked.value).toBe(true) // optimistic
    expect(r.articleLikes.value).toBe(5)

    resolve({ liked: true, likeCount: 9 }) // other readers liked meanwhile
    await pending
    expect(r.articleLikes.value).toBe(9)
    expect(studentFetch).toHaveBeenCalledWith(`news/${SLUG}/like`, { method: 'PUT' })
  })

  it('takes the like back with DELETE', async () => {
    studentFetch.mockResolvedValueOnce({ articleLiked: true, likedCommentIds: [] })
    const r = setup()
    await r.load()
    studentFetch.mockResolvedValueOnce({ liked: false, likeCount: 3 })

    await r.toggleArticle()

    expect(studentFetch).toHaveBeenLastCalledWith(`news/${SLUG}/like`, { method: 'DELETE' })
    expect(r.articleLiked.value).toBe(false)
    expect(r.articleLikes.value).toBe(3)
  })

  it('puts everything back when the request fails', async () => {
    studentFetch.mockRejectedValueOnce(new Error('offline'))
    const r = setup()

    await r.toggleArticle()

    expect(r.articleLiked.value).toBe(false)
    expect(r.articleLikes.value).toBe(4)
    expect(r.failed.value).toBe(true)
  })

  it('ignores a second click while the first request is still running', async () => {
    let resolve!: (v: unknown) => void
    studentFetch.mockReturnValueOnce(new Promise(r => { resolve = r }))
    const r = setup()

    const first = r.toggleArticle()
    await r.toggleArticle()
    resolve({ liked: true, likeCount: 5 })
    await first

    expect(studentFetch).toHaveBeenCalledTimes(1)
    expect(r.articleLiked.value).toBe(true)
  })
})

describe('useNewsReactions: liking a comment', () => {
  it('likes one comment without touching the others', async () => {
    studentFetch.mockResolvedValueOnce({ liked: true, likeCount: 3 })
    const r = setup()

    await r.toggleComment(11)

    expect(studentFetch).toHaveBeenCalledWith(`news/${SLUG}/comments/11/like`, { method: 'PUT' })
    expect(r.isCommentLiked(11)).toBe(true)
    expect(r.commentLikes(11)).toBe(3)
    expect(r.commentLikes(12)).toBe(0)
    expect(r.isCommentLiked(12)).toBe(false)
  })

  it('rolls a comment like back on failure', async () => {
    studentFetch.mockRejectedValueOnce(new Error('offline'))
    const r = setup()

    await r.toggleComment(11)

    expect(r.isCommentLiked(11)).toBe(false)
    expect(r.commentLikes(11)).toBe(2)
  })

  it('counts a comment the thread did not list as zero', () => {
    expect(setup().commentLikes(999)).toBe(0)
  })
})
