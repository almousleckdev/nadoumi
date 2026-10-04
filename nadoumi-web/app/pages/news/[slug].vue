<script setup lang="ts">
import type { CommentNode, PublicArticleDetail } from '~/types/news'

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
</script>

<template>
  <div>
    <NContainer>
      <article v-if="data && article" class="mx-auto max-w-3xl py-10 sm:py-14">
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
            <NAvatar v-if="article.authorName" :name="article.authorName" size="md" />
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

        <figure v-if="article.coverUrl" class="mt-8">
          <img :src="article.coverUrl" :alt="article.title" class="w-full rounded-md object-cover">
        </figure>

        <ArticleBody class="mt-10" :markdown="data.bodyMd" />

        <div class="mt-14">
          <NewsComments :slug="slug" :comments="data.comments" :total="totalComments" @posted="refresh()" />
        </div>
      </article>

      <div v-else class="mx-auto max-w-3xl py-24 text-center">
        <h1 class="font-display text-2xl font-bold text-slate-900">{{ t('news.notFoundTitle') }}</h1>
        <p class="mt-2 text-slate-600">{{ t('news.notFoundBody') }}</p>
        <NButton :to="localePath('/news')" class="mt-6">{{ t('news.back') }}</NButton>
      </div>
    </NContainer>
  </div>
</template>
