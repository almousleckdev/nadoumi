<template>
  <Teleport to="body">
    <div
      class="alt"
      role="dialog"
      aria-modal="true"
      :aria-label="t('news.editor.image.altTitle')"
      @keydown.esc.stop="emit('cancel')"
    >
      <button
        type="button"
        class="alt__close"
        :aria-label="t('news.editor.image.cancel')"
        @click="emit('cancel')"
      >
        <el-icon :size="22">
          <Close />
        </el-icon>
      </button>

      <div class="alt__card">
        <h2 class="alt__title">
          {{ t('news.editor.image.altTitle') }}
        </h2>
        <p class="alt__help">
          {{ t('news.editor.image.altHelp') }}
        </p>
        <img
          class="alt__preview"
          :src="src"
          alt=""
        >
        <input
          ref="field"
          v-model="draft"
          class="alt__input"
          type="text"
          maxlength="300"
          :placeholder="t('news.editor.image.altPlaceholder')"
          :aria-label="t('news.editor.image.altTitle')"
          @keydown.enter.prevent="emit('save', draft.trim())"
        >
        <div class="alt__actions">
          <button
            type="button"
            class="alt__btn alt__btn--primary"
            @click="emit('save', draft.trim())"
          >
            {{ t('news.editor.image.save') }}
          </button>
          <button
            type="button"
            class="alt__btn"
            @click="emit('cancel')"
          >
            {{ t('news.editor.image.cancel') }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Close } from '@element-plus/icons-vue'

/** Medium's "Alternative text" overlay: describe the picture for readers who cannot see it. */
const props = defineProps<{ src: string, alt: string }>()
const emit = defineEmits<{ save: [alt: string], cancel: [] }>()

const { t } = useI18n()
const draft = ref(props.alt)
const field = ref<HTMLInputElement>()

onMounted(() => field.value?.focus())
</script>

<style scoped>
.alt {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgb(255 255 255 / 97%);
  font-family: var(--ne-sans, system-ui);
}
.alt__close {
  position: absolute;
  inset-block-start: 20px;
  inset-inline-end: 24px;
  display: grid;
  padding: 6px;
  border: 0;
  background: transparent;
  color: #242424;
  cursor: pointer;
}
.alt__card {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: min(100%, 720px);
  text-align: center;
}
.alt__title {
  margin: 0 0 14px;
  color: #242424;
  font: 700 26px/32px var(--ne-sans, system-ui);
}
.alt__help {
  margin: 0 0 28px;
  color: #242424;
  font: 400 18px/26px var(--ne-sans, system-ui);
}
.alt__preview {
  width: 288px;
  max-width: 100%;
  max-height: 200px;
  object-fit: cover;
}
.alt__input {
  width: 100%;
  margin-top: 32px;
  padding: 8px 12px;
  border: 0;
  border-inline-start: 1px solid #c8c8c8;
  outline: 0;
  background: transparent;
  color: #242424;
  font: 400 18px/26px var(--ne-sans, system-ui);
}
.alt__input::placeholder {
  color: #b3b3b1;
}
.alt__actions {
  display: flex;
  gap: 10px;
  margin-top: 28px;
}
.alt__btn {
  padding: 7px 18px;
  border: 1px solid #c8c8c8;
  border-radius: 999px;
  background: transparent;
  color: #242424;
  font: 400 14px/20px var(--ne-sans, system-ui);
  cursor: pointer;
}
.alt__btn--primary {
  border-color: var(--ne-accent, #1a8917);
  color: var(--ne-accent, #1a8917);
}
.alt__btn:hover,
.alt__btn:focus-visible {
  border-color: #242424;
}
.alt__btn--primary:hover,
.alt__btn--primary:focus-visible {
  border-color: var(--ne-accent, #1a8917);
  background: var(--ne-accent, #1a8917);
  color: #fff;
}
</style>
