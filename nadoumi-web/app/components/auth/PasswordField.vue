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
  /** No visible label/hint chrome — placeholder + lock icon only (matches the admin login). */
  plain?: boolean
}>(), { autocomplete: 'new-password', hint: undefined, error: undefined, valid: false, required: true, plain: false })
defineEmits<{ 'update:modelValue': [value: string] }>()

const revealed = ref(false)
</script>

<template>
  <NField v-if="!plain" :label="label" :for="id" :hint="hint" :error="error" :required="required">
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
        <PasswordRevealToggle v-model="revealed" />
      </template>
    </NInput>
  </NField>

  <NInput
    v-else
    :id="id"
    :model-value="modelValue"
    :type="revealed ? 'text' : 'password'"
    :autocomplete="autocomplete"
    :invalid="Boolean(error)"
    :placeholder="label"
    :aria-label="label"
    :maxlength="64"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <template #prefix>
      <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
        <rect x="4" y="10" width="16" height="11" rx="2" />
        <path d="M8 10V7a4 4 0 0 1 8 0v3" />
      </svg>
    </template>
    <template #suffix>
      <PasswordRevealToggle v-model="revealed" />
    </template>
  </NInput>
</template>
