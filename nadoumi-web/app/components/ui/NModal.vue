<script setup lang="ts">
const props = defineProps<{ modelValue: boolean; title?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const panel = ref<HTMLElement | null>(null)

function close() { emit('update:modelValue', false) }
function onKey(e: Event) { if ((e as KeyboardEvent).key === 'Escape') close() }

watch(() => props.modelValue, async (open: boolean) => {
  if (import.meta.client) {
    document[open ? 'addEventListener' : 'removeEventListener']('keydown', onKey)
    document.body.style.overflow = open ? 'hidden' : ''
    if (open) { await nextTick(); panel.value?.focus() }
  }
})
onBeforeUnmount(() => {
  if (import.meta.client) { document.removeEventListener('keydown', onKey); document.body.style.overflow = '' }
})
</script>

<template>
  <ClientOnly>
    <Teleport to="body">
      <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-slate-900/40" @click="close" />
        <div
          ref="panel"
          role="dialog"
          aria-modal="true"
          tabindex="-1"
          class="relative w-full max-w-md rounded-lg bg-white p-6 shadow-md outline-none"
        >
          <h2 v-if="title" class="mb-3 font-display text-lg font-semibold">{{ title }}</h2>
          <div><slot /></div>
          <div v-if="$slots.footer" class="mt-5 flex justify-end gap-2"><slot name="footer" /></div>
        </div>
      </div>
    </Teleport>
  </ClientOnly>
</template>
