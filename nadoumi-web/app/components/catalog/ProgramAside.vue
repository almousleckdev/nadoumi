<script setup lang="ts">
import type { ProgramDetail } from '~/types/catalog'
import { programTuition } from '~/utils/programDisplay'

const props = defineProps<{ program: ProgramDetail, applyTo: string, universityTo: string }>()

const { t } = useI18n()
const tuition = computed(() => programTuition(props.program))
</script>

<template>
  <DetailSidebarCard v-if="tuition.cny" :title="t('program.tuition')">
    <p class="font-display text-2xl font-bold text-red-600">{{ tuition.cny }}</p>
    <p v-if="tuition.usd" class="text-sm font-medium text-red-600">≈ {{ tuition.usd }}</p>
    <p class="mt-2 text-xs text-slate-500">{{ t('program.tuitionNote') }}</p>
  </DetailSidebarCard>

  <DetailSidebarCard v-if="program.universityName" :title="t('program.universityLabel')">
    <NuxtLink :to="universityTo" class="block font-display text-base font-semibold text-slate-900 no-underline hover:underline">
      {{ program.universityName }}
    </NuxtLink>
    <NButton :to="universityTo" variant="secondary" size="sm" class="mt-3">{{ t('program.view') }} →</NButton>
  </DetailSidebarCard>

  <DetailSidebarCard>
    <p class="text-sm text-slate-700">{{ t('program.applyVia', { university: program.universityName ?? '' }) }}</p>
    <NButton :to="applyTo" size="sm" class="mt-3">{{ t('program.applyNow') }}</NButton>
  </DetailSidebarCard>
</template>
