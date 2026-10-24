<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'

defineProps<{ applicant: ApplicantDto }>()

const { t } = useI18n()
const localePath = useLocalePath()
</script>

<template>
  <header class="flex flex-wrap items-end justify-between gap-4">
    <div class="min-w-0">
      <h1 class="font-display text-2xl font-bold tracking-tight text-slate-900">{{ t('dashboard.welcome', { name: applicant.givenName }) }}</h1>
      <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.home.subtitle') }}</p>
    </div>
    <NButton size="sm" :to="localePath('/scholarships')">{{ t('dashboard.home.findOpportunity') }}</NButton>
  </header>
</template>
