<script setup lang="ts">
import { toRef } from 'vue'

definePageMeta({ middleware: 'guest', layout: 'auth' })
const { t } = useI18n()
const localePath = useLocalePath()
const { refresh } = useSession()

const STEPS = ['personal', 'verify', 'password'] as const
const step = ref<1 | 2 | 3>(1)

const form = reactive({ username: '', email: '', password: '', confirm: '' })
const consent = ref({ terms: false, privacy: false })
const ticket = ref('')
const emailVerified = ref(false)
const error = ref('')
const submitted = ref(false)
const busy = ref(false)

// The student's legal names are asked for later, in onboarding, from the passport. Here: a handle and a verified email.
const usernameRef = toRef(form, 'username')
const { status: usernameStatus, problem: usernameProblemKey, usable: usernameUsable } = useUsernameCheck(usernameRef)
const usernameFeedback = computed(() => {
  if (usernameProblemKey.value) return { tone: 'error', text: t(`auth.usernameProblem.${usernameProblemKey.value}`) }
  if (usernameStatus.value === 'taken') return { tone: 'error', text: t('auth.usernameTaken') }
  if (usernameStatus.value === 'checking') return { tone: 'info', text: t('auth.usernameChecking') }
  if (usernameStatus.value === 'available') return { tone: 'ok', text: t('auth.usernameAvailable') }
  return null
})
// The OTP verification is the email-ownership check — no separate confirm-email field.
const step1Valid = computed(() => usernameUsable.value && isValidEmail(form.email))

const forbidden = computed(() => [normalizeUsername(form.username), form.email.split('@')[0] ?? ''])
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
    const res = await $fetch<{ signedIn: boolean }>('/api/student-account', {
      method: 'POST',
      body: {
        username: normalizeUsername(form.username),
        email: form.email,
        password: form.password,
        ticket: ticket.value,
      },
    })
    if (!res.signedIn) {
      // The account was created; only the automatic sign-in failed, so send them to sign in.
      useToast().info(t('auth.accountCreatedSignIn'))
      await navigateTo(localePath('/login'))
      return
    }
    await refresh()
    await navigateTo(localePath('/onboarding'))
  }
  catch (err) {
    error.value = authErrorMessage(err, t)
  }
  finally {
    busy.value = false
  }
}

useSeo(t('auth.registerTitle'), t('home.hero.subtitle'))
</script>

<template>
  <div>
    <h1 class="font-display text-3xl font-bold text-slate-900">{{ t('auth.registerTitle') }}</h1>
    <p class="mt-2 text-slate-600">{{ t('auth.registerSubtitle') }}</p>
    <ol class="mb-8 mt-6 grid grid-cols-3 gap-2" data-test="register-steps">
      <li v-for="(s, i) in STEPS" :key="s" :aria-current="step === i + 1 ? 'step' : undefined">
        <span class="block h-1.5 rounded-full transition-colors" :class="step >= i + 1 ? 'bg-brand-500' : 'bg-slate-200'" />
        <span class="mt-2 block text-xs font-medium" :class="step === i + 1 ? 'text-slate-900' : step > i + 1 ? 'text-brand-700' : 'text-slate-500'">
          {{ i + 1 }}. {{ t(`auth.registerStep.${s}`) }}
        </span>
      </li>
    </ol>
    <div>

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
          <NField :label="t('auth.username')" for="username" :hint="t('auth.usernameHint')" required>
            <NInput
              id="username"
              :model-value="form.username"
              autocomplete="username"
              autocapitalize="none"
              spellcheck="false"
              :maxlength="20"
              :invalid="usernameFeedback?.tone === 'error'"
              :valid="usernameFeedback?.tone === 'ok'"
              @update:model-value="(v: string) => form.username = v.toLowerCase().replace(/\s/g, '')"
            />
          </NField>
          <p
            v-if="usernameFeedback"
            class="-mt-2 text-sm"
            :class="{ 'text-red-600': usernameFeedback.tone === 'error', 'text-emerald-700': usernameFeedback.tone === 'ok', 'text-slate-500': usernameFeedback.tone === 'info' }"
            role="status"
            data-test="username-feedback"
          >
            {{ usernameFeedback.text }}
          </p>
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

        <NButton type="submit" size="lg" :loading="busy" :disabled="!canSubmit" block>{{ t('auth.next') }}</NButton>
      </form>
    </div>

    <p class="mt-8 border-t border-slate-200 pt-6 text-center text-sm">
      <NuxtLink :to="localePath('/login')" class="font-medium text-slate-700 hover:text-brand-700 hover:underline">{{ t('auth.toLogin') }}</NuxtLink>
    </p>
  </div>
</template>
