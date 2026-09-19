<script setup lang="ts">
import type { Option } from '~/utils/countries'

/**
 * A searchable single-choice field for long option lists (countries, languages) —
 * type to filter instead of scrolling a native <select>. Emits the chosen value,
 * same contract as FormSelectField.
 */
const props = defineProps<{
  id: string
  label: string
  options: Option[]
  error?: string
  required?: boolean
  placeholder?: string
}>()
const model = defineModel<string>({ required: true })
const { t } = useI18n()

const query = ref('')
const open = ref(false)
const activeIndex = ref(-1)

const selectedLabel = computed(() => props.options.find(o => o.value === model.value)?.label ?? '')
const displayValue = computed(() => (open.value ? query.value : selectedLabel.value))

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return props.options
  return props.options.filter(o => o.label.toLowerCase().includes(q))
})

const listboxId = `${props.id}-listbox`
const optionId = (index: number) => `${props.id}-option-${index}`
const activeDescendant = computed(() => (open.value && activeIndex.value >= 0 ? optionId(activeIndex.value) : undefined))

function openList() {
  open.value = true
  query.value = ''
  activeIndex.value = filtered.value.findIndex((o: Option) => o.value === model.value)
}

function onInput(event: Event) {
  query.value = (event.target as HTMLInputElement).value
  open.value = true
  activeIndex.value = filtered.value.length ? 0 : -1
}

function choose(option: Option) {
  model.value = option.value
  query.value = ''
  open.value = false
}

function close() {
  open.value = false
  query.value = ''
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    if (!open.value) { openList(); return }
    activeIndex.value = Math.min(activeIndex.value + 1, filtered.value.length - 1)
  }
  else if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeIndex.value = Math.max(activeIndex.value - 1, 0)
  }
  else if (event.key === 'Enter') {
    if (!open.value) return
    event.preventDefault()
    const chosen = filtered.value[activeIndex.value]
    if (chosen) choose(chosen)
  }
  else if (event.key === 'Escape') {
    if (open.value) { event.preventDefault(); close() }
  }
}
</script>

<template>
  <NField :label="label" :for="id" :error="error" :required="required">
    <div class="relative">
      <input
        :id="id"
        type="text"
        role="combobox"
        :value="displayValue"
        :placeholder="placeholder ?? t('form.search')"
        autocomplete="off"
        aria-autocomplete="list"
        :aria-expanded="open"
        :aria-controls="listboxId"
        :aria-activedescendant="activeDescendant"
        :aria-invalid="!!error"
        class="w-full rounded-md border px-3 py-2 text-base outline-none transition-colors"
        :class="error ? 'border-red-400 focus-visible:border-red-500' : 'border-slate-200 focus-visible:border-brand-500'"
        @focus="openList"
        @input="onInput"
        @keydown="onKeydown"
        @blur="close"
      >
      <ul
        v-if="open"
        :id="listboxId"
        role="listbox"
        class="absolute z-20 mt-1 max-h-60 w-full overflow-y-auto rounded-md border border-slate-200 bg-white py-1 shadow-lg"
      >
        <li v-if="!filtered.length" class="px-3 py-2 text-sm text-slate-400">{{ t('form.noMatches') }}</li>
        <li
          v-for="(o, i) in filtered"
          :id="optionId(i)"
          :key="o.value"
          role="option"
          :aria-selected="o.value === model"
          class="cursor-pointer px-3 py-2 text-sm"
          :class="i === activeIndex ? 'bg-brand-50 text-brand-800' : 'text-slate-700 hover:bg-slate-50'"
          @mousedown.prevent="choose(o)"
        >
          {{ o.label }}
        </li>
      </ul>
    </div>
  </NField>
</template>
