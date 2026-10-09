<template>
  <button
    v-if="showImage"
    type="button"
    class="att att--image"
    :class="{ 'att--compact': compact }"
    :aria-label="t('conversations.openImage', { name })"
    data-test="attachment-image"
    @click="emit('preview', viewUrl)"
  >
    <img
      :src="viewUrl"
      :alt="name"
      loading="lazy"
      decoding="async"
      @error="imageFailed = true"
    >
  </button>
  <component
    :is="uploading ? 'div' : 'a'"
    v-else
    :href="uploading ? undefined : downloadUrl"
    :download="uploading ? undefined : name"
    class="att att--file"
    :class="{ 'att--mine': mine, 'att--busy': uploading }"
    data-test="attachment-file"
  >
    <span
      class="att__kind"
      aria-hidden="true"
    >{{ fileKind(attachment.filename, attachment.contentType) }}</span>
    <span class="att__text">
      <span class="att__name">{{ name }}</span>
      <span class="att__meta">{{ uploading ? t('conversations.sending') : formatBytes(attachment.byteSize, locale) }}</span>
    </span>
    <el-icon
      v-if="!uploading"
      :size="18"
      class="att__dl"
    >
      <Download />
    </el-icon>
    <span
      v-if="!uploading"
      class="sr-only"
    >{{ t('conversations.download') }}</span>
  </component>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Download } from '@element-plus/icons-vue'
import { attachmentUrl, type ChatAttachment } from '@/api/conversation'
import { fileKind, formatBytes } from '@/utils/chat'

/**
 * One attachment on a bubble: an image preview that opens in the lightbox, or a file card with a download link.
 * Bytes are never inlined; both go through the endpoint that re-checks the caller on every request.
 */
const props = defineProps<{ conversationId: number, attachment: ChatAttachment, mine?: boolean, compact?: boolean }>()
const emit = defineEmits<{ preview: [url: string] }>()
const { t, locale } = useI18n()

const uploading = computed(() => props.attachment.id <= 0)
const viewUrl = computed(() => attachmentUrl(props.conversationId, props.attachment.id))
const downloadUrl = computed(() => attachmentUrl(props.conversationId, props.attachment.id, true))
const imageFailed = ref(false)
const showImage = computed(() => props.attachment.image && !uploading.value && !imageFailed.value)
const name = computed(() => props.attachment.filename ?? t('conversations.attachment'))
</script>

<style scoped>
.att { text-decoration: none; color: inherit; }
.att--image {
  display: block;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--nad-line);
  border-radius: 10px;
  background: #fff;
  cursor: zoom-in;
}
.att--image img { display: block; max-width: 280px; max-height: 240px; width: 100%; object-fit: cover; }
.att--compact img { width: 100%; height: 128px; max-width: none; }
.att--image:focus-visible { outline: 2px solid var(--nad-brand-500); outline-offset: 2px; }
.att--file {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  max-width: 300px;
  padding: 8px 12px;
  border: 1px solid var(--nad-line);
  border-radius: 10px;
  background: #f8fafc;
}
.att--file:hover { background: #fff; }
.att--mine { background: rgba(255, 255, 255, 0.7); border-color: var(--nad-brand-200); }
.att--busy { opacity: 0.7; }
.att__kind {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--nad-brand-100);
  color: var(--nad-brand-700);
  font-size: 10px;
  font-weight: 700;
}
.att__text { display: grid; min-width: 0; flex: 1; }
.att__name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; font-weight: 500; color: var(--nad-ink); }
.att__meta { font-size: 12px; color: var(--nad-ink-soft); }
.att__dl { color: var(--nad-ink-soft); }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
</style>
