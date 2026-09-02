<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()

const stage = ref<'verify' | 'reset' | 'done'>('verify')
const email = ref('')
const ticket = ref('')
const next = reactive({ password: '', confirm: '' })
const error = ref('')
const busy = ref(false)

function onVerified(verifiedTicket: string) {
  ticket.value = verifiedTicket
  error.value = ''
  stage.value = 'reset'
}

async function resetPassword() {
  error.value = ''
  const policyKey = passwordPolicyKey(next.password)
  if (policyKey) { error.value = t(policyKey); return }
  if (next.password !== next.confirm) { error.value = t('auth.passwordMismatch'); return }

  busy.value = true
  try {
    await $fetch('/api/student-password-reset', {
      method: 'POST',
      body: { ticket: ticket.value, newPassword: next.password },
    })
    stage.value = 'done'
  }
  catch (e) {
    error.value = problemMessage(e, t('auth.genericError'))
  }
  finally {
    busy.value = false
  }
}

useSeo(t('auth.forgotTitle'), t('auth.reset.title'))
</script>

<template>
  <div class="mx-auto max-w-md">
    <h1 class="mb-6 font-display text-2xl font-bold">{{ t('auth.forgotTitle') }}</h1>
    <NCard>
      <EmailVerifyStep
        v-if="stage === 'verify'"
        v-model:email="email"
        purpose="PASSWORD_RESET"
        @verified="onVerified"
      />

      <form v-else-if="stage === 'reset'" class="grid gap-4" @submit.prevent="resetPassword">
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
        <NField :label="t('auth.reset.newPassword')" for="reset-new" :hint="t('auth.passwordHint')" required>
          <NInput id="reset-new" v-model="next.password" type="password" autocomplete="new-password" :maxlength="32" />
        </NField>
        <NField :label="t('auth.confirmPassword')" for="reset-confirm" required>
          <NInput id="reset-confirm" v-model="next.confirm" type="password" autocomplete="new-password" :maxlength="32" />
        </NField>
        <NButton type="submit" :loading="busy" block>{{ t('auth.reset.title') }}</NButton>
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
