<script setup lang="ts">
definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId } = useSession()

useSeo(t('dashboard.eduTitle'), t('dashboard.eduTitle'))
</script>

<template>
  <SectionCard :title="t('dashboard.eduTitle')">
    <NAlert v-if="activeApplicantId == null" tone="warning">
      {{ t('dashboard.createProfileBlurb') }}
      <NuxtLink :to="localePath('/dashboard/profile')" class="ms-1 underline">{{ t('dashboard.quickProfile') }}</NuxtLink>
    </NAlert>
    <SuspenseBoundary v-else>
      <EducationSection :applicant-id="activeApplicantId" />
    </SuspenseBoundary>
  </SectionCard>
</template>
