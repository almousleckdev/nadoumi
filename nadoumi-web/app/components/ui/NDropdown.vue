<script setup lang="ts">
defineProps<{ label?: string }>()
const open = ref(false)
const root = ref<HTMLElement | null>(null)

function onDocClick(e: Event) {
  if (root.value && !root.value.contains(e.target as Node)) open.value = false
}
function onKey(e: Event) { if ((e as KeyboardEvent).key === 'Escape') open.value = false }
watch(open, (v: boolean) => {
  if (!import.meta.client) return
  document[v ? 'addEventListener' : 'removeEventListener']('click', onDocClick)
  document[v ? 'addEventListener' : 'removeEventListener']('keydown', onKey)
})
onBeforeUnmount(() => {
  if (!import.meta.client) return
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKey)
})
</script>

<template>
  <div ref="root" class="relative inline-block">
    <button
      type="button"
      aria-haspopup="menu"
      :aria-expanded="open ? 'true' : 'false'"
      class="inline-flex items-center gap-1 rounded-md px-2 py-1.5 hover:bg-slate-100"
      @click="open = !open"
    >
      <slot name="trigger">{{ label }}</slot>
    </button>
    <div
      v-if="open"
      role="menu"
      class="absolute end-0 z-40 mt-1 min-w-44 rounded-md border border-slate-200 bg-white py-1 shadow-md"
      @click="open = false"
    >
      <slot />
    </div>
  </div>
</template>
