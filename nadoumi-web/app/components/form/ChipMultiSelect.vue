<script setup lang="ts">
import type { Option } from '~/utils/countries'

/** Pick several values from a list; with `custom`, the student may also add one of their own. */
const props = defineProps<{
  id: string
  label: string
  options: Option[]
  max: number
  error?: string
  hint?: string
  required?: boolean
  customLabel?: string
}>()
const model = defineModel<string[]>({ required: true })
const { t } = useI18n()

const draft = ref('')
const atLimit = computed(() => model.value.length >= props.max)
const knownValues = computed(() => new Set(props.options.map(o => o.value)))
/** Values the student typed themselves, shown as chips alongside the list. */
const customValues = computed(() => model.value.filter(value => !knownValues.value.has(value)))
const chips = computed<Option[]>(() => [...props.options, ...customValues.value.map((value: string) => ({ value, label: value }))])

function toggle(value: string) {
  if (model.value.includes(value)) model.value = model.value.filter(v => v !== value)
  else if (!atLimit.value) model.value = [...model.value, value]
}

function addCustom() {
  const value = draft.value.trim()
  draft.value = ''
  if (value && !model.value.includes(value) && !atLimit.value) model.value = [...model.value, value]
}
</script>

<template>
  <fieldset class="grid gap-2 sm:col-span-2" :aria-describedby="error ? `${id}-error` : undefined">
    <legend class="text-sm font-medium text-slate-700">
      {{ label }}<span v-if="required" class="text-brand-700" aria-hidden="true"> *</span>
    </legend>
    <p v-if="hint" class="text-xs text-slate-500">{{ hint }}</p>
    <div class="flex flex-wrap gap-2">
      <button
        v-for="chip in chips"
        :key="chip.value"
        type="button"
        :aria-pressed="model.includes(chip.value)"
        :disabled="atLimit && !model.includes(chip.value)"
        class="rounded-full border px-3 py-1 text-sm transition-colors disabled:opacity-40"
        :class="model.includes(chip.value) ? 'border-brand-600 bg-brand-50 font-medium text-brand-800' : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
        @click="toggle(chip.value)"
      >
        {{ chip.label }}
      </button>
    </div>
    <div v-if="customLabel" class="flex gap-2">
      <NInput :id="`${id}-custom`" v-model="draft" :placeholder="customLabel" :maxlength="80" :disabled="atLimit" @keydown.enter.prevent="addCustom" />
      <NButton variant="secondary" size="sm" :disabled="!draft.trim() || atLimit" @click="addCustom">{{ t('common.add') }}</NButton>
    </div>
    <p class="text-xs text-slate-400">{{ t('form.chipsCount', { n: model.length, max }) }}</p>
    <p v-if="error" :id="`${id}-error`" role="alert" class="text-xs text-red-600">{{ error }}</p>
  </fieldset>
</template>
