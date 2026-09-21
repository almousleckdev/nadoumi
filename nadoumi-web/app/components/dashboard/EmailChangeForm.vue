<script setup lang="ts">
/**
 * Self-service sign-in email change: enter the new address, prove it with an
 * emailed code, confirm with the current password (defense against a hijacked
 * session, since email is the sign-in identity). Mirrors ApplicantEmailVerify's
 * two-stage shape but the second stage also applies the change, it does not just
 * return a verified value.
 */
const props = defineProps<{ currentEmail: string }>()
const emit = defineEmits<{ changed: [newEmail: string] }>()
const { t } = useI18n()
const { requestCode, confirm, cooldown, busy } = useEmailChange()

const stage = ref<'idle' | 'code'>('idle')
const newEmail = ref('')
const otp = ref('')
const currentPassword = ref('')
const error = ref('')

const canSendCode = computed(() => {
  const email = newEmail.value.trim()
  return email.length > 0 && email !== props.currentEmail && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
})
const canConfirm = computed(() => otp.value.length === 6 && currentPassword.value.length > 0)

async function sendCode() {
  error.value = ''
  try {
    await requestCode(newEmail.value.trim())
    otp.value = ''
    stage.value = 'code'
  }
  catch (e) {
    error.value = authErrorMessage(e, t)
  }
}

async function submit() {
  error.value = ''
  try {
    await confirm(newEmail.value.trim(), otp.value, currentPassword.value)
    emit('changed', newEmail.value.trim())
    reset()
  }
  catch (e) {
    otp.value = ''
    error.value = authErrorMessage(e, t)
  }
}

function reset() {
  stage.value = 'idle'
  newEmail.value = ''
  otp.value = ''
  currentPassword.value = ''
  error.value = ''
}
</script>

<template>
  <div class="grid max-w-sm gap-4">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

    <template v-if="stage === 'idle'">
      <NField :label="t('dashboard.account.security.newEmail')" for="email-new">
        <NInput id="email-new" v-model="newEmail" type="email" autocomplete="email" @keydown.enter.prevent="canSendCode && sendCode()" />
      </NField>
      <div>
        <NButton size="sm" :loading="busy" :disabled="!canSendCode" @click="sendCode">
          {{ t('dashboard.account.security.sendCode') }}
        </NButton>
      </div>
    </template>

    <template v-else>
      <p class="text-sm text-slate-600">{{ t('auth.otp.sent', { email: newEmail }) }}</p>
      <OtpInput v-model="otp" :busy="busy" :invalid="Boolean(error)" />
      <button
        type="button"
        class="-mt-1 text-start text-sm font-medium text-brand-700 hover:underline disabled:text-slate-400 disabled:no-underline"
        :disabled="cooldown > 0 || busy"
        @click="sendCode"
      >
        {{ cooldown > 0 ? t('auth.otp.resendIn', { n: cooldown }) : t('auth.otp.resend') }}
      </button>

      <PasswordField
        id="email-change-current-password"
        v-model="currentPassword"
        :label="t('dashboard.pwCurrent')"
        autocomplete="current-password"
      />

      <div class="flex gap-2">
        <NButton size="sm" :loading="busy" :disabled="!canConfirm" @click="submit">
          {{ t('dashboard.account.security.confirmEmail') }}
        </NButton>
        <NButton size="sm" variant="ghost" :disabled="busy" @click="reset">{{ t('common.cancel') }}</NButton>
      </div>
    </template>
  </div>
</template>
