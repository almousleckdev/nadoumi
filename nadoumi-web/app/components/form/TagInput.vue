<script setup lang="ts">
import { computed, ref } from 'vue'

/**
 * Free-text multi-entry: the student types a value and presses Enter or comma to add it as a chip, and removes a
 * chip with its × (or Backspace on an empty box). Duplicates (ignoring case) and blanks are dropped. Nothing is
 * preset; `min` and `max` only bound how many entries there are.
 */
const props = withDefaults(defineProps<{
  id: string
  label: string
  placeholder?: string
  hint?: string
  error?: string
  required?: boolean
  min?: number
  max: number
  maxLength?: number
}>(), { placeholder: undefined, hint: undefined, error: undefined, min: 0, maxLength: 80 })
const model = defineModel<string[]>({ required: true })
const { t } = useI18n()

const draft = ref('')
const atLimit = computed(() => model.value.length >= props.max)
const remainingToMin = computed(() => Math.max(props.min - model.value.length, 0))

function add(raw: string) {
  const value = raw.trim().replace(/\s+/g, ' ').slice(0, props.maxLength)
  if (!value || atLimit.value) return
  if (model.value.some(v => v.toLowerCase() === value.toLowerCase())) return
  model.value = [...model.value, value]
}

function commit() {
  add(draft.value)
  draft.value = ''
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' || e.key === ',') {
    e.preventDefault()
    commit()
  }
  else if (e.key === 'Backspace' && !draft.value && model.value.length) {
    model.value = model.value.slice(0, -1)
  }
}

/** Pasting "Beijing, Shanghai, Chengdu" adds all three. */
function onPaste(e: ClipboardEvent) {
  const text = e.clipboardData?.getData('text') ?? ''
  if (!/[,\n;]/.test(text)) return
  e.preventDefault()
  text.split(/[,\n;]/).forEach(add)
}

function remove(value: string) {
  model.value = model.value.filter(v => v !== value)
}
</script>

<template>
  <div class="grid gap-2 sm:col-span-2" :aria-describedby="error ? `${id}-error` : undefined">
    <label :for="id" class="text-sm font-medium text-slate-700">
      {{ label }}<span v-if="required" class="text-brand-700" aria-hidden="true"> *</span>
    </label>
    <p v-if="hint" class="text-xs text-slate-500">{{ hint }}</p>

    <div
      class="flex flex-wrap items-center gap-2 rounded-xl border bg-white p-2 transition-colors focus-within:border-brand-500 focus-within:ring-2 focus-within:ring-brand-100"
      :class="error ? 'border-red-400' : 'border-slate-200'"
    >
      <TransitionGroup name="tag" tag="ul" class="contents">
        <li
          v-for="value in model"
          :key="value"
          class="inline-flex items-center gap-1 rounded-full bg-brand-50 py-1 ps-3 pe-1 text-sm font-medium text-brand-800"
          data-test="tag"
        >
          {{ value }}
          <button
            type="button"
            class="grid h-5 w-5 place-items-center rounded-full text-brand-700 hover:bg-brand-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-600"
            :aria-label="t('form.removeTag', { value })"
            @click="remove(value)"
          >
            ×
          </button>
        </li>
      </TransitionGroup>
      <input
        :id="id"
        v-model="draft"
        type="text"
        :maxlength="maxLength"
        :disabled="atLimit"
        :placeholder="model.length ? '' : placeholder"
        class="min-w-[10rem] flex-1 bg-transparent px-2 py-1.5 text-sm text-slate-900 placeholder:text-slate-500 focus:outline-none disabled:cursor-not-allowed"
        autocomplete="off"
        data-test="tag-input"
        @keydown="onKeydown"
        @paste="onPaste"
        @blur="commit"
      >
    </div>

    <div class="flex items-center justify-between gap-3 text-xs">
      <p class="text-slate-500" data-test="tag-count">
        {{ remainingToMin > 0 ? t('form.tagsNeedMore', { n: remainingToMin }) : t('form.tagsCount', { n: model.length, max }) }}
      </p>
      <button
        v-if="draft.trim()"
        type="button"
        class="font-semibold text-brand-700 hover:underline"
        data-test="tag-add"
        @click="commit"
      >
        {{ t('common.add') }}
      </button>
    </div>
    <p v-if="error" :id="`${id}-error`" role="alert" class="text-xs text-red-600">{{ error }}</p>
  </div>
</template>

<style scoped>
.tag-enter-active, .tag-leave-active { transition: transform 0.18s ease, opacity 0.18s ease; }
.tag-enter-from, .tag-leave-to { transform: scale(0.85); opacity: 0; }
@media (prefers-reduced-motion: reduce) {
  .tag-enter-active, .tag-leave-active { transition: none; }
}
</style>
