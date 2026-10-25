<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useApplicationLabels } from '@/views/applications/useApplicationLabels'
import EmptyState from '@/components/ui/EmptyState.vue'
import { STATUSES } from '@/views/applications/vocabulary'

const props = defineProps<{ perStatus: Record<string, number>, loading: boolean }>()

const { t } = useI18n()
const router = useRouter()
const { statusLabel } = useApplicationLabels()

const rows = computed(() => STATUSES.map(status => ({ status, count: props.perStatus[status] ?? 0 })))
const total = computed(() => rows.value.reduce((sum, r) => sum + r.count, 0))
const max = computed(() => Math.max(1, ...rows.value.map(r => r.count)))
</script>

<template>
  <el-card
    shadow="never"
    class="pf"
  >
    <template #header>
      <span class="pf__title">{{ t('dashboard.pipelineTitle') }}</span>
      <el-button
        link
        type="primary"
        @click="router.push('/applications')"
      >
        {{ t('dashboard.viewAll') }}
      </el-button>
    </template>
    <el-skeleton
      v-if="loading && !total"
      :rows="4"
      animated
    />
    <EmptyState
      v-else-if="!total"
      :title="t('dashboard.noApplications')"
      icon="Document"
    />
    <ol
      v-else
      class="pf__list"
    >
      <li
        v-for="r in rows"
        :key="r.status"
        class="pf__row"
        data-test="funnel-row"
      >
        <span class="pf__label">{{ statusLabel(r.status) }}</span>
        <span class="pf__bar"><span
          class="pf__fill"
          :class="{ 'pf__fill--bad': r.status === 'CLOSED_UNSUCCESSFUL', 'pf__fill--good': r.status === 'CLOSED_SUCCESS' }"
          :style="{ width: r.count ? `${Math.max(3, (r.count / max) * 100)}%` : '0%' }"
        /></span>
        <span class="pf__count">{{ r.count }}</span>
      </li>
    </ol>
  </el-card>
</template>

<style scoped>
.pf { border: 1px solid var(--nad-line); border-radius: var(--nad-radius); }
.pf :deep(.el-card__header) { display: flex; align-items: center; justify-content: space-between; }
.pf__title { font-weight: 650; font-size: 14px; color: var(--nad-ink); }
.pf__list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.pf__row { display: grid; grid-template-columns: minmax(120px, 190px) 1fr 36px; align-items: center; gap: 12px; font-size: 13px; }
.pf__bar { height: 10px; background: var(--nad-line, #e5e7eb); border-radius: 5px; overflow: hidden; }
.pf__fill { display: block; height: 100%; background: var(--el-color-primary, #4f46e5); border-radius: 5px; }
.pf__fill--good { background: var(--el-color-success, #16a34a); }
.pf__fill--bad { background: var(--el-color-danger, #dc2626); }
.pf__count { text-align: end; font-weight: 650; font-variant-numeric: tabular-nums; }
</style>
