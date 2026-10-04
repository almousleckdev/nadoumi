<script setup lang="ts">
import type { ArticleSummary } from '~/types/news'

const props = defineProps<{ article: ArticleSummary }>()

const { t, locale, n } = useI18n()
const localePath = useLocalePath()

const date = computed(() => formatTimestamp(props.article.publishedAt, locale.value))
</script>

<template>
  <article class="border-b border-slate-200 py-8 first:pt-0">
    <NuxtLink :to="localePath(`/news/${article.slug}`)" class="group flex items-start gap-5 no-underline hover:no-underline sm:gap-8">
      <div class="min-w-0 flex-1">
        <p class="flex flex-wrap items-center gap-x-2 text-sm text-slate-600">
          <NAvatar v-if="article.authorName" :name="article.authorName" size="sm" />
          <span v-if="article.authorName" class="font-medium text-slate-900">{{ article.authorName }}</span>
          <span v-if="article.authorName && date" aria-hidden="true">·</span>
          <time v-if="date" :datetime="article.publishedAt ?? undefined">{{ date }}</time>
        </p>
        <h2 class="mt-3 font-display text-xl font-bold leading-snug tracking-tight text-slate-900 group-hover:text-brand-700 sm:text-2xl">
          {{ article.title }}
        </h2>
        <p v-if="article.subtitle" class="mt-1.5 line-clamp-2 font-serif text-base leading-relaxed text-slate-600 sm:text-lg">
          {{ article.subtitle }}
        </p>
        <p class="mt-4 flex items-center gap-3 text-sm text-slate-500">
          <span>{{ t('news.readTime', { n: n(article.readMinutes) }) }}</span>
          <span aria-hidden="true">·</span>
          <span>{{ t('news.count', article.commentCount) }}</span>
        </p>
      </div>
      <img
        v-if="article.coverUrl"
        :src="article.coverUrl"
        alt=""
        loading="lazy"
        decoding="async"
        class="h-20 w-28 shrink-0 rounded object-cover sm:h-28 sm:w-40"
      >
    </NuxtLink>
  </article>
</template>
