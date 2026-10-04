<script setup lang="ts">
import type { CommentNode } from '~/types/news'

const props = defineProps<{
  slug: string
  parentId?: number | null
  placeholder: string
  autofocus?: boolean
}>()
const emit = defineEmits<{ posted: [], cancel: [] }>()

const MAX_LENGTH = 2000

const { t } = useI18n()
const { studentFetch } = useApi()
const { busy, error, run } = useAsyncAction()

const uid = useId()
const body = ref('')
const canPost = computed(() => body.value.trim().length > 0 && !busy.value)

async function submit() {
  if (!canPost.value) return
  const saved = await run(() => studentFetch<CommentNode>(`news/${props.slug}/comments`, {
    method: 'POST',
    body: { parentId: props.parentId ?? null, body: body.value.trim() },
  }))
  if (saved) {
    body.value = ''
    emit('posted')
  }
}
</script>

<template>
  <form class="space-y-3" @submit.prevent="submit">
    <label :for="`c-${uid}`" class="sr-only">{{ placeholder }}</label>
    <NTextarea
      :id="`c-${uid}`"
      v-model="body"
      :rows="3"
      :maxlength="MAX_LENGTH"
      :placeholder="placeholder"
      :autofocus="autofocus"
    />
    <NAlert v-if="error" tone="danger">{{ t('news.postError') }}</NAlert>
    <div class="flex items-center justify-end gap-2">
      <NButton v-if="parentId" variant="ghost" size="sm" @click="emit('cancel')">{{ t('news.cancel') }}</NButton>
      <NButton type="submit" size="sm" :loading="busy" :disabled="!canPost">
        {{ busy ? t('news.posting') : t('news.post') }}
      </NButton>
    </div>
  </form>
</template>
