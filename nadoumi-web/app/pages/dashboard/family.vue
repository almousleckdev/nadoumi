<script setup lang="ts">
definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId } = useSession()

useSeo(t('family.title'), t('family.blurb'))
</script>

<template>
  <div class="grid gap-6">
    <header>
      <h1 class="font-display text-2xl font-bold text-slate-900">{{ t('family.title') }}</h1>
      <p class="mt-1 text-sm text-slate-600">{{ t('family.blurb') }}</p>
    </header>

    <NAlert v-if="activeApplicantId == null" tone="warning">
      {{ t('dashboard.createProfileBlurb') }}
      <NuxtLink :to="localePath('/dashboard/profile')" class="ms-1 underline">{{ t('dashboard.quickProfile') }}</NuxtLink>
    </NAlert>
    <template v-else>
      <SectionCard :title="t('family.personal.title')">
        <SuspenseBoundary><PersonalDetailsSection :applicant-id="activeApplicantId" /></SuspenseBoundary>
      </SectionCard>
      <SectionCard :title="t('family.member.title')">
        <SuspenseBoundary><FamilyMemberSection :applicant-id="activeApplicantId" /></SuspenseBoundary>
      </SectionCard>
      <SectionCard :title="t('family.supporter.title')" :description="t('family.supporter.hint')">
        <SuspenseBoundary><PersonSection :applicant-id="activeApplicantId" role="supporter" /></SuspenseBoundary>
      </SectionCard>
      <SectionCard :title="t('family.chinaEmergency.title')" :description="t('family.chinaEmergency.hint')">
        <SuspenseBoundary><PersonSection :applicant-id="activeApplicantId" role="china-emergency" /></SuspenseBoundary>
      </SectionCard>
    </template>
  </div>
</template>
