<script setup lang="ts">
import { PAD_HEIGHT, PAD_WIDTH, PEN_WIDTH } from '~/constants/signature'

/** A small drawing surface for a handwritten signature (mouse, pen or finger). Exposes the drawing as a PNG. */
const emit = defineEmits<{ change: [hasInk: boolean] }>()
const { t } = useI18n()

const canvas = ref<HTMLCanvasElement | null>(null)
let drawing = false
let inked = false

function context(): CanvasRenderingContext2D | null {
  return canvas.value?.getContext('2d') ?? null
}

function setup() {
  const el = canvas.value
  const ctx = context()
  if (!el || !ctx) return
  const ratio = Math.min(window.devicePixelRatio || 1, 3)
  el.width = PAD_WIDTH * ratio
  el.height = PAD_HEIGHT * ratio
  ctx.scale(ratio, ratio)
  ctx.lineWidth = PEN_WIDTH
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.strokeStyle = '#0f172a'
}

function point(event: PointerEvent) {
  const rect = canvas.value!.getBoundingClientRect()
  return { x: (event.clientX - rect.left) * (PAD_WIDTH / rect.width), y: (event.clientY - rect.top) * (PAD_HEIGHT / rect.height) }
}

function start(event: PointerEvent) {
  const ctx = context()
  if (!ctx) return
  canvas.value?.setPointerCapture?.(event.pointerId)
  drawing = true
  const { x, y } = point(event)
  ctx.beginPath()
  ctx.moveTo(x, y)
  ctx.lineTo(x + 0.01, y) // a tap leaves a dot
  ctx.stroke()
  markInk()
}

function move(event: PointerEvent) {
  const ctx = context()
  if (!drawing || !ctx) return
  const { x, y } = point(event)
  ctx.lineTo(x, y)
  ctx.stroke()
  markInk()
}

function stop() {
  drawing = false
}

function markInk() {
  if (inked) return
  inked = true
  emit('change', true)
}

function clear() {
  const el = canvas.value
  const ctx = context()
  if (!el || !ctx) return
  ctx.save()
  ctx.setTransform(1, 0, 0, 1, 0, 0)
  ctx.clearRect(0, 0, el.width, el.height)
  ctx.restore()
  inked = false
  emit('change', false)
}

/** The drawing as a PNG, or null when nothing was drawn. */
function toBlob(): Promise<Blob | null> {
  if (!inked || !canvas.value) return Promise.resolve(null)
  return new Promise(resolve => canvas.value!.toBlob((blob: Blob | null) => resolve(blob), 'image/png'))
}

defineExpose({ toBlob, clear })
onMounted(setup)
</script>

<template>
  <div class="grid gap-2">
    <canvas
      ref="canvas"
      class="w-full touch-none rounded-lg border border-slate-300 bg-white"
      :style="{ aspectRatio: `${PAD_WIDTH} / ${PAD_HEIGHT}` }"
      :aria-label="t('signature.padLabel')"
      role="img"
      data-test="pad"
      @pointerdown.prevent="start"
      @pointermove.prevent="move"
      @pointerup="stop"
      @pointercancel="stop"
      @pointerleave="stop"
    />
    <div>
      <NButton variant="ghost" size="sm" data-test="clear" @click="clear">{{ t('signature.clear') }}</NButton>
    </div>
  </div>
</template>
