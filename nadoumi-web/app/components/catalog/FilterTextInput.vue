<script setup lang="ts">
/** A free-text catalog filter. Emits the trimmed value (capitalised when `uppercase`) on change; '' means cleared. */
const props = defineProps<{
  value?: string | string[] | null
  placeholder: string
  maxlength?: number
  uppercase?: boolean
}>()
const emit = defineEmits<{ commit: [value: string] }>()

const shown = computed(() => (Array.isArray(props.value) ? props.value.join(', ') : props.value ?? ''))

function onChange(event: Event) {
  const text = (event.target as HTMLInputElement).value.trim()
  emit('commit', props.uppercase ? text.toUpperCase() : text)
}
</script>

<template>
  <input
    :value="shown"
    type="text"
    :maxlength="maxlength"
    :placeholder="placeholder"
    class="rounded-md border border-slate-300 px-2.5 py-2 text-sm outline-none focus-visible:border-brand-500"
    :class="{ uppercase }"
    @change="onChange"
  >
</template>
