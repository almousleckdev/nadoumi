<script setup lang="ts">
import { computed, ref, toRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { Loading, Paperclip } from '@element-plus/icons-vue'
import { MESSAGE_MAX_LENGTH } from '@/api/conversation'
import { MAX_ATTACHMENTS, useMessageAttachments } from './useMessageAttachments'

const props = defineProps<{ conversationId: number | null, closed: boolean, sending: boolean }>()
const emit = defineEmits<{ send: [payload: { body: string, attachmentMediaIds: number[] }] }>()

const { t } = useI18n()

const draft = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
const attachments = useMessageAttachments(toRef(props, 'conversationId'))
const { pending, isUploading, readyIds } = attachments

const attachmentsFull = computed(() => pending.value.length >= MAX_ATTACHMENTS)
const canSend = computed(() => {
  if (props.sending || isUploading.value) return false
  return draft.value.trim().length > 0 || readyIds.value.length > 0
})

async function onFilePicked(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = ''
  await attachments.addFiles(files)
}

function submit() {
  if (!canSend.value) return
  emit('send', { body: draft.value.trim(), attachmentMediaIds: [...readyIds.value] })
}

function reset() {
  draft.value = ''
  attachments.clear()
}

defineExpose({ reset })
</script>

<template>
  <footer class="conv__composer">
    <p
      v-if="closed"
      class="conv__hint"
      data-test="closed-notice"
    >
      {{ t('conversations.closedNotice') }}
    </p>
    <form
      v-else
      @submit.prevent="submit"
    >
      <div
        v-if="pending.length"
        class="conv__pending-attachments"
        data-test="pending-attachments"
      >
        <div
          v-for="p in pending"
          :key="p.key"
          class="conv__pending-chip"
          :class="{ 'is-error': !!p.error }"
          data-test="pending-attachment"
        >
          <span class="conv__pending-name">📎 {{ p.file.name }}</span>
          <el-icon
            v-if="p.uploading"
            class="is-loading"
            data-test="uploading-spinner"
          >
            <Loading />
          </el-icon>
          <span
            v-else-if="p.error"
            class="conv__pending-error"
            data-test="pending-error"
          >{{ p.error }}</span>
          <button
            type="button"
            class="conv__pending-remove"
            :aria-label="t('conversations.removeAttachment')"
            data-test="remove-pending"
            @click="attachments.remove(p.key)"
          >
            ×
          </button>
        </div>
      </div>

      <el-input
        v-model="draft"
        type="textarea"
        :rows="3"
        :maxlength="MESSAGE_MAX_LENGTH"
        :placeholder="t('conversations.composer')"
        data-test="composer"
        @keydown.ctrl.enter.prevent="submit"
        @keydown.meta.enter.prevent="submit"
      />
      <div class="conv__composer-actions">
        <div class="conv__composer-left">
          <input
            ref="fileInput"
            type="file"
            multiple
            accept="image/*,application/pdf,.doc,.docx"
            class="conv__file-input"
            :disabled="sending || attachmentsFull"
            data-test="file-input"
            @change="onFilePicked"
          >
          <el-button
            size="small"
            :icon="Paperclip"
            :disabled="sending || attachmentsFull"
            data-test="attach-button"
            @click="fileInput?.click()"
          >
            {{ t('conversations.attach') }}
          </el-button>
          <span
            v-if="pending.length"
            class="conv__attach-count"
            data-test="attach-count"
          >
            {{ pending.length }}/{{ MAX_ATTACHMENTS }}
          </span>
        </div>
        <el-button
          type="primary"
          native-type="submit"
          :loading="sending"
          :disabled="!canSend"
          data-test="send"
        >
          {{ t('conversations.send') }}
        </el-button>
      </div>
    </form>
  </footer>
</template>

<style scoped>
.conv__composer { padding: 12px 16px; border-top: 1px solid var(--nad-line, #e5e7eb); margin-top: auto; }
.conv__composer-actions { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.conv__composer-left { display: flex; align-items: center; gap: 8px; }
.conv__file-input { display: none; }
.conv__attach-count { font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.conv__hint { margin: 0; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
.conv__pending-attachments { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.conv__pending-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--nad-surface-2, #f1f5f9);
  border: 1px solid var(--nad-line, #e2e8f0);
  border-radius: 6px;
  padding: 2px 8px;
  font-size: 12px;
  color: var(--nad-ink, #1e293b);
  max-width: 100%;
}
.conv__pending-chip.is-error {
  border-color: var(--el-color-danger, #ef4444);
  background: #fef2f2;
  color: var(--el-color-danger, #ef4444);
}
.conv__pending-name {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv__pending-error {
  font-size: 11px;
}
.conv__pending-remove {
  background: none;
  border: none;
  color: var(--nad-ink-soft, #64748b);
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  padding: 0 2px;
}
.conv__pending-remove:hover {
  color: var(--el-color-danger, #ef4444);
}
</style>
