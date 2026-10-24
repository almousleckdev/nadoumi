<script setup lang="ts">
import { DOCUMENT_STATUS_TONES, type DocumentStatus } from '~/constants/onboarding'

defineProps<{ completeness: number, pending: boolean, photoStatus: DocumentStatus, passportStatus: DocumentStatus }>()

const { t } = useI18n()
const localePath = useLocalePath()
</script>

<template>
  <SectionCard :title="t('dashboard.home.readiness')">
    <template #actions>
      <NTextLink :to="localePath('/dashboard/documents')">{{ t('dashboard.home.manageDocuments') }}</NTextLink>
    </template>
    <div class="flex flex-wrap items-center gap-5">
      <ProgressRing :percent="completeness" :label="t('dashboard.completeness')" />
      <ul class="grid min-w-0 flex-1 gap-3 text-sm">
        <li class="flex items-center justify-between gap-3">
          <NuxtLink :to="localePath('/dashboard/profile')" class="text-slate-700 hover:underline">{{ t('dashboard.completeness') }}</NuxtLink>
          <span class="font-semibold tabular-nums text-slate-900">{{ completeness }}%</span>
        </li>
        <template v-if="pending">
          <li><NSkeleton class="h-6 w-full rounded" /></li>
          <li><NSkeleton class="h-6 w-full rounded" /></li>
        </template>
        <template v-else>
          <li class="flex items-center justify-between gap-3">
            <span class="text-slate-700">{{ t('dashboard.home.docPhoto') }}</span>
            <NBadge :tone="DOCUMENT_STATUS_TONES[photoStatus]">{{ t(`onboarding.docStatus.${photoStatus}`) }}</NBadge>
          </li>
          <li class="flex items-center justify-between gap-3">
            <span class="text-slate-700">{{ t('dashboard.home.docPassport') }}</span>
            <NBadge :tone="DOCUMENT_STATUS_TONES[passportStatus]">{{ t(`onboarding.docStatus.${passportStatus}`) }}</NBadge>
          </li>
        </template>
      </ul>
    </div>
  </SectionCard>
</template>
