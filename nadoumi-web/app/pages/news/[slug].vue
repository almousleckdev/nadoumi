<script setup lang="ts">
import type { ArticleSummary, CommentNode, PublicArticleDetail } from '~/types/news'

const RELATED_LIMIT = 4

const { t, locale, n } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { publicGet } = useApi()
const slug = computed(() => String(route.params.slug))

const { data, refresh } = await useAsyncData(
  () => `news-${slug.value}`,
  () => publicGet<PublicArticleDetail>(`news/${slug.value}`).catch(() => null),
  { watch: [slug] },
)

const article = computed(() => data.value?.article)

// "More to read" must never get in the way of the article: a failed lookup just shows nothing.
const { data: related } = await useAsyncData(
  () => `news-related-${slug.value}`,
  () => (article.value
    ? publicGet<ArticleSummary[]>(`news/${slug.value}/related`, { limit: RELATED_LIMIT }).catch(() => [])
    : Promise.resolve([])),
  { watch: [slug], default: () => [] as ArticleSummary[] },
)

useSeo(
  article.value?.title ?? t('news.notFoundTitle'),
  article.value?.subtitle ?? t('news.subtitle'),
)
useHead(() => ({
  meta: article.value?.coverUrl ? [{ property: 'og:image', content: article.value.coverUrl }] : [],
}))

const date = computed(() => formatTimestamp(article.value?.publishedAt, locale.value, { dateStyle: 'long' }))

function countVisible(nodes: CommentNode[]): number {
  return nodes.reduce((sum, c) => sum + (c.deleted ? 0 : 1) + countVisible(c.replies), 0)
}
const totalComments = computed(() => countVisible(data.value?.comments ?? []))

// ---- reader engagement: likes (article + comments) and sharing
const { status } = useSession()
const signedIn = computed(() => status.value === 'authed')
const reactions = useNewsReactions(slug.value, {
  likeCount: article.value?.likeCount ?? 0,
  commentLikes: commentLikeCounts(data.value?.comments ?? []),
})
const requestURL = useRequestURL()
const shareUrl = computed(() => `${requestURL.origin}${route.path}`)
const likersOpen = ref(false)

// The same bar sits under the byline and after the body, so both read from one set of props.
const actions = computed(() => ({
  liked: reactions.articleLiked.value,
  likes: reactions.articleLikes.value,
  commentTotal: totalComments.value,
  url: shareUrl.value,
  title: article.value?.title ?? '',
  failed: reactions.failed.value,
}))

// A guest who taps the heart is sent to sign in and brought back to this very article.
function onLikeArticle() {
  if (signedIn.value) return void reactions.toggleArticle()
  return void navigateTo({ path: localePath('/login'), query: { redirect: route.fullPath } })
}

// What this reader already liked is per-person, so it is loaded in the browser, never baked into the cached page.
onMounted(() => {
  if (signedIn.value) void reactions.load()
})
watch(signedIn, (value: boolean) => {
  if (value && import.meta.client) void reactions.load()
})
</script>

<template>
  <!-- full-width figures bleed to the viewport edge on small screens; never let them add a horizontal scrollbar -->
  <div class="overflow-x-clip">
    <NContainer>
      <div
        v-if="data && article"
        class="py-10 sm:py-14 lg:mx-auto lg:grid lg:max-w-6xl lg:grid-cols-[minmax(0,48rem)_minmax(0,18rem)] lg:justify-center lg:gap-12"
      >
        <article class="mx-auto w-full max-w-3xl lg:max-w-none">
          <NuxtLink :to="localePath('/news')" class="text-sm font-medium text-slate-600 no-underline hover:text-slate-900">
            ← {{ t('news.back') }}
          </NuxtLink>

          <header class="mt-6">
            <h1 class="font-display text-3xl font-bold leading-tight tracking-tight text-slate-900 sm:text-5xl">
              {{ article.title }}
            </h1>
            <p v-if="article.subtitle" class="mt-4 font-serif text-xl leading-snug text-slate-600 sm:text-2xl">
              {{ article.subtitle }}
            </p>
            <div class="mt-6 flex items-center gap-3">
              <NAvatar v-if="article.authorName" :name="article.authorName" :src="article.authorAvatarUrl ?? undefined" size="md" />
              <div class="text-sm">
                <p v-if="article.authorName" class="font-medium text-slate-900">{{ t('news.by', { name: article.authorName }) }}</p>
                <p class="text-slate-500">
                  <time v-if="date" :datetime="article.publishedAt ?? undefined">{{ date }}</time>
                  <span v-if="date" class="mx-1.5" aria-hidden="true">·</span>
                  {{ t('news.readTime', { n: n(article.readMinutes) }) }}
                </p>
              </div>
            </div>
          </header>

          <div class="mt-6">
            <ArticleActions v-bind="actions" @toggle-like="onLikeArticle" @show-likers="likersOpen = true" />
          </div>

          <figure v-if="article.coverUrl" class="mt-8">
            <img :src="article.coverUrl" :alt="article.title" class="w-full rounded-md object-cover">
          </figure>

          <ArticleBody class="mt-10" :markdown="data.bodyMd" />

          <div class="mt-12">
            <ArticleActions v-bind="actions" @toggle-like="onLikeArticle" @show-likers="likersOpen = true" />
          </div>

          <div class="mt-14">
            <NewsComments :slug="slug" :comments="data.comments" :total="totalComments" :reactions="reactions" @posted="refresh()" />
          </div>
        </article>

        <aside v-if="related.length" class="mt-14 border-t border-slate-200 pt-10 lg:mt-0 lg:border-t-0 lg:pt-0">
          <div class="lg:sticky lg:top-24">
            <RelatedArticles :articles="related" />
          </div>
        </aside>
      </div>

      <div v-else class="mx-auto max-w-3xl py-24 text-center">
        <h1 class="font-display text-2xl font-bold text-slate-900">{{ t('news.notFoundTitle') }}</h1>
        <p class="mt-2 text-slate-600">{{ t('news.notFoundBody') }}</p>
        <NButton :to="localePath('/news')" class="mt-6">{{ t('news.back') }}</NButton>
      </div>
    </NContainer>

    <LikersModal v-if="article" v-model="likersOpen" :slug="slug" :signed-in="signedIn" />
  </div>
</template>
