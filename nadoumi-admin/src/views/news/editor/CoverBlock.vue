<template>
  <div class="cover">
    <figure
      v-if="coverUrl"
      class="cover__figure"
    >
      <img
        class="cover__img"
        :class="{ 'cover__img--busy': uploading }"
        :src="coverUrl"
        :alt="t('news.editor.cover.alt')"
      >
      <div
        v-if="uploading"
        class="cover__loading"
        role="status"
        data-test="cover-loading"
      >
        <el-icon
          class="is-loading"
          :size="22"
        >
          <Loading />
        </el-icon>
        {{ t('news.editor.cover.uploading') }}
      </div>
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

    <div
      v-else-if="uploading"
      class="cover__placeholder"
      role="status"
      data-test="cover-loading"
    >
      <el-icon
        class="is-loading"
        :size="26"
      >
        <Loading />
      </el-icon>
      {{ t('news.editor.cover.uploading') }}
    </div>

    <button
      v-else-if="!disabled"
      type="button"
      class="cover__add"
      @click="open"
    >
      <el-icon :size="18">
        <Picture />
      </el-icon>
      {{ t('news.editor.cover.add') }}
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
import { Loading, Picture } from '@element-plus/icons-vue'

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
.cover__img--busy {
  opacity: 0.45;
  filter: blur(1px);
}
.cover__loading,
.cover__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--ne-ink);
  font: 400 14px/20px var(--ne-sans);
}
.cover__loading {
  position: absolute;
  inset: 0;
  background: rgb(255 255 255 / 55%);
}
.cover__placeholder {
  min-height: 220px;
  border-radius: 2px;
  background: linear-gradient(90deg, #f2f2f2 25%, #e8e8e8 37%, #f2f2f2 63%);
  background-size: 400% 100%;
  animation: cover-shimmer 1.4s ease infinite;
}
@keyframes cover-shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
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
