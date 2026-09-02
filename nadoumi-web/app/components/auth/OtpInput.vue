<script setup lang="ts">
const props = withDefaults(defineProps<{ modelValue: string; length?: number; disabled?: boolean }>(), {
  length: 6,
  disabled: false,
})
const emit = defineEmits<{ 'update:modelValue': [value: string]; complete: [value: string] }>()

const boxes = ref<string[]>(Array.from({ length: props.length }, (_, i) => props.modelValue[i] ?? ''))
const inputs = ref<HTMLInputElement[]>([])

watch(() => props.modelValue, (v: string) => {
  boxes.value = Array.from({ length: props.length }, (_, i) => v[i] ?? '')
})

function registerInput(el: Element | null, index: number) {
  if (el) inputs.value[index] = el as HTMLInputElement
}

function emitValue() {
  const value = boxes.value.join('')
  emit('update:modelValue', value)
  if (value.length === props.length) emit('complete', value)
}

function onInput(index: number, event: Event) {
  const digit = (event.target as HTMLInputElement).value.replace(/\D/g, '').slice(-1)
  boxes.value[index] = digit
  if (digit && index < props.length - 1) inputs.value[index + 1]?.focus()
  emitValue()
}

function onKeydown(index: number, event: KeyboardEvent) {
  if (event.key === 'Backspace' && !boxes.value[index] && index > 0) {
    inputs.value[index - 1]?.focus()
  }
}

function onPaste(event: ClipboardEvent) {
  const text = (event.clipboardData?.getData('text') ?? '').replace(/\D/g, '').slice(0, props.length)
  if (!text) return
  event.preventDefault()
  boxes.value = Array.from({ length: props.length }, (_, i) => text[i] ?? '')
  inputs.value[Math.min(text.length, props.length - 1)]?.focus()
  emitValue()
}
</script>

<template>
  <!-- always left-to-right: a numeric code is not mirrored in RTL -->
  <div class="flex gap-2" dir="ltr">
    <input
      v-for="(box, i) in boxes"
      :key="i"
      :ref="(el: unknown) => registerInput(el as Element | null, i)"
      :value="box"
      :disabled="disabled"
      inputmode="numeric"
      autocomplete="one-time-code"
      maxlength="1"
      :aria-label="`Digit ${i + 1}`"
      class="h-12 w-10 rounded-md border border-slate-200 text-center text-lg font-semibold text-slate-900 focus-visible:border-brand-500 focus-visible:outline-none disabled:bg-slate-50"
      @input="onInput(i, $event)"
      @keydown="onKeydown(i, $event)"
      @paste="onPaste"
    >
  </div>
</template>
