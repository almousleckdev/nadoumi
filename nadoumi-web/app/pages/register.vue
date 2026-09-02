<script setup lang="ts">
definePageMeta({ middleware: 'guest' })
const { t } = useI18n()
const localePath = useLocalePath()
const { refresh } = useSession()

const step = ref<1 | 2>(1)
const form = reactive({ firstName: '', lastName: '', email: '', password: '', confirm: '', terms: false })
const ticket = ref('')
const error = ref('')
const busy = ref(false)

const namesReady = computed(() => Boolean(form.firstName.trim() && form.lastName.trim()))

function goToPassword(verifiedTicket: string) {
  ticket.value = verifiedTicket
  error.value = ''
  step.value = 2
}

async function submit() {
  error.value = ''
  if (!form.terms) { error.value = t('validation.required'); return }
  const policyKey = passwordPolicyKey(form.password)
  if (policyKey) { error.value = t(policyKey); return }
  if (form.password !== form.confirm) { error.value = t('auth.passwordMismatch'); return }

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
    error.value = problemMessage(err, t('auth.genericError'))
  }
  finally {
    busy.value = false
  }
}

useSeo(t('auth.registerTitle'), t('home.subtitle'))
</script>

<template>
  <div class="mx-auto max-w-md">
    <h1 class="mb-6 font-display text-2xl font-bold">{{ t('auth.registerTitle') }}</h1>
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
        <NField :label="t('auth.password')" for="password" :hint="t('auth.passwordHint')" required>
          <NInput id="password" v-model="form.password" type="password" autocomplete="new-password" :maxlength="32" />
        </NField>
        <NField :label="t('auth.confirmPassword')" for="confirm" required>
          <NInput id="confirm" v-model="form.confirm" type="password" autocomplete="new-password" :maxlength="32" />
        </NField>
        <NCheckbox id="terms" v-model="form.terms">
          <i18n-t keypath="auth.acceptTerms">
            <template #terms>
              <NuxtLink :to="localePath('/terms')" class="text-brand-700 hover:underline">{{ t('footer.terms') }}</NuxtLink>
            </template>
            <template #privacy>
              <NuxtLink :to="localePath('/privacy')" class="text-brand-700 hover:underline">{{ t('footer.privacy') }}</NuxtLink>
            </template>
          </i18n-t>
        </NCheckbox>
        <NButton type="submit" :loading="busy" block>{{ t('auth.next') }}</NButton>
      </form>
    </NCard>
    <p class="mt-4 text-center text-sm">
      <NuxtLink :to="localePath('/login')" class="text-brand-700 hover:underline">{{ t('auth.toLogin') }}</NuxtLink>
    </p>
  </div>
</template>
