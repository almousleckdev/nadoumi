<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'
import { profileCompleteness } from '~/utils/profileCompleteness'

const props = defineProps<{ applicant: ApplicantDto }>()

const { t } = useI18n()
const localePath = useLocalePath()
const completeness = computed(() => profileCompleteness(props.applicant))
</script>

<template>
  <div class="rounded-xl border border-slate-200 bg-white p-5">
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <h1 class="font-display text-xl font-bold text-slate-900">
          {{ t('dashboard.welcome', { name: applicant.givenName }) }}
        </h1>
        <p class="mt-1 text-sm text-slate-500">{{ t('dashboard.home.subtitle') }}</p>
      </div>
      <div v-if="completeness < 100" class="w-full shrink-0 sm:w-56">
        <div class="mb-1 flex items-center justify-between text-xs">
          <span class="font-medium text-slate-600">{{ t('dashboard.completeness') }}</span>
          <span class="font-semibold text-slate-700">{{ completeness }}%</span>
        </div>
        <div class="h-1.5 rounded-full bg-slate-100">
          <div class="h-1.5 rounded-full bg-brand-500" :style="{ width: `${completeness}%` }" />
        </div>
        <NButton class="mt-2" size="sm" variant="secondary" :to="localePath('/dashboard/profile')">
          {{ t('dashboard.quickProfile') }}
        </NButton>
      </div>
    </div>
  </div>
</template>
