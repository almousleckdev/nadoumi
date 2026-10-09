<script setup lang="ts">
import type { CommentNode } from '~/types/news'

defineProps<{
  slug: string
  comments: CommentNode[]
  total: number
  reactions: ReturnType<typeof useNewsReactions>
}>()
const emit = defineEmits<{ posted: [] }>()

const { t } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { status } = useSession()
const signedIn = computed(() => status.value === 'authed')
</script>

<template>
  <section aria-labelledby="comments-heading" class="border-t border-slate-200 pt-10">
    <h2 id="comments-heading" class="font-display text-2xl font-bold text-slate-900">
      {{ t('news.comments') }} <span class="text-slate-400">({{ total }})</span>
    </h2>

    <div class="mt-6">
      <CommentForm
        v-if="signedIn"
        :slug="slug"
        :placeholder="t('news.placeholder')"
        @posted="emit('posted')"
      />
      <p v-else class="rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700">
        {{ t('news.signInPrompt') }}
        <NuxtLink
          :to="{ path: localePath('/login'), query: { redirect: route.fullPath } }"
          class="ms-1 font-semibold text-brand-700"
        >{{ t('news.signIn') }}</NuxtLink>
      </p>
    </div>

    <p v-if="!comments.length" class="mt-8 text-sm text-slate-500">{{ t('news.noComments') }}</p>
    <ul v-else class="mt-8 space-y-6">
      <NewsComment
        v-for="c in comments"
        :key="c.id"
        :comment="c"
        :slug="slug"
        :signed-in="signedIn"
        :reactions="reactions"
        @posted="emit('posted')"
      />
    </ul>
  </section>
</template>
