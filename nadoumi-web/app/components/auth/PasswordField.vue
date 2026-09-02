<script setup lang="ts">
withDefaults(defineProps<{
  id: string
  modelValue: string
  label: string
  autocomplete?: string
  hint?: string
  error?: string
  valid?: boolean
  required?: boolean
}>(), { autocomplete: 'new-password', hint: undefined, error: undefined, valid: false, required: true })
defineEmits<{ 'update:modelValue': [value: string] }>()

const { t } = useI18n()
const revealed = ref(false)
</script>

<template>
  <NField :label="label" :for="id" :hint="hint" :error="error" :required="required">
    <NInput
      :id="id"
      :model-value="modelValue"
      :type="revealed ? 'text' : 'password'"
      :autocomplete="autocomplete"
      :invalid="Boolean(error)"
      :valid="valid && !error"
      :maxlength="64"
      @update:model-value="$emit('update:modelValue', $event)"
    >
      <template #suffix>
        <button
          type="button"
          class="rounded p-1 text-slate-400 transition-colors hover:text-slate-600 focus-visible:text-brand-600 focus-visible:outline-brand-500"
          :aria-label="revealed ? t('auth.hidePassword') : t('auth.showPassword')"
          :aria-pressed="revealed"
          @click="revealed = !revealed"
        >
          <svg v-if="!revealed" viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
            <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z" />
            <circle cx="12" cy="12" r="3" />
          </svg>
          <svg v-else viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
            <path d="M3 3l18 18M10.6 10.6a3 3 0 0 0 4.2 4.2M9.9 4.2A10.9 10.9 0 0 1 12 4c6.4 0 10 7 10 7a17.8 17.8 0 0 1-3.4 4M6.1 6.1A17.8 17.8 0 0 0 2 12s3.6 7 10 7a10.7 10.7 0 0 0 4.3-.9" />
          </svg>
        </button>
      </template>
    </NInput>
  </NField>
</template>
