<script setup lang="ts">
definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId } = useSession()

useSeo(t('dashboard.workTitle'), t('dashboard.workTitle'))
</script>

<template>
  <SectionCard :title="t('dashboard.workTitle')">
    <NAlert v-if="activeApplicantId == null" tone="warning">
      {{ t('dashboard.createProfileBlurb') }}
      <NuxtLink :to="localePath('/dashboard/profile')" class="ms-1 underline">{{ t('dashboard.quickProfile') }}</NuxtLink>
    </NAlert>
    <SuspenseBoundary v-else>
      <WorkSection :applicant-id="activeApplicantId" />
    </SuspenseBoundary>
  </SectionCard>
</template>
