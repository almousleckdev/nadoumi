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
    await navigateTo(isSafeRedirectPath(r) ? r : localePath('/dashboard'))
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
    <h1 class="font-display text-3xl font-bold text-slate-900">{{ t('auth.loginTitle') }}</h1>
    <p class="mt-2 text-slate-600">{{ t('auth.loginSubtitle') }}</p>

    <form class="mt-8 grid gap-5" @submit.prevent="submit">
      <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
      <NInput
        id="email"
        v-model="form.email"
        type="email"
        autocomplete="email"
        :placeholder="t('auth.email')"
        :aria-label="t('auth.email')"
      >
        <template #prefix>
          <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
            <rect x="3" y="5" width="18" height="14" rx="2" />
            <path d="m3 7 9 6 9-6" />
          </svg>
        </template>
      </NInput>
      <div class="grid gap-2">
        <PasswordField
          id="password"
          v-model="form.password"
          :label="t('auth.password')"
          autocomplete="current-password"
          plain
        />
        <NuxtLink :to="localePath('/forgot-password')" class="justify-self-end rounded text-sm font-medium text-brand-700 hover:underline">{{ t('auth.forgot') }}</NuxtLink>
      </div>
      <AuthCaptcha ref="captchaRef" v-model:code="form.code" v-model:uuid="form.uuid" />
      <NButton type="submit" size="lg" :loading="busy" block>{{ t('auth.submitLogin') }}</NButton>
    </form>

    <p class="mt-8 border-t border-slate-200 pt-6 text-center text-sm text-slate-600">
      {{ t('auth.newToNadoumi') }}
      <NuxtLink :to="localePath('/register')" class="ms-1 font-semibold text-brand-700 hover:underline">{{ t('auth.createProfile') }}</NuxtLink>
    </p>
  </div>
</template>
