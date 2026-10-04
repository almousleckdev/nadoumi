<script setup lang="ts">
import type { CommentNode } from '~/types/news'

const props = withDefaults(defineProps<{
  comment: CommentNode
  slug: string
  signedIn: boolean
  depth?: number
}>(), { depth: 0 })
const emit = defineEmits<{ posted: [] }>()

// Past this depth replies stop indenting so deep threads stay readable on phones.
const MAX_INDENT_DEPTH = 4

const { t, locale } = useI18n()

const replying = ref(false)
const collapsed = ref(false)

const when = computed(() => formatTimestamp(props.comment.createTime, locale.value))
const indent = computed(() => props.depth < MAX_INDENT_DEPTH)

function onPosted() {
  replying.value = false
  emit('posted')
}
</script>

<template>
  <li class="list-none">
    <div class="flex gap-3">
      <NAvatar v-if="!comment.deleted" :name="comment.authorName ?? ''" size="sm" class="shrink-0" />
      <span v-else class="h-8 w-8 shrink-0 rounded-full bg-slate-100" aria-hidden="true" />

      <div class="min-w-0 flex-1">
        <p v-if="comment.deleted" class="text-sm italic text-slate-500">{{ t('news.deleted') }}</p>
        <template v-else>
          <p class="text-sm">
            <span class="font-semibold text-slate-900">{{ comment.authorName }}</span>
            <span class="ms-2 text-slate-500">{{ when }}</span>
          </p>
          <p class="mt-1 whitespace-pre-line break-words text-base leading-relaxed text-slate-800">{{ comment.body }}</p>
          <div class="mt-2 flex items-center gap-4 text-sm">
            <button
              v-if="signedIn"
              type="button"
              class="font-medium text-slate-600 hover:text-slate-900"
              @click="replying = !replying"
            >
              {{ t('news.reply') }}
            </button>
            <button
              v-if="comment.replies.length"
              type="button"
              class="text-slate-500 hover:text-slate-900"
              :aria-expanded="collapsed ? 'false' : 'true'"
              @click="collapsed = !collapsed"
            >
              {{ collapsed ? t('news.showReplies') : t('news.hideReplies') }} ({{ comment.replies.length }})
            </button>
          </div>
        </template>

        <div v-if="replying" class="mt-3">
          <p class="mb-2 text-xs text-slate-500">{{ t('news.replyingTo', { name: comment.authorName ?? '' }) }}</p>
          <CommentForm
            :slug="slug"
            :parent-id="comment.id"
            :placeholder="t('news.replyPlaceholder')"
            autofocus
            @posted="onPosted"
            @cancel="replying = false"
          />
        </div>

        <ul
          v-if="comment.replies.length && !collapsed"
          class="mt-4 space-y-5"
          :class="indent ? 'border-s border-slate-200 ps-4 sm:ps-6' : ''"
        >
          <NewsComment
            v-for="r in comment.replies"
            :key="r.id"
            :comment="r"
            :slug="slug"
            :signed-in="signedIn"
            :depth="depth + 1"
            @posted="emit('posted')"
          />
        </ul>
      </div>
    </div>
  </li>
</template>
