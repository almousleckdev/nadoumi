<script setup lang="ts">
definePageMeta({ middleware: 'guest', layout: 'auth' })
const { t } = useI18n()
const localePath = useLocalePath()
const { refresh } = useSession()

const STEPS = ['personal', 'verify', 'password'] as const
const step = ref<1 | 2 | 3>(1)

const form = reactive({ firstName: '', lastName: '', email: '', password: '', confirm: '' })
const consent = ref({ terms: false, privacy: false })
const ticket = ref('')
const emailVerified = ref(false)
const error = ref('')
const submitted = ref(false)
const busy = ref(false)

// The OTP verification is the email-ownership check — no separate confirm-email field.
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const step1Valid = computed(() =>
  Boolean(form.firstName.trim() && form.lastName.trim() && EMAIL_RE.test(form.email)))

const forbidden = computed(() => [form.firstName, form.lastName, form.email.split('@')[0] ?? ''])
const passwordResult = computed(() =>
  passwordChecks(form.password, { forbidden: forbidden.value, confirm: form.confirm }))

const canSubmit = computed(() =>
  emailVerified.value
  && passwordResult.value.firstError === null
  && consent.value.terms && consent.value.privacy
  && !busy.value)

function onSent() {
  step.value = 2
}
function onEditEmail() {
  emailVerified.value = false
  ticket.value = ''
  step.value = 1
}
function onVerified(verifiedTicket: string) {
  ticket.value = verifiedTicket
  emailVerified.value = true
  error.value = ''
  step.value = 3
}

async function submit() {
  submitted.value = true
  error.value = ''
  if (!canSubmit.value) {
    if (!emailVerified.value) error.value = t('errors.otpExpired')
    else if (passwordResult.value.firstError) error.value = t(passwordResult.value.firstError)
    else error.value = t('auth.consentRequired')
    return
  }
  busy.value = true
  try {
    await $fetch('/api/student-account', {
      method: 'POST',
      body: {
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        password: form.password,
        ticket: ticket.value,
      },
    })
    await refresh()
    await navigateTo(localePath('/dashboard/onboarding'))
  }
  catch (err) {
    error.value = authErrorMessage(err, t)
  }
  finally {
    busy.value = false
  }
}

useSeo(t('auth.registerTitle'), t('home.subtitle'))
</script>

<template>
  <div>
    <h1 class="mb-1 text-center font-display text-2xl font-bold text-slate-900">{{ t('auth.registerTitle') }}</h1>
    <ol class="mb-6 flex items-center justify-center gap-2 text-xs font-medium text-slate-400">
      <li v-for="(s, i) in STEPS" :key="s" class="flex items-center gap-2">
        <span
          class="inline-flex h-6 w-6 items-center justify-center rounded-full border transition-colors"
          :class="step >= i + 1 ? 'border-brand-500 bg-brand-50 text-brand-700' : 'border-slate-200'"
        >{{ i + 1 }}</span>
        <span :class="step >= i + 1 ? 'text-slate-700' : ''">{{ t(`auth.registerStep.${s}`) }}</span>
        <span v-if="i < STEPS.length - 1" class="h-px w-6 bg-slate-200" />
      </li>
    </ol>

    <NCard>
      <!-- Steps 1 & 2 · Personal information + email verification.
           EmailVerifyStep is mounted once and owns the OTP UI; `step` (1 vs 2) only
           controls whether the name fields are shown above it. The OTP is the
           email-ownership check — there is no separate confirm-email field. -->
      <div v-if="step < 3" class="grid gap-4">
        <p class="text-sm font-semibold text-slate-700">
          {{ step === 1 ? t('auth.step1Title') : t('auth.step2Title') }}
        </p>
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

        <template v-if="step === 1">
          <div class="grid gap-4 sm:grid-cols-2">
            <NField :label="t('auth.firstName')" for="firstName" required>
              <NInput id="firstName" v-model="form.firstName" autocomplete="given-name" :maxlength="100" />
            </NField>
            <NField :label="t('auth.lastName')" for="lastName" required>
              <NInput id="lastName" v-model="form.lastName" autocomplete="family-name" :maxlength="100" />
            </NField>
          </div>
          <p class="text-xs text-slate-500">{{ t('auth.passportNameHint') }}</p>
        </template>

        <EmailVerifyStep
          v-model:email="form.email"
          purpose="REGISTER"
          :can-verify="step1Valid"
          @sent="onSent"
          @edit="onEditEmail"
          @verified="onVerified"
        />
      </div>

      <!-- Step 3 · Password -->
      <form v-else class="grid gap-4" @submit.prevent="submit">
        <p class="text-sm font-semibold text-slate-700">{{ t('auth.step3Title') }}</p>
        <p class="flex items-center gap-1.5 text-sm font-medium text-emerald-700">
          <span aria-hidden="true">✓</span>{{ t('auth.otp.verified') }} · {{ form.email }}
        </p>
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

        <PasswordField
          id="password"
          v-model="form.password"
          :label="t('auth.password')"
          :valid="passwordResult.strong"
        />
        <PasswordRequirements :value="form.password" :forbidden="forbidden" :confirm="form.confirm" />
        <PasswordField
          id="confirm"
          v-model="form.confirm"
          :label="t('auth.confirmPassword')"
          :error="submitted && form.confirm && form.password !== form.confirm ? t('validation.password.mismatch') : ''"
        />

        <ConsentCheckboxes v-model="consent" single :invalid="submitted" />

        <NButton type="submit" :loading="busy" :disabled="!canSubmit" block>{{ t('auth.next') }}</NButton>
      </form>
    </NCard>

    <p class="mt-4 text-center text-sm">
      <NuxtLink :to="localePath('/login')" class="text-brand-700 hover:underline">{{ t('auth.toLogin') }}</NuxtLink>
    </p>
  </div>
</template>
