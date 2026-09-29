<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import DonutChart from '@/components/dashboard/DonutChart.vue'
import DonutChartSkeleton from '@/components/dashboard/DonutChartSkeleton.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import type { Slice } from './dashboardMath'

defineProps<{
  title: string
  slices: Slice[]
  caption: string
  loading: boolean
  emptyTitle: string
  emptyIcon: string
  viewAllTo?: string
}>()

const { t } = useI18n()
const router = useRouter()
</script>

<template>
  <el-card
    shadow="never"
    class="dash__panel"
  >
    <template #header>
      <span class="dash__panel-title">{{ title }}</span>
      <el-button
        v-if="viewAllTo"
        link
        type="primary"
        @click="router.push(viewAllTo)"
      >
        {{ t('dashboard.viewAll') }}
      </el-button>
    </template>
    <DonutChart
      v-if="slices.length"
      :slices="slices"
      :caption="caption"
    />
    <DonutChartSkeleton v-else-if="loading" />
    <EmptyState
      v-else
      :title="emptyTitle"
      :icon="emptyIcon"
    />
  </el-card>
</template>

<style scoped>
.dash__panel {
  border: 1px solid var(--nad-line);
  border-radius: var(--nad-radius);
}
.dash__panel-title {
  font-weight: 650;
  font-size: 14px;
  color: var(--nad-ink);
}
.dash__panel :deep(.el-card__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
