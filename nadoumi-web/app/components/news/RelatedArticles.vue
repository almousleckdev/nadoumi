<script setup lang="ts">
import type { ArticleSummary } from '~/types/news'

defineProps<{ articles: ArticleSummary[] }>()

const { t, n } = useI18n()
const localePath = useLocalePath()
</script>

<template>
  <section aria-labelledby="related-heading">
    <h2 id="related-heading" class="font-display text-lg font-bold text-slate-900">{{ t('news.moreToRead') }}</h2>
    <ul class="mt-4 divide-y divide-slate-200">
      <li v-for="a in articles" :key="a.slug" class="py-4 first:pt-0 last:pb-0">
        <NuxtLink :to="localePath(`/news/${a.slug}`)" class="group flex items-start gap-3 no-underline hover:no-underline">
          <div class="min-w-0 flex-1">
            <p v-if="a.authorName" class="flex items-center gap-2 text-xs text-slate-600">
              <NAvatar :name="a.authorName" :src="a.authorAvatarUrl ?? undefined" size="sm" />
              <span class="truncate font-medium text-slate-900">{{ a.authorName }}</span>
            </p>
            <h3 class="mt-2 line-clamp-3 font-display text-base font-bold leading-snug text-slate-900 group-hover:text-brand-700">
              {{ a.title }}
            </h3>
            <p class="mt-1.5 text-xs text-slate-500">
              {{ t('news.readTime', { n: n(a.readMinutes) }) }}
              <template v-if="a.likeCount > 0">
                <span aria-hidden="true"> · </span>{{ t('news.likeCount', a.likeCount) }}
              </template>
            </p>
          </div>
          <img
            v-if="a.coverUrl"
            :src="a.coverUrl"
            alt=""
            loading="lazy"
            decoding="async"
            class="h-16 w-16 shrink-0 rounded object-cover"
          >
        </NuxtLink>
      </li>
    </ul>
  </section>
</template>
