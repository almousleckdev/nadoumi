import type { LikeState, MyReactions } from '~/types/news'

interface Initial {
  /** The article's like total as served with the page. */
  likeCount: number
  /** Like totals of the comments in the served thread, by comment id. */
  commentLikes: Record<number, number>
}

/**
 * A reader's likes on one article and its comments. Taps feel instant (the change is shown first,
 * then replaced by the server's total), a failed request puts everything back, and a second tap
 * on the same item is ignored while its request is still running.
 */
export function useNewsReactions(slug: string, initial: Initial) {
  const { studentFetch } = useApi()

  const articleLiked = ref(false)
  const articleLikes = ref(initial.likeCount)
  const likedComments = ref<ReadonlySet<number>>(new Set())
  const commentCounts = ref<Record<number, number>>({ ...initial.commentLikes })
  const failed = ref(false)
  const inFlight = new Set<string>()

  /** What this reader already liked. A guest has nothing to load, which is not an error. */
  async function load(): Promise<void> {
    try {
      const mine = await studentFetch<MyReactions>(`news/${slug}/reactions`)
      articleLiked.value = mine.articleLiked === true
      likedComments.value = new Set(mine.likedCommentIds ?? [])
    }
    catch {
      // signed out or offline: the page simply shows nothing as liked
    }
  }

  async function toggleArticle(): Promise<void> {
    if (!claim('article')) return
    const wasLiked = articleLiked.value
    const before = articleLikes.value
    articleLiked.value = !wasLiked
    articleLikes.value = Math.max(0, before + (wasLiked ? -1 : 1))
    try {
      const state = await studentFetch<LikeState>(`news/${slug}/like`, { method: wasLiked ? 'DELETE' : 'PUT' })
      articleLiked.value = state.liked
      articleLikes.value = state.likeCount
    }
    catch {
      articleLiked.value = wasLiked
      articleLikes.value = before
      failed.value = true
    }
    finally {
      inFlight.delete('article')
    }
  }

  async function toggleComment(id: number): Promise<void> {
    const key = `comment-${id}`
    if (!claim(key)) return
    const wasLiked = likedComments.value.has(id)
    const before = commentLikes(id)
    setCommentLiked(id, !wasLiked)
    commentCounts.value = { ...commentCounts.value, [id]: Math.max(0, before + (wasLiked ? -1 : 1)) }
    try {
      const state = await studentFetch<LikeState>(`news/${slug}/comments/${id}/like`, {
        method: wasLiked ? 'DELETE' : 'PUT',
      })
      setCommentLiked(id, state.liked)
      commentCounts.value = { ...commentCounts.value, [id]: state.likeCount }
    }
    catch {
      setCommentLiked(id, wasLiked)
      commentCounts.value = { ...commentCounts.value, [id]: before }
      failed.value = true
    }
    finally {
      inFlight.delete(key)
    }
  }

  /** Starts a request for `key`, or refuses when one is already running for it. */
  function claim(key: string): boolean {
    if (inFlight.has(key)) return false
    inFlight.add(key)
    failed.value = false
    return true
  }

  function setCommentLiked(id: number, liked: boolean) {
    const next = new Set(likedComments.value)
    if (liked) next.add(id)
    else next.delete(id)
    likedComments.value = next
  }

  const isCommentLiked = (id: number) => likedComments.value.has(id)
  const commentLikes = (id: number) => commentCounts.value[id] ?? 0

  return { articleLiked, articleLikes, failed, load, toggleArticle, toggleComment, isCommentLiked, commentLikes }
}
