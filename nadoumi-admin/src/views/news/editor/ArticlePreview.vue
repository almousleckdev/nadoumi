<template>
  <Teleport to="body">
    <div
      class="pv ne-theme"
      role="dialog"
      aria-modal="true"
      :aria-label="t('news.editor.preview.title')"
      @keydown.esc.stop="emit('close')"
    >
      <header class="pv__bar">
        <button
          ref="closeButton"
          type="button"
          class="pv__back"
          @click="emit('close')"
        >
          <el-icon :size="18">
            <ArrowLeft />
          </el-icon>
          {{ t('news.editor.preview.back') }}
        </button>

        <div
          class="pv__devices"
          role="group"
          :aria-label="t('news.editor.preview.device')"
        >
          <button
            v-for="d in devices"
            :key="d.key"
            type="button"
            class="pv__device"
            :class="{ 'pv__device--on': device === d.key }"
            :aria-pressed="device === d.key"
            @click="device = d.key"
          >
            <el-icon :size="16">
              <component :is="d.icon" />
            </el-icon>
            {{ d.label }}
          </button>
        </div>

        <span class="pv__badge">{{ t('news.editor.preview.badge') }}</span>
      </header>

      <div class="pv__scroll">
        <div
          class="pv__stage"
          :class="`pv__stage--${device}`"
        >
          <article class="pv__article">
            <img
              v-if="coverUrl"
              class="pv__cover"
              :src="coverUrl"
              alt=""
            >
            <h1 class="pv__title">
              {{ title.trim() || t('news.untitled') }}
            </h1>
            <p
              v-if="subtitle.trim()"
              class="pv__subtitle"
            >
              {{ subtitle }}
            </p>
            <div class="pv__byline">
              <Avatar
                :name="authorName"
                :src="authorAvatar"
                :size="40"
              />
              <div>
                <p class="pv__author">
                  {{ t('news.editor.preview.by', { name: authorName }) }}
                </p>
                <p class="pv__meta">
                  {{ whenLabel }} · {{ t('news.editor.preview.readTime', { n: readMinutes }) }}
                </p>
              </div>
            </div>
            <editor-content :editor="editor" />
          </article>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, Cellphone, Monitor } from '@element-plus/icons-vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import Avatar from '@/components/ui/Avatar.vue'
import { formatDateTime } from '@/utils/date'
import { wordCount } from './dialect'
import { createNewsExtensions } from './extensions'
import { newsNodeViews } from './nodeViews'
import './editor.css'

/**
 * A read-only rendering of the article as readers will see it, including edits that are not saved yet.
 * It mounts its own editor over the same schema and node views, so the output is the same one the
 * writer authored, only without any editing chrome.
 */
const props = defineProps<{
  title: string
  subtitle: string
  coverUrl: string
  authorName: string
  authorAvatar?: string
  /** First publish time, or null for an article that has not been published. */
  publishedAt: string | null
  markdown: string
}>()
const emit = defineEmits<{ close: [] }>()

const { t } = useI18n()

type Device = 'desktop' | 'mobile'
const device = ref<Device>('desktop')
const closeButton = ref<HTMLButtonElement>()

const WORDS_PER_MINUTE = 200

const devices = computed(() => [
  { key: 'desktop' as const, label: t('news.editor.preview.desktop'), icon: Monitor },
  { key: 'mobile' as const, label: t('news.editor.preview.mobile'), icon: Cellphone },
])

const editor = useEditor({
  extensions: createNewsExtensions({ placeholder: '', nodeViews: newsNodeViews }),
  content: props.markdown,
  editable: false,
  editorProps: { attributes: { class: 'ne-prose ne-prose--readonly' } },
})

const readMinutes = computed(() => Math.max(1, Math.ceil(wordCount(props.markdown) / WORDS_PER_MINUTE)))
const whenLabel = computed(() => (props.publishedAt ? formatDateTime(props.publishedAt) : t('news.editor.preview.draft')))

// The page behind must not scroll while the overlay is open.
let previousOverflow = ''
onMounted(() => {
  previousOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  closeButton.value?.focus()
})
onBeforeUnmount(() => {
  document.body.style.overflow = previousOverflow
})
</script>

<style scoped>
.pv {
  position: fixed;
  inset: 0;
  z-index: 2500;
  display: flex;
  flex-direction: column;
  font-family: var(--ne-sans);
}
.pv__bar {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 16px;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #ececec;
  background: var(--ne-paper);
}
.pv__back {
  justify-self: start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px 6px 4px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--ne-ink);
  font: 400 14px/20px var(--ne-sans);
  cursor: pointer;
}
.pv__back:hover,
.pv__back:focus-visible {
  background: #f2f2f2;
}
.pv__devices {
  display: inline-flex;
  padding: 3px;
  border-radius: 999px;
  background: #f2f2f2;
}
.pv__device {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--ne-muted);
  font: 400 13px/20px var(--ne-sans);
  cursor: pointer;
}
.pv__device--on {
  background: #fff;
  color: var(--ne-ink);
  box-shadow: 0 1px 3px rgb(0 0 0 / 14%);
}
.pv__badge {
  justify-self: end;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--ne-accent-soft);
  color: var(--ne-accent-strong);
  font: 500 12px/18px var(--ne-sans);
  letter-spacing: 0.02em;
}
.pv__back:focus-visible,
.pv__device:focus-visible {
  outline: 2px solid var(--ne-accent);
  outline-offset: 2px;
}

.pv__scroll {
  flex: 1;
  overflow: auto;
  background: #f7f7f7;
}
.pv__stage {
  /* a size container: full-bleed figures measure against this frame, not the browser window */
  container-type: inline-size;
  --ne-full-width: 100cqw;
  --ne-wide-width: min(1032px, calc(100cqw - 32px));
  margin: 0 auto;
  min-height: 100%;
  background: var(--ne-paper);
  overflow-x: clip;
}
.pv__stage--mobile {
  width: 390px;
  max-width: 100%;
  box-shadow: 0 0 0 1px #e6e6e6, 0 8px 30px rgb(0 0 0 / 8%);
}
.pv__article {
  max-width: 680px;
  margin: 0 auto;
  padding: 56px 16px 120px;
}
.pv__cover {
  display: block;
  width: 100%;
  max-height: 420px;
  margin-bottom: 28px;
  object-fit: cover;
  border-radius: 2px;
}
.pv__title {
  margin: 0;
  font: 700 42px/52px var(--ne-serif);
  letter-spacing: -0.016em;
}
.pv__subtitle {
  margin: 10px 0 0;
  color: var(--ne-muted);
  font: 400 22px/30px var(--ne-serif);
}
.pv__byline {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 28px 0 32px;
}
.pv__author,
.pv__meta {
  margin: 0;
  font: 400 14px/20px var(--ne-sans);
}
.pv__meta {
  color: var(--ne-muted);
}

@container (max-width: 600px) {
  .pv__article {
    padding-top: 32px;
  }
  .pv__title {
    font-size: 30px;
    line-height: 38px;
  }
  .pv__subtitle {
    font-size: 19px;
    line-height: 26px;
  }
}
</style>
