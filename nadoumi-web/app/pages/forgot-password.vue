<script setup lang="ts">
definePageMeta({ layout: 'auth' })
const { t } = useI18n()
const localePath = useLocalePath()

const stage = ref<'verify' | 'reset' | 'done'>('verify')
const email = ref('')
const ticket = ref('')
const next = reactive({ password: '', confirm: '' })
const error = ref('')
const submitted = ref(false)
const busy = ref(false)

const forbidden = computed(() => [email.value.split('@')[0] ?? ''])
const passwordResult = computed(() =>
  passwordChecks(next.password, { forbidden: forbidden.value, confirm: next.confirm }))
const canSubmit = computed(() => passwordResult.value.firstError === null && !busy.value)

function onVerified(verifiedTicket: string) {
  ticket.value = verifiedTicket
  error.value = ''
  stage.value = 'reset'
}

async function resetPassword() {
  submitted.value = true
  error.value = ''
  if (!canSubmit.value) {
    error.value = passwordResult.value.firstError ? t(passwordResult.value.firstError) : ''
    return
  }
  busy.value = true
  try {
    await $fetch('/api/student-password-reset', {
      method: 'POST',
      body: { ticket: ticket.value, newPassword: next.password },
    })
    stage.value = 'done'
  }
  catch (e) {
    error.value = authErrorMessage(e, t)
  }
  finally {
    busy.value = false
  }
}

useSeo(t('auth.forgotTitle'), t('auth.reset.title'))
</script>

<template>
  <div>
    <h1 class="mb-6 text-center font-display text-2xl font-bold text-slate-900">{{ t('auth.forgotTitle') }}</h1>
    <NCard>
      <EmailVerifyStep
        v-if="stage === 'verify'"
        v-model:email="email"
        purpose="PASSWORD_RESET"
        @verified="onVerified"
      />

      <form v-else-if="stage === 'reset'" class="grid gap-4" @submit.prevent="resetPassword">
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
        <PasswordField
          id="reset-new"
          v-model="next.password"
          :label="t('auth.reset.newPassword')"
          :valid="passwordResult.strong"
        />
        <PasswordRequirements :value="next.password" :forbidden="forbidden" :confirm="next.confirm" />
        <PasswordField
          id="reset-confirm"
          v-model="next.confirm"
          :label="t('auth.confirmPassword')"
          :error="submitted && next.confirm && next.password !== next.confirm ? t('validation.password.mismatch') : ''"
        />
        <NButton type="submit" :loading="busy" :disabled="!canSubmit" block>{{ t('auth.reset.title') }}</NButton>
      </form>

      <div v-else class="grid gap-4">
        <NAlert tone="success">{{ t('auth.reset.done') }}</NAlert>
        <NuxtLink :to="localePath('/login')" class="text-brand-700 hover:underline">
          {{ t('auth.reset.toLogin') }}
        </NuxtLink>
      </div>
    </NCard>
    <p class="mt-4 text-center text-sm">
      <NuxtLink :to="localePath('/login')" class="text-brand-700 hover:underline">{{ t('auth.toLogin') }}</NuxtLink>
    </p>
  </div>
</template>
