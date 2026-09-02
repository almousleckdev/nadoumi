<script setup lang="ts">
/**
 * Client-only image crop: zoom + rotate + pan a source image inside a fixed frame,
 * then emit the framed result as a data URL. No upload — the parent decides what to
 * do with the crop (today: preview only; REQUIRES BACKEND for persistence).
 */
const props = withDefaults(defineProps<{ src: string; aspect?: number; size?: number }>(), {
  aspect: 1,
  size: 320,
})
const emit = defineEmits<{ crop: [dataUrl: string] }>()
const { t } = useI18n()

const zoom = ref(1)
const rotation = ref(0)
const offset = reactive({ x: 0, y: 0 })
const dragging = ref(false)
let start = { x: 0, y: 0 }

const frameW = computed(() => props.size)
const frameH = computed(() => Math.round(props.size / props.aspect))

const transform = computed(() =>
  `translate(${offset.x}px, ${offset.y}px) rotate(${rotation.value}deg) scale(${zoom.value})`)

function onPointerDown(e: PointerEvent) {
  dragging.value = true
  start = { x: e.clientX - offset.x, y: e.clientY - offset.y }
  ;(e.target as HTMLElement).setPointerCapture(e.pointerId)
}
function onPointerMove(e: PointerEvent) {
  if (!dragging.value) return
  offset.x = e.clientX - start.x
  offset.y = e.clientY - start.y
}
function onPointerUp() {
  dragging.value = false
}
function rotate() {
  rotation.value = (rotation.value + 90) % 360
}
function reset() {
  zoom.value = 1
  rotation.value = 0
  offset.x = 0
  offset.y = 0
}

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
        :src="src"
        alt=""
        draggable="false"
        class="absolute left-1/2 top-1/2 max-w-none -translate-x-1/2 -translate-y-1/2 select-none motion-safe:transition-transform motion-safe:duration-75"
        :style="{ transform }"
      >
    </div>

    <label class="flex items-center gap-3 text-xs text-slate-600">
      {{ t('onboarding.crop.zoom') }}
      <input v-model.number="zoom" type="range" min="1" max="3" step="0.05" class="flex-1 accent-brand-600">
    </label>

    <div class="flex flex-wrap gap-2">
      <NButton size="sm" variant="secondary" @click="rotate">{{ t('onboarding.crop.rotate') }}</NButton>
      <NButton size="sm" variant="ghost" @click="reset">{{ t('onboarding.crop.reset') }}</NButton>
      <NButton size="sm" class="ms-auto" @click="apply">{{ t('onboarding.crop.apply') }}</NButton>
    </div>
  </div>
</template>
