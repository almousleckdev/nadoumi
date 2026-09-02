<script setup lang="ts">
defineProps<{ busy: boolean }>()
const emit = defineEmits<{ submit: [payload: { current: string; next: string }] }>()
const { t } = useI18n()

const f = reactive({ current: '', next: '', confirm: '' })
const error = ref('')

function submit() {
  error.value = ''
  if (!f.current) { error.value = t('validation.required'); return }
  const policyKey = passwordPolicyKey(f.next, f.current)
  if (policyKey) { error.value = t(policyKey); return }
  if (f.next !== f.confirm) { error.value = t('auth.passwordMismatch'); return }
  emit('submit', { current: f.current, next: f.next })
}
</script>

<template>
  <form class="grid max-w-sm gap-4" @submit.prevent="submit">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
    <NField :label="t('dashboard.pwCurrent')" for="pw-current" required>
      <NInput id="pw-current" v-model="f.current" type="password" autocomplete="current-password" :maxlength="32" />
    </NField>
    <NField :label="t('dashboard.pwNew')" for="pw-new" required>
      <NInput id="pw-new" v-model="f.next" type="password" autocomplete="new-password" :maxlength="32" />
    </NField>
    <NField :label="t('dashboard.pwConfirm')" for="pw-confirm" required>
      <NInput id="pw-confirm" v-model="f.confirm" type="password" autocomplete="new-password" :maxlength="32" />
    </NField>
    <NButton type="submit" :loading="busy">{{ t('dashboard.changePassword') }}</NButton>
  </form>
</template>
