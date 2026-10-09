<template>
  <textarea
    ref="el"
    class="agt"
    rows="1"
    :value="modelValue"
    :placeholder="placeholder"
    :maxlength="maxlength"
    :readonly="readonly"
    :aria-label="label"
    @input="onInput"
    @keydown.enter.prevent="emit('enter')"
    @blur="emit('blur')"
  />
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

/** A borderless single-flow field that grows with its text (the story title and subtitle). */
const props = defineProps<{
  modelValue: string
  placeholder: string
  maxlength: number
  label: string
  readonly?: boolean
}>()
const emit = defineEmits<{ 'update:modelValue': [value: string], enter: [], blur: [] }>()

const el = ref<HTMLTextAreaElement>()

function fit() {
  const field = el.value
  if (!field) return
  field.style.height = 'auto'
  field.style.height = `${field.scrollHeight}px`
}

function onInput(event: Event) {
  // Enter is reserved for "move on", so a pasted line break becomes a space
  emit('update:modelValue', (event.target as HTMLTextAreaElement).value.replace(/\s*\n\s*/g, ' '))
  void nextTick(fit)
}

watch(() => props.modelValue, () => void nextTick(fit))
onMounted(() => {
  fit()
  window.addEventListener('resize', fit)
})
onBeforeUnmount(() => window.removeEventListener('resize', fit))

defineExpose({ focus: () => el.value?.focus() })
</script>

<style scoped>
.agt {
  display: block;
  width: 100%;
  padding: 0;
  margin: 0;
  border: 0;
  outline: 0;
  resize: none;
  overflow: hidden;
  background: transparent;
  color: inherit;
  font: inherit;
  letter-spacing: inherit;
  line-height: inherit;
}
.agt::placeholder {
  color: var(--ne-placeholder);
}
</style>
