import request from '@/utils/request'
import { uploadMedia, type MediaUploadResult } from './media'
import type { Page } from './applicant'

export type ArticleStatus = 'DRAFT' | 'PUBLISHED' | 'UNPUBLISHED'
export type ArticleLanguage = 'en' | 'fr' | 'zh' | 'ar' | 'es'
export const ARTICLE_STATUSES: ArticleStatus[] = ['DRAFT', 'PUBLISHED', 'UNPUBLISHED']
export const ARTICLE_LANGUAGES: ArticleLanguage[] = ['en', 'fr', 'zh', 'ar', 'es']

/** `id` is the article's opaque public UUID, the only identifier staff URLs and API paths use. */
export interface Article {
  id: string
  slug: string
  title: string
  subtitle: string | null
  bodyMd: string
  coverMediaId: number | null
  coverUrl: string | null
  language: ArticleLanguage
  status: ArticleStatus
  publishedAt: string | null
  authorName: string | null
  /** Absolute photo URL, or null when the author has none (or only a legacy local file). */
  authorAvatarUrl: string | null
  commentCount: number
  likeCount: number
  createTime: string | null
  updateTime: string | null
}

export interface ArticleInput {
  title: string
  subtitle: string | null
  bodyMd: string
  language: ArticleLanguage
}

export interface ArticleComment {
  id: number
  articleId: number
  parentId: number | null
  authorId: number
  authorName: string | null
  body: string
  status: 'VISIBLE' | 'DELETED'
  createTime: string
  deletedTime: string | null
}

const BASE = '/api/staff/news'

export const listArticles = (params: {
  q?: string
  language?: string
  status?: string
  page?: number
  size?: number
}) => request.get<unknown, Page<Article>>(BASE, { params })

export const getArticle = (id: number | string) =>
  request.get<unknown, Article>(`${BASE}/${id}`)

export const createArticle = (body: ArticleInput) =>
  request.post<unknown, Article>(BASE, body)

export const updateArticle = (id: number | string, body: ArticleInput) =>
  request.put<unknown, Article>(`${BASE}/${id}`, body)

export const publishArticle = (id: number | string) =>
  request.post<unknown, Article>(`${BASE}/${id}/publish`)

export const unpublishArticle = (id: number | string) =>
  request.post<unknown, Article>(`${BASE}/${id}/unpublish`)

export const deleteArticle = (id: number | string) =>
  request.delete(`${BASE}/${id}`)

export const uploadArticleCover = (id: number | string, file: File): Promise<MediaUploadResult> =>
  uploadMedia(`${BASE}/${id}/cover`, file)

export const uploadArticleImage = (id: number | string, file: File): Promise<MediaUploadResult> =>
  uploadMedia(`${BASE}/${id}/images`, file)

export const listArticleComments = (id: number | string) =>
  request.get<unknown, ArticleComment[]>(`${BASE}/${id}/comments`)

export const deleteArticleComment = (commentId: number | string) =>
  request.delete(`${BASE}/comments/${commentId}`)
