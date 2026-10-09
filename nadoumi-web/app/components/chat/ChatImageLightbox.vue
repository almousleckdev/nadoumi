<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

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

<template>
  <Teleport to="body">
    <div
      v-if="current"
      class="fixed inset-0 z-[60] grid place-items-center bg-slate-900/40 p-4 backdrop-blur-md"
      role="dialog"
      aria-modal="true"
      :aria-label="current.filename ?? t('dashboard.messages.attachment')"
      data-test="lightbox"
      @click.self="emit('close')"
      @touchstart.passive="onTouchStart"
      @touchend.passive="onTouchEnd"
    >
      <div class="relative flex max-h-full max-w-full flex-col overflow-hidden rounded-2xl bg-white shadow-2xl">
        <div class="flex items-center justify-between gap-3 border-b border-slate-100 px-4 py-2.5">
          <p class="min-w-0 truncate text-sm font-medium text-slate-900" data-test="lightbox-name">{{ current.filename }}</p>
          <div class="flex shrink-0 items-center gap-1">
            <span v-if="many" class="me-1 text-xs font-medium text-slate-500" data-test="lightbox-counter">
              {{ t('dashboard.messages.imageCounter', { n: index + 1, total: images.length }) }}
            </span>
            <a
              :href="downloadUrl"
              :download="current.filename ?? undefined"
              class="grid h-8 w-8 place-items-center rounded-full text-slate-600 hover:bg-slate-100"
              :aria-label="t('dashboard.messages.download')"
              :title="t('dashboard.messages.download')"
            >
              <svg viewBox="0 0 24 24" class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M12 4v12m0 0-4-4m4 4 4-4M5 20h14" stroke-linecap="round" stroke-linejoin="round" /></svg>
            </a>
            <button
              ref="closeButton"
              type="button"
              class="grid h-8 w-8 place-items-center rounded-full text-slate-600 hover:bg-slate-100"
              :aria-label="t('dashboard.messages.close')"
              data-test="lightbox-close"
              @click="emit('close')"
            >
              <svg viewBox="0 0 24 24" class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18" stroke-linecap="round" /></svg>
            </button>
          </div>
        </div>
        <div class="grid min-h-0 flex-1 place-items-center bg-slate-50 p-3">
          <img :key="current.url" :src="current.url" :alt="current.filename ?? ''" class="max-h-[75vh] max-w-[85vw] rounded-lg object-contain">
        </div>

        <template v-if="many">
          <button
            type="button"
            class="absolute start-3 top-1/2 grid h-10 w-10 -translate-y-1/2 place-items-center rounded-full bg-white/95 text-slate-800 shadow-md ring-1 ring-slate-200 hover:bg-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-500"
            :aria-label="t('dashboard.messages.previousImage')"
            data-test="lightbox-prev"
            @click="step(-1)"
          >
            <svg viewBox="0 0 24 24" class="h-5 w-5 rtl:-scale-x-100" fill="none" stroke="currentColor" stroke-width="2.2" aria-hidden="true"><path d="m15 6-6 6 6 6" stroke-linecap="round" stroke-linejoin="round" /></svg>
          </button>
          <button
            type="button"
            class="absolute end-3 top-1/2 grid h-10 w-10 -translate-y-1/2 place-items-center rounded-full bg-white/95 text-slate-800 shadow-md ring-1 ring-slate-200 hover:bg-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-500"
            :aria-label="t('dashboard.messages.nextImage')"
            data-test="lightbox-next"
            @click="step(1)"
          >
            <svg viewBox="0 0 24 24" class="h-5 w-5 rtl:-scale-x-100" fill="none" stroke="currentColor" stroke-width="2.2" aria-hidden="true"><path d="m9 6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round" /></svg>
          </button>
        </template>
      </div>
    </div>
  </Teleport>
</template>
