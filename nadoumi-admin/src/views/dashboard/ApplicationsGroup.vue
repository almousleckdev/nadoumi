<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import DashboardGroup from '@/components/dashboard/DashboardGroup.vue'
import StatCard from '@/components/dashboard/StatCard.vue'
import type { useApplicationPipeline } from './useApplicationPipeline'
import type { useDashboardAccess } from './useDashboardAccess'

defineProps<{
  pipeline: ReturnType<typeof useApplicationPipeline>
  access: ReturnType<typeof useDashboardAccess>
}>()

const { t } = useI18n()
const router = useRouter()
</script>

<template>
  <DashboardGroup
    :title="t('dashboard.groups.applications')"
    :subtitle="t('dashboard.liveData')"
  >
    <template v-if="access.canSeeApplications.value">
      <StatCard
        class="dash-link"
        :label="t('dashboard.k.applicationsSubmitted')"
        :value="pipeline.total.value"
        :state="pipeline.failed.value ? 'error' : (pipeline.loading.value && !pipeline.total.value ? 'loading' : 'ok')"
        @click="router.push('/applications')"
      />
      <StatCard
        class="dash-link"
        :label="t('dashboard.k.applicationsInReview')"
        :value="pipeline.inReview.value"
        :hint="t('dashboard.k.needsAction')"
        :state="pipeline.failed.value ? 'error' : (pipeline.loading.value && !pipeline.total.value ? 'loading' : 'ok')"
        @click="router.push('/applications')"
      />
      <StatCard
        class="dash-link"
        :label="t('dashboard.k.applicationsOffers')"
        :value="pipeline.offers.value"
        :state="pipeline.failed.value ? 'error' : (pipeline.loading.value && !pipeline.total.value ? 'loading' : 'ok')"
        @click="router.push('/applications')"
      />
    </template>
    <StatCard
      v-if="access.canSeePayments.value"
      class="dash-link"
      :label="t('dashboard.k.paymentsToConfirm')"
      :value="pipeline.paymentsToConfirm.value"
      :hint="t('dashboard.k.needsAction')"
      :state="pipeline.failed.value ? 'error' : (pipeline.paymentsToConfirm.value === null ? 'loading' : 'ok')"
      @click="router.push('/payments')"
    />
  </DashboardGroup>
</template>

<style scoped>
.dash-link {
  cursor: pointer;
}
</style>
