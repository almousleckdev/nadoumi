<script setup lang="ts">
/**
 * Read-optimised viewer for an uploaded document image: zoom, rotate, pan.
 * For PDFs it shows a file card (no PDF renderer bundled). Client-only.
 */
const props = defineProps<{ src: string; type: string; name?: string }>()
const { t } = useI18n()

const isPdf = computed(() => props.type === 'application/pdf' || props.src.startsWith('data:application/pdf'))
const { transform, onPointerDown, onPointerMove, onPointerUp, zoomBy, rotate, reset } = usePanZoom({ min: 1, max: 4 })
</script>

<template>
  <div class="grid gap-2">
    <div
      v-if="!isPdf"
      class="relative h-72 overflow-hidden rounded-lg border border-slate-200 bg-slate-900/5 touch-none"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointercancel="onPointerUp"
      @wheel.prevent="zoomBy(-$event.deltaY * 0.001)"
    >
      <img
        :src="src"
        :alt="name ?? t('onboarding.doc.previewAlt')"
        draggable="false"
        class="absolute left-1/2 top-1/2 h-full w-full -translate-x-1/2 -translate-y-1/2 select-none object-contain"
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
      <NButton size="sm" variant="ghost" @click="reset">{{ t('onboarding.doc.fit') }}</NButton>
    </div>
  </div>
</template>
