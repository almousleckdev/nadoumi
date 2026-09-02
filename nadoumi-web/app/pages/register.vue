<script setup lang="ts">
definePageMeta({ middleware: 'guest' })
const { t } = useI18n()
const localePath = useLocalePath()
const { refresh } = useSession()

const form = reactive({ fullName: '', username: '', email: '', password: '', confirm: '', code: '', uuid: '', terms: false })
const error = ref('')
const busy = ref(false)
const captchaRef = ref<{ enabled: boolean } | null>(null)

function validate(): string | null {
  if (!form.fullName || !form.username || !form.password) return t('validation.required')
  if (form.username.length < 2 || form.username.length > 20) return t('auth.usernameHint')
  if (form.password.length < 5 || form.password.length > 20) return t('auth.passwordHint')
  if (form.password !== form.confirm) return t('auth.passwordMismatch')
  if (!form.terms) return t('validation.required')
  if (captchaRef.value?.enabled && !form.code) return t('auth.captcha')
  return null
}

async function submit() {
  error.value = ''
  const v = validate()
  if (v) { error.value = v; return }
  busy.value = true
  try {
    await $fetch('/api/student-account', {
      method: 'POST',
      body: {
        fullName: form.fullName, username: form.username, email: form.email || undefined,
        password: form.password, code: form.code, uuid: form.uuid,
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
      <form class="grid gap-4" @submit.prevent="submit">
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
        <NField :label="t('auth.fullName')" for="fullName" required>
          <NInput id="fullName" v-model="form.fullName" autocomplete="name" />
        </NField>
        <NField :label="t('auth.username')" for="username" :hint="t('auth.usernameHint')" required>
          <NInput id="username" v-model="form.username" autocomplete="username" :maxlength="20" />
        </NField>
        <NField :label="t('auth.email')" for="email">
          <NInput id="email" v-model="form.email" type="email" autocomplete="email" />
        </NField>
        <NField :label="t('auth.password')" for="password" :hint="t('auth.passwordHint')" required>
          <NInput id="password" v-model="form.password" type="password" autocomplete="new-password" :maxlength="20" />
        </NField>
        <NField :label="t('auth.confirmPassword')" for="confirm" required>
          <NInput id="confirm" v-model="form.confirm" type="password" autocomplete="new-password" :maxlength="20" />
        </NField>
        <AuthCaptcha ref="captchaRef" v-model:code="form.code" v-model:uuid="form.uuid" />
        <NCheckbox id="terms" v-model="form.terms">
          <i18n-t keypath="auth.acceptTerms">
            <template #terms><NuxtLink :to="localePath('/terms')" class="text-brand-700 hover:underline">{{ t('footer.terms') }}</NuxtLink></template>
            <template #privacy><NuxtLink :to="localePath('/privacy')" class="text-brand-700 hover:underline">{{ t('footer.privacy') }}</NuxtLink></template>
          </i18n-t>
        </NCheckbox>
        <NButton type="submit" :loading="busy" block>{{ t('auth.submitRegister') }}</NButton>
      </form>
    </NCard>
    <p class="mt-4 text-center text-sm">
      <NuxtLink :to="localePath('/login')" class="text-brand-700 hover:underline">{{ t('auth.toLogin') }}</NuxtLink>
    </p>
  </div>
</template>
