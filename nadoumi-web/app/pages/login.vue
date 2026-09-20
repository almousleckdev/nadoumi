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
    <NCard>
      <h1 class="mb-1 font-display text-2xl font-bold text-slate-900">{{ t('auth.loginTitle') }}</h1>
      <p class="mb-6 text-sm text-slate-500">{{ t('auth.loginSubtitle') }}</p>
      <form class="grid gap-4" @submit.prevent="submit">
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
        <PasswordField
          id="password"
          v-model="form.password"
          :label="t('auth.password')"
          autocomplete="current-password"
          plain
        />
        <AuthCaptcha ref="captchaRef" v-model:code="form.code" v-model:uuid="form.uuid" />
        <NButton type="submit" :loading="busy" block>{{ t('auth.submitLogin') }}</NButton>
      </form>
    </NCard>
    <div class="mt-4 text-center text-sm">
      <NuxtLink :to="localePath('/forgot-password')" class="rounded-full bg-white/95 px-3 py-1 font-medium text-slate-700 shadow-sm hover:bg-white hover:underline">{{ t('auth.forgot') }}</NuxtLink>
    </div>
    <div class="mt-5 rounded-xl bg-white/95 p-4 text-center text-sm text-slate-700 shadow-sm">
      {{ t('auth.newToNadoumi') }}
      <NuxtLink :to="localePath('/register')" class="ms-1 font-semibold text-brand-700 hover:underline">{{ t('auth.createProfile') }}</NuxtLink>
    </div>
  </div>
</template>
