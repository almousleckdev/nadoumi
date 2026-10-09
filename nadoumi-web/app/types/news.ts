/** Public news DTOs: mirror `ArticleSummary`, `PublicArticleDetail`, `CommentNode` in nadoumi-content. */
export interface ArticleSummary {
  slug: string
  title: string
  subtitle?: string | null
  coverUrl?: string | null
  language: string
  authorName?: string | null
  /** The author's photo; absent when the author has none. */
  authorAvatarUrl?: string | null
  publishedAt?: string | null
  commentCount: number
  likeCount: number
  readMinutes: number
}

export interface CommentNode {
  id: number
  parentId?: number | null
  /** Null once a moderator has deleted the comment. */
  authorName?: string | null
  authorAvatarUrl?: string | null
  body?: string | null
  deleted: boolean
  likeCount: number
  createTime: string
  replies: CommentNode[]
}

export interface PublicArticleDetail {
  article: ArticleSummary
  bodyMd: string
  comments: CommentNode[]
}

/** Result of liking or unliking: the reader's state and the item's new total. */
export interface LikeState {
  liked: boolean
  likeCount: number
}

/** What the signed-in reader already liked on one article. */
export interface MyReactions {
  articleLiked: boolean
  likedCommentIds: number[]
}

/** A reader who liked an article, as shown publicly: a first name and, for staff only, a photo. */
export interface Liker {
  displayName: string
  avatarUrl?: string | null
}

export interface LikersPage {
  total: number
  likers: Liker[]
}
