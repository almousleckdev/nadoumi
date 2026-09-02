<script setup lang="ts">
defineProps<{ busy: boolean }>()
const emit = defineEmits<{ submit: [payload: { current: string; next: string }] }>()
const { t } = useI18n()

const f = reactive({ current: '', next: '', confirm: '' })
const submitted = ref(false)

const result = computed(() => passwordChecks(f.next, { current: f.current, confirm: f.confirm }))
const canSubmit = computed(() => Boolean(f.current) && result.value.firstError === null)
const error = ref('')

function submit() {
  submitted.value = true
  error.value = ''
  if (!f.current) { error.value = t('validation.required'); return }
  if (result.value.firstError) { error.value = t(result.value.firstError); return }
  emit('submit', { current: f.current, next: f.next })
}
</script>

<template>
  <form class="grid max-w-sm gap-4" @submit.prevent="submit">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
    <PasswordField
      id="pw-current"
      v-model="f.current"
      :label="t('dashboard.pwCurrent')"
      autocomplete="current-password"
    />
    <PasswordField
      id="pw-new"
      v-model="f.next"
      :label="t('dashboard.pwNew')"
      :valid="result.strong"
    />
    <PasswordRequirements :value="f.next" :confirm="f.confirm" />
    <PasswordField
      id="pw-confirm"
      v-model="f.confirm"
      :label="t('dashboard.pwConfirm')"
      :error="submitted && f.confirm && f.next !== f.confirm ? t('validation.password.mismatch') : ''"
    />
    <NButton type="submit" :loading="busy" :disabled="!canSubmit">{{ t('dashboard.changePassword') }}</NButton>
  </form>
</template>
