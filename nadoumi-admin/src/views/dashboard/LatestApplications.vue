<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import type { ApplicationRow } from '@/api/application'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { formatDateTime } from '@/utils/date'
import { useApplicationLabels } from '@/views/applications/useApplicationLabels'
import { STATUS_TONES } from '@/views/applications/vocabulary'

defineProps<{ rows: ApplicationRow[], loading: boolean }>()

const { t } = useI18n()
const router = useRouter()
const { statusLabel } = useApplicationLabels()
</script>

<template>
  <el-card
    shadow="never"
    class="latest"
  >
    <template #header>
      <span class="latest__title">{{ t('dashboard.latestApplications') }}</span>
      <el-button
        link
        type="primary"
        @click="router.push('/applications')"
      >
        {{ t('dashboard.viewAll') }}
      </el-button>
    </template>
    <el-table
      v-if="rows.length || loading"
      v-loading="loading && !rows.length"
      :data="rows"
      row-key="id"
      class="latest__table"
      @row-click="(r: ApplicationRow) => router.push(`/applications/${r.id}`)"
    >
      <el-table-column
        :label="t('applications.applicant')"
        min-width="150"
      >
        <template #default="{ row }">
          <strong>{{ row.applicantName }}</strong>
          <div class="latest__sub">
            {{ row.reference }}
          </div>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('applications.opportunity')"
        min-width="200"
      >
        <template #default="{ row }">
          {{ row.opportunityTitle }}
          <div
            v-if="row.universityName"
            class="latest__sub"
          >
            {{ row.universityName }}
          </div>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('applications.status')"
        width="190"
      >
        <template #default="{ row }">
          <StatusBadge
            :status="row.currentStatus"
            :map="STATUS_TONES"
            :label="statusLabel(row.currentStatus)"
          />
        </template>
      </el-table-column>
      <el-table-column
        :label="t('applications.assignee')"
        min-width="140"
      >
        <template #default="{ row }">
          {{ row.assigneeName ?? t('applications.unassigned') }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('applications.submitted')"
        width="170"
      >
        <template #default="{ row }">
          {{ formatDateTime(row.submittedAt) }}
        </template>
      </el-table-column>
    </el-table>
    <EmptyState
      v-else
      :title="t('dashboard.noApplications')"
      icon="Document"
    />
  </el-card>
</template>

<style scoped>
.latest {
  border: 1px solid var(--nad-line);
  border-radius: var(--nad-radius);
  margin-bottom: 28px;
}
.latest :deep(.el-card__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.latest__title {
  font-weight: 650;
  font-size: 14px;
  color: var(--nad-ink);
}
.latest__sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.latest__table :deep(tr) {
  cursor: pointer;
}
</style>
