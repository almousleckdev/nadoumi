<script setup lang="ts">
definePageMeta({ middleware: 'guest', layout: 'auth' })
const { t } = useI18n()
const localePath = useLocalePath()
const { refresh } = useSession()

const step = ref<1 | 2>(1)
const form = reactive({ firstName: '', lastName: '', email: '', password: '', confirm: '' })
const consent = ref({ terms: false, privacy: false })
const ticket = ref('')
const error = ref('')
const submitted = ref(false)
const busy = ref(false)

const namesReady = computed(() => Boolean(form.firstName.trim() && form.lastName.trim()))

const forbidden = computed(() => [
  form.firstName,
  form.lastName,
  form.email.split('@')[0] ?? '',
])

const passwordResult = computed(() =>
  passwordChecks(form.password, { forbidden: forbidden.value, confirm: form.confirm }))

const canSubmit = computed(() =>
  passwordResult.value.firstError === null
  && consent.value.terms
  && consent.value.privacy
  && !busy.value)

function goToPassword(verifiedTicket: string) {
  ticket.value = verifiedTicket
  error.value = ''
  step.value = 2
}

async function submit() {
  submitted.value = true
  error.value = ''
  if (!canSubmit.value) {
    const key = passwordResult.value.firstError
    if (key) error.value = t(key)
    else if (!consent.value.terms || !consent.value.privacy) error.value = t('auth.consentRequired')
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
    await navigateTo(localePath('/dashboard/profile'))
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
    <h1 class="mb-6 text-center font-display text-2xl font-bold text-slate-900">{{ t('auth.registerTitle') }}</h1>
    <NCard>
      <div v-if="step === 1" class="grid gap-4">
        <p class="text-sm font-semibold text-slate-700">{{ t('auth.step1Title') }}</p>
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
        <div class="grid gap-4 sm:grid-cols-2">
          <NField :label="t('auth.firstName')" for="firstName" required>
            <NInput id="firstName" v-model="form.firstName" autocomplete="given-name" :maxlength="100" />
          </NField>
          <NField :label="t('auth.lastName')" for="lastName" required>
            <NInput id="lastName" v-model="form.lastName" autocomplete="family-name" :maxlength="100" />
          </NField>
        </div>
        <p class="text-xs text-slate-500">{{ t('auth.passportNameHint') }}</p>
        <EmailVerifyStep
          v-model:email="form.email"
          purpose="REGISTER"
          :can-verify="namesReady"
          @verified="goToPassword"
        />
      </div>

      <form v-else class="grid gap-4" @submit.prevent="submit">
        <p class="text-sm font-semibold text-slate-700">{{ t('auth.step2Title') }}</p>
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

        <ConsentCheckboxes v-model="consent" :invalid="submitted" />

        <NButton type="submit" :loading="busy" :disabled="!canSubmit" block>{{ t('auth.next') }}</NButton>
      </form>
    </NCard>
    <p class="mt-4 text-center text-sm">
      <NuxtLink :to="localePath('/login')" class="text-brand-700 hover:underline">{{ t('auth.toLogin') }}</NuxtLink>
    </p>
  </div>
</template>
