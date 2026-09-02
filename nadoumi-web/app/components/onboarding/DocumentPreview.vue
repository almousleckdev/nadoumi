<script setup lang="ts">
/**
 * Read-optimised viewer for an uploaded document image: zoom, rotate, pan.
 * For PDFs it shows a file card (no PDF renderer bundled). Client-only.
 */
const props = defineProps<{ src: string; type: string; name?: string }>()
const { t } = useI18n()

const isPdf = computed(() => props.type === 'application/pdf' || props.src.startsWith('data:application/pdf'))
const zoom = ref(1)
const rotation = ref(0)
const offset = reactive({ x: 0, y: 0 })
const dragging = ref(false)
let start = { x: 0, y: 0 }

const transform = computed(() =>
  `translate(${offset.x}px, ${offset.y}px) rotate(${rotation.value}deg) scale(${zoom.value})`)

function down(e: PointerEvent) {
  dragging.value = true
  start = { x: e.clientX - offset.x, y: e.clientY - offset.y }
  ;(e.target as HTMLElement).setPointerCapture(e.pointerId)
}
function move(e: PointerEvent) {
  if (dragging.value) { offset.x = e.clientX - start.x; offset.y = e.clientY - start.y }
}
function up() { dragging.value = false }
function zoomBy(d: number) { zoom.value = Math.min(4, Math.max(1, +(zoom.value + d).toFixed(2))) }
function rotate() { rotation.value = (rotation.value + 90) % 360 }
function fit() { zoom.value = 1; rotation.value = 0; offset.x = 0; offset.y = 0 }
</script>

<template>
  <div class="grid gap-2">
    <div
      v-if="!isPdf"
      class="relative h-72 overflow-hidden rounded-lg border border-slate-200 bg-slate-900/5 touch-none"
      @pointerdown="down"
      @pointermove="move"
      @pointerup="up"
      @pointercancel="up"
      @wheel.prevent="zoomBy(-$event.deltaY * 0.001)"
    >
      <img
        :src="src"
        :alt="name ?? t('onboarding.doc.previewAlt')"
        draggable="false"
        class="absolute left-1/2 top-1/2 max-w-none -translate-x-1/2 -translate-y-1/2 select-none"
        :style="{ transform }"
      >
    </div>
    <div v-else class="flex items-center gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm">
      <span class="rounded bg-white px-2 py-1 text-xs font-semibold text-slate-500">PDF</span>
      <span class="min-w-0 truncate text-slate-700">{{ name ?? 'document.pdf' }}</span>
    </div>

    <div v-if="!isPdf" class="flex flex-wrap gap-2">
      <NButton size="sm" variant="secondary" @click="zoomBy(0.25)">{{ t('onboarding.doc.zoomIn') }}</NButton>
      <NButton size="sm" variant="secondary" @click="zoomBy(-0.25)">{{ t('onboarding.doc.zoomOut') }}</NButton>
      <NButton size="sm" variant="secondary" @click="rotate">{{ t('onboarding.crop.rotate') }}</NButton>
      <NButton size="sm" variant="ghost" @click="fit">{{ t('onboarding.doc.fit') }}</NButton>
    </div>
  </div>
</template>
