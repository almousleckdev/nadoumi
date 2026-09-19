<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'

/**
 * Proves ownership of a new contact email: sends a code to it, takes the 6 digits,
 * and reports the updated applicant once the server has stored the verified address.
 */
const props = defineProps<{ applicantId: number, email: string }>()
const emit = defineEmits<{ verified: [applicant: ApplicantDto] }>()

const { t } = useI18n()
const { requestEmailCode, verifyEmail } = useApplicant()
const { seconds: cooldown, start: startCooldown } = useCooldown()

const stage = ref<'idle' | 'code'>('idle')
const otp = ref('')
const busy = ref(false)
const error = ref('')

async function sendCode() {
  error.value = ''
  busy.value = true
  try {
    const res = await requestEmailCode(props.applicantId, props.email)
    otp.value = ''
    stage.value = 'code'
    startCooldown(res.retryAfter > 0 ? res.retryAfter : 60)
  }
  catch (e) {
    error.value = authErrorMessage(e, t)
  }
  finally {
    busy.value = false
  }
}

async function submit(code: string) {
  error.value = ''
  busy.value = true
  try {
    emit('verified', await verifyEmail(props.applicantId, props.email, code))
  }
  catch (e) {
    otp.value = ''
    error.value = authErrorMessage(e, t)
  }
  finally {
    busy.value = false
  }
}

// a different address needs a fresh code
watch(() => props.email, () => {
  stage.value = 'idle'
  otp.value = ''
  error.value = ''
})
</script>

<template>
  <div class="grid gap-3 rounded-md border border-amber-200 bg-amber-50 px-4 py-3">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

    <template v-if="stage === 'idle'">
      <p class="text-sm text-amber-900">{{ t('profileForm.verifyBeforeSave') }}</p>
      <div>
        <NButton size="sm" :loading="busy" @click="sendCode">{{ t('profileForm.verifyEmail') }}</NButton>
      </div>
    </template>

    <template v-else>
      <p class="text-sm text-slate-700">{{ t('auth.otp.sent', { email }) }}</p>
      <OtpInput v-model="otp" :busy="busy" :invalid="Boolean(error)" @complete="submit" />
      <button
        type="button"
        class="text-start text-sm font-medium text-brand-700 hover:underline disabled:text-slate-400 disabled:no-underline"
        :disabled="cooldown > 0 || busy"
        @click="sendCode"
      >
        {{ cooldown > 0 ? t('auth.otp.resendIn', { n: cooldown }) : t('auth.otp.resend') }}
      </button>
    </template>
  </div>
</template>
