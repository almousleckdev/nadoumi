<script setup lang="ts">
definePageMeta({ middleware: 'guest', layout: 'auth' })
const { t } = useI18n()
const route = useRoute()
const localePath = useLocalePath()
const { refresh } = useSession()

const form = reactive({ email: '', password: '', code: '', uuid: '' })
const error = ref('')
const busy = ref(false)
const captchaRef = ref<{ enabled: boolean } | null>(null)

async function submit() {
  error.value = ''
  if (!form.email || !form.password) { error.value = t('validation.required'); return }
  if (captchaRef.value?.enabled && !form.code) { error.value = t('auth.captcha'); return }
  busy.value = true
  try {
    await $fetch('/api/student-session', { method: 'POST', body: { ...form } })
    await refresh()
    const r = route.query.redirect as string | undefined
    const safe = !!r && r.startsWith('/') && !r.startsWith('//')
    await navigateTo(safe ? r : localePath('/dashboard'))
  }
  catch (err) {
    error.value = authErrorMessage(err, t)
  }
  finally {
    busy.value = false
  }
}

useSeo(t('auth.loginTitle'), t('auth.loginTitle'))
</script>

<template>
  <div>
    <h1 class="mb-6 text-center font-display text-2xl font-bold text-slate-900">{{ t('auth.loginTitle') }}</h1>
    <NCard>
      <form class="grid gap-4" @submit.prevent="submit">
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
        <NField :label="t('auth.email')" for="email" required>
          <NInput id="email" v-model="form.email" type="email" autocomplete="email" />
        </NField>
        <PasswordField
          id="password"
          v-model="form.password"
          :label="t('auth.password')"
          autocomplete="current-password"
        />
        <AuthCaptcha ref="captchaRef" v-model:code="form.code" v-model:uuid="form.uuid" />
        <NButton type="submit" :loading="busy" block>{{ t('auth.submitLogin') }}</NButton>
      </form>
    </NCard>
    <div class="mt-4 flex justify-between text-sm">
      <NuxtLink :to="localePath('/register')" class="text-brand-700 hover:underline">{{ t('auth.toRegister') }}</NuxtLink>
      <NuxtLink :to="localePath('/forgot-password')" class="text-slate-500 hover:underline">{{ t('auth.forgot') }}</NuxtLink>
    </div>
  </div>
</template>
