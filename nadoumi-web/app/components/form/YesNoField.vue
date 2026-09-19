<script setup lang="ts">
/** A required yes/no question; `null` means it has not been answered yet. */
defineProps<{ id: string, label: string, error?: string }>()
const model = defineModel<boolean | null>({ required: true })
const { t } = useI18n()
const choices = [{ value: true, key: 'common.yes' }, { value: false, key: 'common.no' }] as const
</script>

<template>
  <fieldset class="grid gap-1.5" :aria-describedby="error ? `${id}-error` : undefined">
    <legend class="text-sm font-medium text-slate-700">{{ label }}<span class="text-brand-700" aria-hidden="true"> *</span></legend>
    <div class="flex gap-3">
      <label
        v-for="choice in choices"
        :key="String(choice.value)"
        class="flex min-w-24 cursor-pointer items-center justify-center rounded-md border px-4 py-2 text-sm font-medium transition-colors has-[:focus-visible]:outline has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-brand-500"
        :class="model === choice.value ? 'border-brand-600 bg-brand-50 text-brand-800' : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
      >
        <input v-model="model" type="radio" class="sr-only" :name="id" :value="choice.value">
        {{ t(choice.key) }}
      </label>
    </div>
    <p v-if="error" :id="`${id}-error`" role="alert" class="text-xs text-red-600">{{ error }}</p>
  </fieldset>
</template>
