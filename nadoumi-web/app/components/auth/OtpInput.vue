<script setup lang="ts">
const props = withDefaults(defineProps<{
  modelValue: string
  length?: number
  disabled?: boolean
  /** verifying: dim + lock the boxes while the code is being checked */
  busy?: boolean
  invalid?: boolean
}>(), { length: 6, disabled: false, busy: false, invalid: false })
const emit = defineEmits<{ 'update:modelValue': [value: string]; complete: [value: string] }>()

const { t } = useI18n()
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
  if (event.key === 'ArrowLeft' && index > 0) inputs.value[index - 1]?.focus()
  if (event.key === 'ArrowRight' && index < props.length - 1) inputs.value[index + 1]?.focus()
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
  <div
    role="group"
    :aria-label="t('auth.otp.groupLabel', { n: length })"
    :aria-busy="busy"
    class="flex gap-2 sm:gap-2.5"
    dir="ltr"
  >
    <input
      v-for="(box, i) in boxes"
      :key="i"
      :ref="(el: unknown) => registerInput(el as Element | null, i)"
      :value="box"
      :disabled="disabled || busy"
      inputmode="numeric"
      autocomplete="one-time-code"
      maxlength="1"
      :aria-label="t('auth.otp.digitLabel', { i: i + 1, n: length })"
      class="h-11 w-11 rounded-lg border text-center text-lg font-semibold text-slate-900 outline-none transition-[border-color,background-color,box-shadow] duration-150 focus-visible:border-brand-500 focus-visible:ring-2 focus-visible:ring-brand-500/30 disabled:opacity-60 motion-reduce:transition-none"
      :class="[
        invalid ? 'border-red-400' : box ? 'border-brand-300 bg-brand-50/60' : 'border-slate-200',
      ]"
      @input="onInput(i, $event)"
      @keydown="onKeydown(i, $event)"
      @paste="onPaste"
    >
  </div>
</template>
