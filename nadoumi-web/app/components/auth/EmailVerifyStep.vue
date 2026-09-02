<script setup lang="ts">
import type { OtpPurpose } from '~/composables/useOtp'

const props = withDefaults(
  defineProps<{ email: string; purpose: OtpPurpose; emailLocked?: boolean; canVerify?: boolean }>(),
  { canVerify: true },
)
const emit = defineEmits<{ 'update:email': [value: string]; verified: [ticket: string] }>()

const { t } = useI18n()
const { request, verify, cooldown, busy } = useOtp()

const stage = ref<'email' | 'otp'>('email')
const otp = ref('')
const error = ref('')

async function sendCode() {
  error.value = ''
  try {
    await request(props.email, props.purpose)
    stage.value = 'otp'
  }
  catch (e) {
    error.value = authErrorMessage(e, t)
  }
}

async function submitCode(code: string) {
  error.value = ''
  try {
    emit('verified', await verify(props.email, props.purpose, code))
  }
  catch (e) {
    error.value = authErrorMessage(e, t)
  }
}
</script>

<template>
  <div class="grid gap-4">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

    <template v-if="stage === 'email'">
      <NField :label="t('auth.email')" for="otp-email" required>
        <NInput
          id="otp-email"
          :model-value="email"
          type="email"
          autocomplete="email"
          :disabled="emailLocked"
          @update:model-value="emit('update:email', $event)"
        />
      </NField>
      <NButton :loading="busy" :disabled="!canVerify || !email" @click="sendCode">{{ t('auth.otp.verify') }}</NButton>
    </template>

    <template v-else>
      <p class="text-sm text-slate-600">{{ t('auth.otp.sent', { email }) }}</p>
      <OtpInput v-model="otp" @complete="submitCode" />
      <button
        type="button"
        class="text-start text-sm font-medium text-brand-700 hover:underline disabled:text-slate-400 disabled:no-underline"
        :disabled="cooldown > 0"
        @click="sendCode"
      >
        {{ cooldown > 0 ? t('auth.otp.resendIn', { n: cooldown }) : t('auth.otp.resend') }}
      </button>
    </template>
  </div>
</template>
