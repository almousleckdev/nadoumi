/** Public news DTOs: mirror `ArticleSummary`, `PublicArticleDetail`, `CommentNode` in nadoumi-content. */
export interface ArticleSummary {
  slug: string
  title: string
  subtitle?: string | null
  coverUrl?: string | null
  language: string
  authorName?: string | null
  publishedAt?: string | null
  commentCount: number
  readMinutes: number
}

export interface CommentNode {
  id: number
  parentId?: number | null
  /** Null once a moderator has deleted the comment. */
  authorName?: string | null
  body?: string | null
  deleted: boolean
  createTime: string
  replies: CommentNode[]
}

export interface PublicArticleDetail {
  article: ArticleSummary
  bodyMd: string
  comments: CommentNode[]
}
