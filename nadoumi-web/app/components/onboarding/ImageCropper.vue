<script setup lang="ts">
/**
 * Client-only image crop: zoom + rotate + pan a source image inside a fixed frame,
 * then emit the framed result as a data URL. No upload — the parent decides what to
 * do with the crop (today: preview only; REQUIRES BACKEND for persistence).
 *
 * Zoom is the actual natural-pixel-to-frame scale factor (not a multiplier on a
 * CSS-fitted size): the image is shown at that scale via a plain transform, so the
 * preview and the exported canvas always agree pixel-for-pixel.
 */
import { imageSize } from '~/utils/files'

const props = withDefaults(defineProps<{ src: string; aspect?: number; size?: number }>(), {
  aspect: 1,
  size: 320,
})
const emit = defineEmits<{ crop: [dataUrl: string] }>()
const { t } = useI18n()

const { zoom, rotation, offset, transform, onPointerDown, onPointerMove, onPointerUp, rotate: rotateFrame } = usePanZoom({ min: 0.01, max: 100 })

const frameW = computed(() => props.size)
const frameH = computed(() => Math.round(props.size / props.aspect))

const naturalWidth = ref(0)
const naturalHeight = ref(0)
const ready = ref(false)

/** Fills the frame with no letterboxing, like a standard avatar cropper — the sane starting point. */
const coverScale = computed(() => {
  if (!naturalWidth.value || !naturalHeight.value) return 1
  return Math.max(frameW.value / naturalWidth.value, frameH.value / naturalHeight.value)
})
const minZoom = computed(() => coverScale.value / 2)
const maxZoom = computed(() => coverScale.value * 4)

function resetToFit() {
  zoom.value = coverScale.value
  rotation.value = 0
  offset.x = 0
  offset.y = 0
}

watch(() => props.src, async (src: string) => {
  ready.value = false
  if (!src) return
  const { width, height } = await imageSize(src)
  naturalWidth.value = width
  naturalHeight.value = height
  resetToFit()
  ready.value = true
}, { immediate: true })

async function apply() {
  const img = new Image()
  img.src = props.src
  await img.decode().catch(() => {})
  const canvas = document.createElement('canvas')
  canvas.width = frameW.value
  canvas.height = frameH.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.fillStyle = '#fff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  ctx.translate(canvas.width / 2 + offset.x, canvas.height / 2 + offset.y)
  ctx.rotate((rotation.value * Math.PI) / 180)
  ctx.scale(zoom.value, zoom.value)
  ctx.drawImage(img, -img.width / 2, -img.height / 2)
  emit('crop', canvas.toDataURL('image/jpeg', 0.9))
}
</script>

<template>
  <div class="grid gap-3">
    <div
      class="relative mx-auto overflow-hidden rounded-lg border border-slate-200 bg-slate-100 touch-none"
      :style="{ width: `${frameW}px`, height: `${frameH}px` }"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointercancel="onPointerUp"
    >
      <img
        v-if="ready"
        :src="src"
        alt=""
        draggable="false"
        class="absolute left-1/2 top-1/2 max-w-none -translate-x-1/2 -translate-y-1/2 select-none motion-safe:transition-transform motion-safe:duration-75"
        :style="{ transform }"
      >
    </div>

    <label class="flex items-center gap-3 text-xs text-slate-600">
      {{ t('onboarding.crop.zoom') }}
      <input v-model.number="zoom" type="range" :min="minZoom" :max="maxZoom" :step="(maxZoom - minZoom) / 100" class="flex-1 accent-brand-600">
    </label>

    <div class="flex flex-wrap gap-2">
      <NButton size="sm" variant="secondary" @click="rotateFrame">{{ t('onboarding.crop.rotate') }}</NButton>
      <NButton size="sm" variant="ghost" @click="resetToFit">{{ t('onboarding.crop.reset') }}</NButton>
      <NButton size="sm" class="ms-auto" @click="apply">{{ t('onboarding.crop.apply') }}</NButton>
    </div>
  </div>
</template>
