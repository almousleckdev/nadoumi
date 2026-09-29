<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import DashboardGroup from '@/components/dashboard/DashboardGroup.vue'
import StatCard from '@/components/dashboard/StatCard.vue'
import type { useDashboardAccess } from './useDashboardAccess'

defineProps<{
  access: ReturnType<typeof useDashboardAccess>
  counts: Record<'students' | 'universities' | 'programmes' | 'scholarships', number | null>
  applicantTotal: number | null
  applicantState: 'ok' | 'loading' | 'error'
}>()

const { t } = useI18n()
</script>

<template>
  <DashboardGroup
    :title="t('dashboard.groups.platform')"
    :subtitle="t('dashboard.liveData')"
  >
    <StatCard
      v-if="access.canSeeStudents.value"
      :label="t('dashboard.k.totalStudents')"
      :value="counts.students"
      :state="counts.students === null ? 'loading' : 'ok'"
    />
    <StatCard
      v-if="access.canSeeApplicants.value"
      :label="t('dashboard.k.totalApplicants')"
      :value="applicantTotal"
      :state="applicantState"
    />
    <StatCard
      v-if="access.canSeeUniversities.value"
      :label="t('dashboard.k.totalUniversities')"
      :value="counts.universities"
      :state="counts.universities === null ? 'loading' : 'ok'"
    />
    <StatCard
      v-if="access.canSeeCatalog.value"
      :label="t('dashboard.k.totalProgrammes')"
      :value="counts.programmes"
      :state="counts.programmes === null ? 'loading' : 'ok'"
    />
    <StatCard
      v-if="access.canSeeScholarships.value"
      :label="t('dashboard.k.totalScholarships')"
      :value="counts.scholarships"
      :state="counts.scholarships === null ? 'loading' : 'ok'"
    />
  </DashboardGroup>
</template>
