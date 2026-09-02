<template>
  <el-card
    shadow="never"
    class="ra"
  >
    <template #header>
      <div class="ra__head">
        <span class="ra__title">{{ t('dashboard.recentApplicants') }}</span>
        <el-button
          link
          type="primary"
          @click="router.push('/applicants')"
        >
          {{ t('dashboard.viewAll') }}
        </el-button>
      </div>
    </template>

    <ErrorState
      v-if="error"
      :message="error"
      :retryable="false"
    />
    <LoadingState
      v-else-if="loading && rows.length === 0"
      :rows="4"
    />
    <EmptyState
      v-else-if="rows.length === 0"
      :title="t('dashboard.noApplicants')"
      icon="User"
    />
    <el-table
      v-else
      :data="rows"
      class="ra__table"
      :row-class-name="() => 'ra__row'"
      @row-click="(row: Row) => router.push(`/applicants/${row.id}`)"
    >
      <el-table-column :label="t('dashboard.name')">
        <template #default="{ row }">
          <div class="ra__who">
            <Avatar
              :name="`${row.givenName} ${row.familyName}`"
              :size="26"
            />
            {{ row.givenName }} {{ row.familyName }}
          </div>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('dashboard.nationality')"
        width="110"
        prop="nationality"
        align="center"
      />
      <el-table-column
        :label="t('dashboard.status')"
        width="130"
      >
        <template #default="{ row }">
          <StatusBadge :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column
        :label="t('dashboard.registered')"
        width="160"
      >
        <template #default="{ row }">
          {{ fmtDate(row.createdAt) }}
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import type { ApplicantRow } from '@/api/applicant'
import Avatar from '@/components/ui/Avatar.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import LoadingState from '@/components/ui/LoadingState.vue'

type Row = ApplicantRow

defineProps<{ rows: ApplicantRow[], loading: boolean, error: string | null }>()

const { t } = useI18n()
const router = useRouter()

function fmtDate(v: string | null): string {
  if (!v) return '—'
  const d = new Date(v.replace(' ', 'T'))
  return Number.isNaN(d.getTime()) ? v : d.toLocaleDateString()
}
</script>

<style scoped>
.ra__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ra__title {
  font-weight: 650;
  font-size: 14px;
  color: var(--nad-ink);
}
.ra__who {
  display: flex;
  align-items: center;
  gap: 8px;
}
:deep(.ra__row) {
  cursor: pointer;
}
:deep(.ra__row:hover) td {
  background: var(--nad-brand-50);
}
</style>
