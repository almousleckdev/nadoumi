<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ChatAttachment } from '~/types/chat'
import { attachmentUrl } from '~/composables/useChatApi'
import { fileKind, formatBytes } from '~/utils/chat'

/**
 * One attachment on a bubble: an image preview that opens in the lightbox, or a file card with a download link.
 * Bytes are never inlined; both go through the same-origin redirect that re-checks the caller on every request.
 */
const props = defineProps<{ conversationId: number, attachment: ChatAttachment, mine?: boolean, compact?: boolean }>()
const emit = defineEmits<{ preview: [url: string] }>()
const { t, locale } = useI18n()

const uploading = computed(() => props.attachment.id <= 0)
const viewUrl = computed(() => attachmentUrl(props.conversationId, props.attachment.id))
const downloadUrl = computed(() => attachmentUrl(props.conversationId, props.attachment.id, true))
const imageFailed = ref(false)
const showImage = computed(() => props.attachment.image && !uploading.value && !imageFailed.value)
const name = computed(() => props.attachment.filename ?? t('dashboard.messages.attachment'))
</script>

<template>
  <button
    v-if="showImage"
    type="button"
    class="block overflow-hidden rounded-lg border border-slate-200 bg-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-500"
    :aria-label="t('dashboard.messages.openImage', { name })"
    data-test="attachment-image"
    @click="emit('preview', viewUrl)"
  >
    <img
      :src="viewUrl"
      :alt="name"
      loading="lazy"
      decoding="async"
      class="w-full object-cover"
      :class="compact ? 'h-32' : 'max-h-64 max-w-xs'"
      @error="imageFailed = true"
    >
  </button>

  <component
    :is="uploading ? 'div' : 'a'"
    v-else
    :href="uploading ? undefined : downloadUrl"
    :download="uploading ? undefined : name"
    class="flex w-full max-w-xs items-center gap-3 rounded-lg border px-3 py-2 text-start"
    :class="[mine ? 'border-brand-200 bg-white/70' : 'border-slate-200 bg-slate-50', uploading ? 'opacity-70' : 'hover:bg-white']"
    data-test="attachment-file"
  >
    <span class="grid h-10 w-10 shrink-0 place-items-center rounded-md bg-brand-100 text-[10px] font-bold text-brand-800" aria-hidden="true">
      {{ fileKind(attachment.filename, attachment.contentType) }}
    </span>
    <span class="min-w-0 flex-1">
      <span class="block truncate text-sm font-medium text-slate-900">{{ name }}</span>
      <span class="block text-xs text-slate-500">
        {{ uploading ? t('dashboard.messages.sending') : formatBytes(attachment.byteSize, locale) }}
      </span>
    </span>
    <svg v-if="!uploading" viewBox="0 0 24 24" class="h-5 w-5 shrink-0 text-slate-500" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
      <path d="M12 4v12m0 0-4-4m4 4 4-4M5 20h14" stroke-linecap="round" stroke-linejoin="round" />
    </svg>
    <span v-if="!uploading" class="sr-only">{{ t('dashboard.messages.download') }}</span>
  </component>
</template>
