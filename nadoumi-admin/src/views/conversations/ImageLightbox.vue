<template>
  <Teleport to="body">
    <div
      v-if="current"
      class="lb"
      role="dialog"
      aria-modal="true"
      :aria-label="current.filename ?? t('conversations.attachment')"
      data-test="lightbox"
      @click.self="emit('close')"
      @touchstart.passive="onTouchStart"
      @touchend.passive="onTouchEnd"
    >
      <div class="lb__card">
        <div class="lb__bar">
          <p
            class="lb__name"
            data-test="lightbox-name"
          >
            {{ current.filename }}
          </p>
          <div class="lb__actions">
            <span
              v-if="many"
              class="lb__count"
              data-test="lightbox-counter"
            >{{ t('conversations.imageCounter', { n: index + 1, total: images.length }) }}</span>
            <a
              :href="downloadUrl"
              :download="current.filename ?? undefined"
              class="lb__btn"
              :aria-label="t('conversations.download')"
              :title="t('conversations.download')"
            >
              <el-icon :size="16"><Download /></el-icon>
            </a>
            <button
              ref="closeButton"
              type="button"
              class="lb__btn"
              :aria-label="t('conversations.closeViewer')"
              data-test="lightbox-close"
              @click="emit('close')"
            >
              <el-icon :size="16">
                <Close />
              </el-icon>
            </button>
          </div>
        </div>
        <div class="lb__stage">
          <img
            :key="current.url"
            :src="current.url"
            :alt="current.filename ?? ''"
          >
        </div>
        <template v-if="many">
          <button
            type="button"
            class="lb__nav lb__nav--prev"
            :aria-label="t('conversations.previousImage')"
            data-test="lightbox-prev"
            @click="step(-1)"
          >
            <el-icon :size="20">
              <ArrowLeft />
            </el-icon>
          </button>
          <button
            type="button"
            class="lb__nav lb__nav--next"
            :aria-label="t('conversations.nextImage')"
            data-test="lightbox-next"
            @click="step(1)"
          >
            <el-icon :size="20">
              <ArrowRight />
            </el-icon>
          </button>
        </template>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, ArrowRight, Close, Download } from '@element-plus/icons-vue'

export interface GalleryImage { url: string, filename: string | null }

/**
 * Image viewer over a blurred backdrop: the picture in a centred card, with previous/next through every image of
 * the conversation (buttons, arrow keys, or a swipe), a counter, download and close. Esc or a click outside closes
 * it and focus returns to the opener.
 */
const props = defineProps<{ images: GalleryImage[], start: number }>()
const emit = defineEmits<{ close: [] }>()
const { t } = useI18n()

const SWIPE_PX = 50
const index = ref(Math.min(Math.max(props.start, 0), Math.max(props.images.length - 1, 0)))
const current = computed(() => props.images[index.value])
const many = computed(() => props.images.length > 1)
const closeButton = ref<HTMLButtonElement | null>(null)
let opener: HTMLElement | null = null
let touchX = 0

function step(delta: number) {
  if (!many.value) return
  index.value = (index.value + delta + props.images.length) % props.images.length
}

function onKey(e: KeyboardEvent) {
  const rtl = document.documentElement.dir === 'rtl'
  if (e.key === 'Escape') emit('close')
  else if (e.key === 'ArrowRight') step(rtl ? -1 : 1)
  else if (e.key === 'ArrowLeft') step(rtl ? 1 : -1)
}
function onTouchStart(e: TouchEvent) {
  touchX = e.touches[0]?.clientX ?? 0
}
function onTouchEnd(e: TouchEvent) {
  const dx = (e.changedTouches[0]?.clientX ?? 0) - touchX
  if (Math.abs(dx) >= SWIPE_PX) step(dx < 0 ? 1 : -1)
}

onMounted(() => {
  opener = document.activeElement as HTMLElement | null
  document.addEventListener('keydown', onKey)
  document.body.style.overflow = 'hidden'
  closeButton.value?.focus()
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey)
  document.body.style.overflow = ''
  opener?.focus?.()
})
const downloadUrl = computed(() => current.value ? current.value.url + (current.value.url.includes('?') ? '&' : '?') + 'download=1' : '#')
</script>

<style scoped>
.lb { position: fixed; inset: 0; z-index: 3000; display: grid; place-items: center; padding: 16px; background: rgba(15, 23, 42, 0.4); backdrop-filter: blur(8px); }
.lb__card { position: relative; display: flex; flex-direction: column; max-width: 100%; max-height: 100%; overflow: hidden; border-radius: 16px; background: #fff; box-shadow: 0 25px 60px rgba(15, 23, 42, 0.35); }
.lb__bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 16px; border-bottom: 1px solid #f1f5f9; }
.lb__name { margin: 0; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; font-weight: 500; color: var(--nad-ink); }
.lb__actions { display: flex; flex-shrink: 0; align-items: center; gap: 4px; }
.lb__count { margin-inline-end: 4px; font-size: 12px; font-weight: 500; color: var(--nad-ink-soft); }
.lb__btn { display: grid; place-items: center; width: 32px; height: 32px; border: 0; border-radius: 50%; background: transparent; color: var(--nad-ink-soft); text-decoration: none; cursor: pointer; }
.lb__btn:hover { background: #f1f5f9; }
.lb__stage { display: grid; place-items: center; min-height: 0; padding: 12px; background: #f8fafc; }
.lb__stage img { max-width: 85vw; max-height: 75vh; border-radius: 8px; object-fit: contain; }
.lb__nav { position: absolute; inset-block-start: 50%; display: grid; place-items: center; width: 40px; height: 40px; transform: translateY(-50%); border: 0; border-radius: 50%; background: rgba(255, 255, 255, 0.95); box-shadow: var(--nad-shadow-md), 0 0 0 1px var(--nad-line); color: var(--nad-ink); cursor: pointer; }
.lb__nav:hover { background: #fff; }
.lb__nav:focus-visible, .lb__btn:focus-visible { outline: 2px solid var(--nad-brand-500); }
.lb__nav--prev { inset-inline-start: 12px; }
.lb__nav--next { inset-inline-end: 12px; }
</style>
