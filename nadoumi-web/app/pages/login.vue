<script setup lang="ts">
definePageMeta({ middleware: 'guest' })
const { t } = useI18n()
const route = useRoute()
const localePath = useLocalePath()
const { refresh } = useSession()

const form = reactive({ username: '', password: '', code: '', uuid: '' })
const error = ref('')
const busy = ref(false)

async function submit() {
  error.value = ''
  busy.value = true
  try {
    await $fetch('/api/student-session', { method: 'POST', body: { ...form } })
    await refresh()
    await navigateTo((route.query.redirect as string) || localePath('/dashboard'))
  }
  catch (err) {
    error.value = problemMessage(err, t('auth.genericError'))
  }
  finally {
    busy.value = false
  }
}

useSeo(t('auth.loginTitle'), t('auth.loginTitle'))
</script>

<template>
  <div class="mx-auto max-w-md">
    <h1 class="mb-6 font-display text-2xl font-bold">{{ t('auth.loginTitle') }}</h1>
    <NCard>
      <form class="grid gap-4" @submit.prevent="submit">
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
        <NField :label="t('auth.username')" for="username" required>
          <NInput id="username" v-model="form.username" autocomplete="username" />
        </NField>
        <NField :label="t('auth.password')" for="password" required>
          <NInput id="password" v-model="form.password" type="password" autocomplete="current-password" />
        </NField>
        <AuthCaptcha v-model:code="form.code" v-model:uuid="form.uuid" />
        <NButton type="submit" :loading="busy" block>{{ t('auth.submitLogin') }}</NButton>
      </form>
    </NCard>
    <div class="mt-4 flex justify-between text-sm">
      <NuxtLink :to="localePath('/register')" class="text-brand-700 hover:underline">{{ t('auth.toRegister') }}</NuxtLink>
      <NuxtLink :to="localePath('/forgot-password')" class="text-slate-500 hover:underline">{{ t('auth.forgot') }}</NuxtLink>
    </div>
  </div>
</template>
