<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
useSeo(t('auth.forgotTitle'), t('auth.forgotBody'))
</script>

<template>
  <div class="mx-auto max-w-md">
    <h1 class="mb-6 font-display text-2xl font-bold">{{ t('auth.forgotTitle') }}</h1>
    <NCard>
      <p class="text-sm text-slate-700">{{ t('auth.forgotBody') }}</p>
      <p class="mt-4">
        <a href="mailto:support@nadoumi.com" class="text-brand-700 hover:underline">support@nadoumi.com</a>
      </p>
    </NCard>
    <p class="mt-4 text-center text-sm">
      <NuxtLink :to="localePath('/login')" class="text-brand-700 hover:underline">{{ t('auth.toLogin') }}</NuxtLink>
    </p>
  </div>
</template>
