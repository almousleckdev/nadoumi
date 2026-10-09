<script setup lang="ts">
defineProps<{
  liked: boolean
  likes: number
  commentTotal: number
  url: string
  title: string
  failed: boolean
}>()
defineEmits<{ toggleLike: [], showLikers: [] }>()

const { t, n } = useI18n()
</script>

<template>
  <div>
    <div class="flex items-center justify-between border-y border-slate-200 py-2">
      <div class="flex items-center gap-1">
        <LikeButton
          :liked="liked"
          :count="likes"
          :show-count="false"
          :label="liked ? t('news.unlikeArticleLabel') : t('news.likeArticleLabel')"
          @toggle="$emit('toggleLike')"
        />
        <button
          v-if="likes > 0"
          type="button"
          :title="t('news.viewLikers')"
          class="rounded-full px-1.5 py-1.5 text-sm tabular-nums text-slate-500 hover:text-slate-900 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
          @click="$emit('showLikers')"
        >
          {{ t('news.likeCount', likes) }}
        </button>
        <a
          href="#comments-heading"
          :aria-label="t('news.goToComments')"
          class="inline-flex items-center gap-1.5 rounded-full px-2 py-1.5 text-slate-500 no-underline hover:text-slate-900 hover:no-underline"
        >
          <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z" />
          </svg>
          <span v-if="commentTotal > 0" class="tabular-nums">{{ n(commentTotal) }}</span>
        </a>
      </div>
      <ShareMenu :url="url" :title="title" />
    </div>
    <p v-if="failed" role="alert" class="mt-2 text-sm text-red-600">{{ t('news.likeFailed') }}</p>
  </div>
</template>
