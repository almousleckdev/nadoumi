<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Document, Warning } from '@element-plus/icons-vue'
import type { ConversationMessage } from '@/api/conversation'
import { formatMessageTime } from '@/utils/messages'

defineProps<{ message: ConversationMessage, mine: boolean, chained: boolean }>()

const { t, locale } = useI18n()

const IMAGE_FILE = /\.(jpeg|jpg|gif|png|webp)$/i
</script>

<template>
  <div
    class="conv__msg"
    :class="{ 'conv__msg--mine': mine, 'conv__msg--chained': chained }"
    data-test="message"
  >
    <div class="conv__bubble">
      <p class="conv__body">
        {{ message.body }}
      </p>

      <div
        v-if="message.attachments && message.attachments.length"
        class="conv__attachments"
      >
        <div
          v-for="a in message.attachments"
          :key="a.id"
          class="conv__attachment-item"
        >
          <el-image
            v-if="a.url && a.filename && IMAGE_FILE.test(a.filename)"
            :src="a.url"
            class="conv__image-preview"
            :preview-src-list="[a.url]"
            fit="cover"
            lazy
          />
          <a
            v-else-if="a.url"
            :href="a.url"
            target="_blank"
            rel="noopener noreferrer"
            class="conv__file-card"
          >
            <el-icon class="conv__file-icon"><Document /></el-icon>
            <span
              class="conv__file-name"
              :title="a.filename || t('conversations.attachment')"
            >{{ a.filename || t('conversations.attachment') }}</span>
          </a>
          <span
            v-else
            class="conv__attach-missing"
          >
            <el-icon><Warning /></el-icon>
            {{ t('conversations.attachmentUnavailable', { name: a.filename || t('conversations.attachment') }) }}
          </span>
        </div>
      </div>

      <p class="conv__time">
        {{ formatMessageTime(message.createdAt, locale) }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.conv__msg { display: flex; justify-content: flex-start; }
.conv__msg--mine { justify-content: flex-end; }
.conv__msg--chained { margin-top: -6px; }
.conv__bubble { max-width: 80%; padding: 8px 12px; border-radius: 12px; background: var(--nad-surface-2, #f1f5f9); font-size: 14px; }
.conv__msg--mine .conv__bubble { background: var(--el-color-primary-light-9, #eef4ff); }
.conv__msg--chained .conv__bubble { border-top-left-radius: 4px; border-top-right-radius: 4px; }
.conv__body { margin: 2px 0 0; white-space: pre-wrap; overflow-wrap: anywhere; }
.conv__time { margin: 4px 0 0; text-align: end; font-size: 11px; color: var(--nad-ink-faint, #9ca3af); }
.conv__attachments { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
.conv__image-preview { border-radius: 8px; border: 1px solid var(--nad-line, #e2e8f0); max-width: 240px; max-height: 240px; display: block; }
.conv__file-card { display: flex; align-items: center; gap: 8px; padding: 10px 14px; background: var(--nad-surface, #ffffff); border: 1px solid var(--nad-line, #e2e8f0); border-radius: 8px; text-decoration: none; transition: background 0.2s; max-width: 280px; }
.conv__file-card:hover { background: var(--nad-surface-2, #f8fafc); }
.conv__file-icon { font-size: 18px; color: var(--el-color-primary, #4338ca); }
.conv__file-name { font-size: 13px; color: var(--nad-ink, #1e293b); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv__attach-missing { font-size: 12px; color: var(--el-color-danger, #ef4444); font-style: italic; display: flex; align-items: center; gap: 4px; }
</style>
