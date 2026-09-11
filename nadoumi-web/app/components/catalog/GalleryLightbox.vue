<script setup lang="ts">
import type { UniversityGalleryImage } from '~/types/catalog'

const props = defineProps<{
  images: UniversityGalleryImage[]
  /** Index of the open image, or null when the lightbox is closed. */
  modelValue: number | null
}>()
const emit = defineEmits<{ 'update:modelValue': [value: number | null] }>()

const { t } = useI18n()
const panel = ref<HTMLElement | null>(null)

const open = computed(() => props.modelValue != null)
const current = computed(() =>
  props.modelValue == null ? null : props.images[props.modelValue] ?? null)
const many = computed(() => props.images.length > 1)

function close() { emit('update:modelValue', null) }
function step(delta: number) {
  if (props.modelValue == null || !props.images.length) return
  const n = props.images.length
  emit('update:modelValue', (props.modelValue + delta + n) % n)
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') close()
  else if (e.key === 'ArrowRight') step(1)
  else if (e.key === 'ArrowLeft') step(-1)
}

watch(open, async (isOpen: boolean) => {
  if (!import.meta.client) return
  document[isOpen ? 'addEventListener' : 'removeEventListener']('keydown', onKey as EventListener)
  document.body.style.overflow = isOpen ? 'hidden' : ''
  if (isOpen) { await nextTick(); panel.value?.focus() }
})
onBeforeUnmount(() => {
  if (!import.meta.client) return
  document.removeEventListener('keydown', onKey as EventListener)
  document.body.style.overflow = ''
})
</script>

<template>
  <ClientOnly>
    <Teleport to="body">
      <Transition name="lb-fade">
        <div
          v-if="open && current"
          ref="panel"
          role="dialog"
          aria-modal="true"
          :aria-label="t('university.gallery')"
          tabindex="-1"
          class="fixed inset-0 z-[60] flex flex-col bg-slate-950/90 outline-none backdrop-blur-sm"
          @click.self="close"
        >
          <!-- top bar -->
          <div class="flex items-center justify-between gap-4 px-4 py-3 text-white/80">
            <span class="text-sm tabular-nums">
              {{ (modelValue ?? 0) + 1 }} / {{ images.length }}
            </span>
            <button
              type="button"
              class="rounded-full p-2 transition hover:bg-white/10 hover:text-white"
              :aria-label="t('common.close')"
              @click="close"
            >
              <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 6l12 12M18 6L6 18" stroke-linecap="round" />
              </svg>
            </button>
          </div>

          <!-- stage -->
          <div class="relative flex min-h-0 flex-1 items-center justify-center px-4 pb-4" @click.self="close">
            <button
              v-if="many"
              type="button"
              class="absolute left-2 top-1/2 -translate-y-1/2 rounded-full bg-white/10 p-2 text-white transition hover:bg-white/20 sm:left-4"
              :aria-label="t('common.previous')"
              @click="step(-1)"
            >
              <svg viewBox="0 0 24 24" class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M15 6l-6 6 6 6" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>

            <figure class="flex max-h-full max-w-5xl flex-col items-center">
              <img
                :key="current.id"
                :src="mediaUrl(current.url ?? current.imageUrl)"
                :alt="current.caption ?? ''"
                class="max-h-[78vh] w-auto max-w-full rounded-lg object-contain shadow-2xl"
              >
              <figcaption
                v-if="current.caption"
                class="mt-3 max-w-2xl text-center text-sm text-white/80"
              >
                {{ current.caption }}
              </figcaption>
            </figure>

            <button
              v-if="many"
              type="button"
              class="absolute right-2 top-1/2 -translate-y-1/2 rounded-full bg-white/10 p-2 text-white transition hover:bg-white/20 sm:right-4"
              :aria-label="t('common.next')"
              @click="step(1)"
            >
              <svg viewBox="0 0 24 24" class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 6l6 6-6 6" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>
  </ClientOnly>
</template>

<style scoped>
.lb-fade-enter-active,
.lb-fade-leave-active {
  transition: opacity 0.15s ease;
}
.lb-fade-enter-from,
.lb-fade-leave-to {
  opacity: 0;
}
</style>
