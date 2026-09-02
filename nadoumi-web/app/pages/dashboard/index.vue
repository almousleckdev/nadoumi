<script setup lang="ts">
definePageMeta({ layout: 'dashboard', middleware: 'auth' })
const { t } = useI18n()
const localePath = useLocalePath()
const { user } = useSession()
const { listMine } = useApplicant()

const { data: applicants } = await useAsyncData('dash-applicants', () => listMine().catch(() => []))
const mine = computed(() => applicants.value ?? [])
const primary = computed(() => mine.value[0] ?? null)

function completeness(a: NonNullable<typeof primary.value>) {
  const fields = [a.givenName, a.familyName, a.dob, a.nationality, a.passportNo, a.email, a.phone]
  const filled = fields.filter(Boolean).length
  return Math.round((filled / fields.length) * 100)
}

useSeo(t('dashboard.nav.overview'), t('dashboard.overviewBlurb'))
</script>

<template>
  <div class="grid gap-6">
    <h1 class="font-display text-2xl font-bold">{{ t('dashboard.welcome', { name: user?.nickName ?? user?.username ?? '' }) }}</h1>

    <SectionCard v-if="!primary" :title="t('dashboard.createProfileTitle')">
      <p class="text-sm text-slate-600">{{ t('dashboard.createProfileBlurb') }}</p>
      <NButton class="mt-4" :to="localePath('/dashboard/profile')">{{ t('dashboard.quickProfile') }}</NButton>
    </SectionCard>

    <template v-else>
      <SectionCard :title="t('dashboard.completeness')">
        <div class="flex items-center gap-3">
          <div class="h-2 flex-1 rounded-full bg-slate-100">
            <div class="h-2 rounded-full bg-brand-500" :style="{ width: `${completeness(primary)}%` }" />
          </div>
          <span class="text-sm font-semibold">{{ completeness(primary) }}%</span>
        </div>
      </SectionCard>
      <div class="flex gap-3">
        <NButton :to="localePath('/dashboard/profile')">{{ t('dashboard.quickProfile') }}</NButton>
        <NButton variant="secondary" :to="localePath('/dashboard/education')">{{ t('dashboard.quickEducation') }}</NButton>
      </div>
    </template>
  </div>
</template>
