<template>
  <div class="cover">
    <figure
      v-if="coverUrl"
      class="cover__figure"
    >
      <img
        class="cover__img"
        :src="coverUrl"
        :alt="t('news.editor.cover.alt')"
      >
      <div
        v-if="!disabled"
        class="cover__actions"
      >
        <button
          type="button"
          class="cover__btn"
          :disabled="uploading"
          @click="open"
        >
          {{ uploading ? t('news.editor.cover.uploading') : t('news.editor.cover.replace') }}
        </button>
      </div>
    </figure>

    <button
      v-else-if="!disabled"
      type="button"
      class="cover__add"
      :disabled="uploading"
      @click="open"
    >
      <el-icon :size="18">
        <Picture />
      </el-icon>
      {{ uploading ? t('news.editor.cover.uploading') : t('news.editor.cover.add') }}
    </button>

    <input
      ref="input"
      type="file"
      accept="image/jpeg,image/png,image/webp"
      hidden
      @change="onPicked"
    >
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Picture } from '@element-plus/icons-vue'

/** The article's cover: a quiet "add" link while empty, the picture with a Replace action once set. */
defineProps<{ coverUrl: string, uploading: boolean, disabled: boolean }>()
const emit = defineEmits<{ select: [file: File] }>()

const { t } = useI18n()
const input = ref<HTMLInputElement>()

function open() {
  input.value?.click()
}

function onPicked(event: Event) {
  const field = event.target as HTMLInputElement
  const file = field.files?.[0]
  field.value = ''
  if (file) emit('select', file)
}

defineExpose({ open })
</script>

<style scoped>
.cover {
  margin-top: 28px;
}
.cover__figure {
  position: relative;
  margin: 0;
}
.cover__img {
  display: block;
  width: 100%;
  max-height: 420px;
  object-fit: cover;
  border-radius: 2px;
}
.cover__actions {
  position: absolute;
  inset-block-start: 12px;
  inset-inline-end: 12px;
  opacity: 0;
  transition: opacity 150ms ease;
}
.cover__figure:hover .cover__actions,
.cover__figure:focus-within .cover__actions {
  opacity: 1;
}
.cover__btn {
  padding: 7px 16px;
  border: 0;
  border-radius: 999px;
  background: rgb(36 36 36 / 88%);
  color: #fff;
  font: 400 14px/20px var(--ne-sans);
  cursor: pointer;
}
.cover__btn:hover:not(:disabled) {
  background: #242424;
}
.cover__add {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border: 0;
  background: transparent;
  color: var(--ne-muted);
  font: 400 14px/20px var(--ne-sans);
  cursor: pointer;
}
.cover__add:hover:not(:disabled),
.cover__add:focus-visible {
  color: var(--ne-ink);
}
.cover__btn:disabled,
.cover__add:disabled {
  cursor: progress;
}
</style>
