<script setup lang="ts">
import type { MessageAttachment } from '~/types/messages'
import { isImageAttachment } from '~/constants/messages'

/** One already-sent attachment: fetches its own short-lived signed URL and renders
 *  an inline thumbnail for a photo, or a small file card with a view link otherwise. */
const props = defineProps<{ conversationId: number, attachment: MessageAttachment }>()
const { t } = useI18n()
const { attachmentAccess } = useMessages()

const url = ref('')
const failed = ref(false)
const pending = ref(true)

onMounted(async () => {
  try {
    const access = await attachmentAccess(props.conversationId, props.attachment.id)
    url.value = access.url
  }
  catch {
    failed.value = true
  }
  finally {
    pending.value = false
  }
})

const isImage = computed(() => isImageAttachment(props.attachment.contentType))
</script>

<template>
  <NSkeleton v-if="pending" class="h-24 w-24 rounded-lg" />
  <p v-else-if="failed" class="text-xs italic opacity-70">{{ t('dashboard.messages.attachmentUnavailable') }}</p>
  <a
    v-else-if="isImage"
    :href="url" target="_blank" rel="noopener"
    class="block overflow-hidden rounded-lg border border-white/20"
  >
    <img :src="url" :alt="attachment.filename ?? t('dashboard.messages.attachment')" class="max-h-48 max-w-full object-contain">
  </a>
  <a
    v-else
    :href="url" target="_blank" rel="noopener"
    class="flex items-center gap-2 rounded-lg border border-white/20 bg-black/5 px-3 py-2 text-xs no-underline"
  >
    <span class="truncate">{{ attachment.filename ?? t('dashboard.messages.attachment') }}</span>
    <span class="shrink-0 underline">{{ t('dashboard.messages.viewAttachment') }}</span>
  </a>
</template>
