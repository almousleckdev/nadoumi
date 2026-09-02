<script setup lang="ts">
const props = withDefaults(defineProps<{
  id: string
  modelValue: string
  type?: string
  invalid?: boolean
  valid?: boolean
  autocomplete?: string
  placeholder?: string
  maxlength?: number
  disabled?: boolean
}>(), { type: 'text', autocomplete: undefined, placeholder: undefined, maxlength: undefined })
defineEmits<{ 'update:modelValue': [value: string] }>()
const slots = useSlots()

const describedBy = `${props.id}-hint ${props.id}-error`
const borderClass = computed(() => {
  if (props.invalid) return 'border-red-400 focus-visible:border-red-500'
  if (props.valid) return 'border-emerald-400 focus-visible:border-emerald-500'
  return 'border-slate-200 focus-visible:border-brand-500'
})
</script>

<template>
  <div class="relative">
    <input
      :id="id"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :maxlength="maxlength"
      :disabled="disabled"
      :autocomplete="autocomplete"
      :aria-invalid="invalid ? 'true' : undefined"
      :aria-describedby="describedBy"
      class="w-full rounded-md border px-3 py-2 text-base outline-none transition-colors disabled:bg-slate-50 disabled:text-slate-400"
      :class="[borderClass, slots.suffix ? 'pe-11' : '']"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    >
    <div v-if="slots.suffix" class="absolute inset-y-0 end-0 flex items-center pe-2">
      <slot name="suffix" />
    </div>
  </div>
</template>
