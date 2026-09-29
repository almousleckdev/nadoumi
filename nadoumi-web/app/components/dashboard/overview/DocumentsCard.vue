<script setup lang="ts">
import { DOCUMENT_STATUS_TONES, type DocumentStatus } from '~/constants/onboarding'

defineProps<{ pending: boolean, photoStatus: DocumentStatus, passportStatus: DocumentStatus }>()

const { t } = useI18n()
const localePath = useLocalePath()
</script>

<template>
  <SectionCard :title="t('dashboard.home.documents')">
    <template #actions>
      <NuxtLink :to="localePath('/dashboard/documents')" class="text-sm font-medium text-brand-700 hover:underline">
        {{ t('dashboard.home.manageDocuments') }}
      </NuxtLink>
    </template>
    <div v-if="pending" class="grid gap-2">
      <NSkeleton class="h-6 w-full rounded" />
      <NSkeleton class="h-6 w-full rounded" />
    </div>
    <ul v-else class="grid gap-2">
      <li class="flex items-center justify-between text-sm">
        <span class="text-slate-700">{{ t('dashboard.home.docPhoto') }}</span>
        <NBadge :tone="DOCUMENT_STATUS_TONES[photoStatus]">{{ t(`onboarding.docStatus.${photoStatus}`) }}</NBadge>
      </li>
      <li class="flex items-center justify-between text-sm">
        <span class="text-slate-700">{{ t('dashboard.home.docPassport') }}</span>
        <NBadge :tone="DOCUMENT_STATUS_TONES[passportStatus]">{{ t(`onboarding.docStatus.${passportStatus}`) }}</NBadge>
      </li>
    </ul>
  </SectionCard>
</template>
