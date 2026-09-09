<script setup lang="ts">
import type { OtpPurpose } from '~/composables/useOtp'

const props = withDefaults(
  defineProps<{ email: string; purpose: OtpPurpose; emailLocked?: boolean; canVerify?: boolean }>(),
  { canVerify: true },
)
const emit = defineEmits<{
  'update:email': [value: string]
  verified: [ticket: string]
  sent: []
  edit: []
}>()

const { t } = useI18n()
const { request, verify, cooldown, busy } = useOtp()

const MAX_ATTEMPTS = 5
const stage = ref<'email' | 'otp' | 'done'>('email')
const otp = ref('')
const error = ref('')
const notice = ref('')
const verifying = ref(false)
const attemptsLeft = ref(MAX_ATTEMPTS)

// Captcha: the backend requires it on every /email-otp call when the login
// captcha is enabled (it is in production). One instance, kept mounted so its
// uuid is stable; shown on the email step and whenever a resend is possible.
const captchaRef = ref<{ enabled: boolean; reload: () => void } | null>(null)
const captchaCode = ref('')
const captchaUuid = ref('')
const showCaptcha = computed(
  () => stage.value === 'email' || (stage.value === 'otp' && cooldown.value === 0),
)

async function sendCode() {
  error.value = ''
  notice.value = ''
  if (captchaRef.value?.enabled && !captchaCode.value.trim()) {
    error.value = t('errors.captcha')
    return
  }
  const wasFirstRequest = stage.value === 'email'
  try {
    const { throttled } = await request(props.email, props.purpose, {
      code: captchaCode.value.trim(),
      uuid: captchaUuid.value,
    })
    otp.value = ''
    attemptsLeft.value = MAX_ATTEMPTS
    stage.value = 'otp'
    if (throttled) notice.value = t('auth.otp.throttledNote')
    else if (!wasFirstRequest) notice.value = t('auth.otp.resentNote')
    emit('sent')
  }
  catch (e) {
    error.value = authErrorMessage(e, t)
  }
  finally {
    // a captcha answer is single-use (consumed on success, expired on failure)
    captchaCode.value = ''
    captchaRef.value?.reload()
  }
}

async function submitCode(code: string) {
  error.value = ''
  notice.value = ''
  verifying.value = true
  try {
    const ticket = await verify(props.email, props.purpose, code)
    stage.value = 'done'
    // brief "verified" confirmation, then hand the ticket up
    setTimeout(() => emit('verified', ticket), 550)
  }
  catch (e) {
    attemptsLeft.value = Math.max(0, attemptsLeft.value - 1)
    otp.value = ''
    error.value = authErrorMessage(e, t)
  }
  finally {
    verifying.value = false
  }
}

function editEmail() {
  stage.value = 'email'
  otp.value = ''
  error.value = ''
  notice.value = ''
  attemptsLeft.value = MAX_ATTEMPTS
  emit('edit')
}
</script>

<template>
  <div class="grid gap-4">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

    <!-- one instance, kept mounted so its uuid is stable across the email/otp steps -->
    <AuthCaptcha
      v-show="showCaptcha"
      ref="captchaRef"
      v-model:code="captchaCode"
      v-model:uuid="captchaUuid"
    />

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
      <NButton :loading="busy" :disabled="!canVerify || !email" @click="sendCode">
        {{ t('auth.otp.verify') }}
      </NButton>
    </template>

    <template v-else>
      <div class="flex items-center justify-between gap-3 rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-sm">
        <span class="min-w-0 truncate">
          <span class="text-slate-500">{{ t('auth.otp.emailLabel') }}</span>
          <span class="ms-1 font-medium text-slate-800">{{ email }}</span>
        </span>
        <button
          type="button"
          class="shrink-0 font-medium text-brand-700 hover:underline focus-visible:outline-brand-500"
          @click="editEmail"
        >
          {{ t('auth.otp.editEmail') }}
        </button>
      </div>

      <template v-if="stage === 'otp'">
        <p class="text-sm text-slate-600">{{ t('auth.otp.sent', { email }) }}</p>
        <OtpInput v-model="otp" :busy="verifying" :invalid="Boolean(error)" @complete="submitCode" />
        <p v-if="verifying" class="text-sm text-slate-500">{{ t('auth.otp.verifying') }}</p>
        <p v-else-if="error && attemptsLeft < MAX_ATTEMPTS" class="text-xs text-slate-500">
          {{ t('auth.otp.attemptsLeft', { n: attemptsLeft }) }}
        </p>
        <p v-if="notice && !error" class="text-xs text-slate-500">{{ notice }}</p>

        <button
          type="button"
          class="text-start text-sm font-medium text-brand-700 hover:underline disabled:text-slate-400 disabled:no-underline"
          :disabled="cooldown > 0"
          @click="sendCode"
        >
          {{ cooldown > 0 ? t('auth.otp.resendIn', { n: cooldown }) : t('auth.otp.resend') }}
        </button>
      </template>

      <p
        v-else
        class="flex items-center gap-2 text-sm font-semibold text-emerald-700 transition-opacity duration-150 motion-reduce:transition-none"
      >
        <span
          class="inline-flex h-5 w-5 items-center justify-center rounded-full bg-emerald-100 text-emerald-700"
          aria-hidden="true"
        >✓</span>
        {{ t('auth.otp.verified') }}
      </p>
    </template>
  </div>
</template>
