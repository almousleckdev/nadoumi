<script setup lang="ts">
defineProps<{ label: string }>()

const track = ref<HTMLElement | null>(null)
const atStart = ref(true)
const atEnd = ref(false)

function measure() {
  const el = track.value
  if (!el) return
  atStart.value = el.scrollLeft <= 1
  atEnd.value = el.scrollLeft + el.clientWidth >= el.scrollWidth - 1
}

function step(dir: 1 | -1) {
  const el = track.value
  if (!el) return
  el.scrollBy({ left: dir * Math.round(el.clientWidth * 0.85), behavior: motionOk() ? 'smooth' : 'auto' })
}
function scrollPrev() { step(-1) }
function scrollNext() { step(1) }

function motionOk() {
  return !(import.meta.client && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches)
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'ArrowRight') { e.preventDefault(); scrollNext() }
  else if (e.key === 'ArrowLeft') { e.preventDefault(); scrollPrev() }
}

onMounted(() => {
  measure()
  const el = track.value
  el?.addEventListener('scroll', measure, { passive: true })
  window.addEventListener('resize', measure)
})
onBeforeUnmount(() => {
  track.value?.removeEventListener('scroll', measure)
  if (import.meta.client) window.removeEventListener('resize', measure)
})

defineExpose({ scrollPrev, scrollNext, atStart, atEnd })
</script>

<template>
  <div
    ref="track"
    role="group"
    :aria-label="label"
    tabindex="0"
    class="nad-carousel -mx-1 flex snap-x snap-mandatory gap-4 overflow-x-auto scroll-px-1 px-1 pb-2 focus-visible:outline-2 focus-visible:outline-brand-500"
    @keydown="onKey"
  >
    <slot />
  </div>
</template>

<style scoped>
.nad-carousel {
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.nad-carousel::-webkit-scrollbar {
  display: none;
}
.nad-carousel > :deep(*) {
  scroll-snap-align: start;
  flex: 0 0 auto;
}
</style>
