<template>
  <el-card
    shadow="never"
    class="ra"
  >
    <template #header>
      <div class="ra__head">
        <span>{{ t('dashboard.recentApplicants') }}</span>
        <el-button
          link
          type="primary"
          @click="router.push('/nadoumi/applicant')"
        >
          {{ t('dashboard.viewAll') }}
        </el-button>
      </div>
    </template>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
      show-icon
    />
    <el-empty
      v-else-if="!loading && rows.length === 0"
      :description="t('dashboard.noApplicants')"
      :image-size="72"
    />
    <el-table
      v-else
      v-loading="loading"
      :data="rows"
      :show-header="rows.length > 0"
    >
      <el-table-column :label="t('dashboard.name')">
        <template #default="{ row }">
          {{ row.givenName }} {{ row.familyName }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('dashboard.nationality')"
        width="120"
        prop="nationality"
      />
      <el-table-column
        :label="t('dashboard.status')"
        width="120"
      >
        <template #default="{ row }">
          <el-tag
            :type="statusType(row.status)"
            size="small"
            disable-transitions
          >
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('dashboard.registered')"
        width="170"
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

defineProps<{ rows: ApplicantRow[]; loading: boolean; error: string | null }>()

const { t } = useI18n()
const router = useRouter()

function statusType(s: string): 'success' | 'info' | 'warning' | 'danger' {
  return s === 'ACTIVE' ? 'success' : s === 'DRAFT' ? 'info' : s === 'UNLINKED' ? 'warning' : 'danger'
}
function fmtDate(v: string | null): string {
  if (!v) return '—'
  const d = new Date(v.replace(' ', 'T'))
  return Number.isNaN(d.getTime()) ? v : d.toLocaleString()
}
</script>

<style scoped>
.ra__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
