<script setup lang="ts">
definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId } = useSession()

useSeo(t('dashboard.interestsTitle'), t('dashboard.interestsTitle'))
</script>

<template>
  <SectionCard :title="t('dashboard.interestsTitle')">
    <NAlert v-if="activeApplicantId == null" tone="warning">
      {{ t('dashboard.createProfileBlurb') }}
      <NuxtLink :to="localePath('/dashboard/profile')" class="ms-1 underline">{{ t('dashboard.quickProfile') }}</NuxtLink>
    </NAlert>
    <SuspenseBoundary v-else>
      <InterestsSection :applicant-id="activeApplicantId" />
    </SuspenseBoundary>
  </SectionCard>
</template>
