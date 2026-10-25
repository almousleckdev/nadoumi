<template>
  <div class="nad-page">
    <PageHeader
      :title="t('applications.title')"
      :subtitle="t('applications.subtitle')"
    />

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.q"
        :placeholder="t('applications.searchPlaceholder')"
        clearable
        style="width: 220px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select
        v-model="filters.applicationType"
        :placeholder="t('applications.type')"
        clearable
        style="width: 220px"
        @change="reload"
      >
        <el-option
          v-for="ty in APPLICATION_TYPES"
          :key="ty"
          :label="t(`applications.typeMap.${ty}`)"
          :value="ty"
        />
      </el-select>
      <el-select
        v-model="filters.status"
        :placeholder="t('applications.status')"
        clearable
        style="width: 220px"
        @change="reload"
      >
        <el-option
          v-for="s in STATUSES"
          :key="s"
          :label="t(`applications.statusMap.${s}`)"
          :value="s"
        />
      </el-select>
      <el-select
        v-model="filters.assigneeUserId"
        :placeholder="t('applications.assignee')"
        clearable
        filterable
        style="width: 220px"
        @change="reload"
      >
        <el-option
          v-for="s in staff"
          :key="s.userId"
          :label="s.name"
          :value="s.userId"
        />
      </el-select>
      <el-button
        :icon="Search"
        @click="reload"
      >
        {{ t('common.search') }}
      </el-button>
    </FilterBar>

    <DataTable
      :rows="rows"
      :columns="columns"
      :loading="loading"
      :error="error"
      row-key="id"
      :empty-title="t('applications.emptyTitle')"
      clickable-rows
      @retry="load"
      @row-click="(r) => router.push(`/applications/${(r as ApplicationRow).id}`)"
    >
      <template #cell-opportunityTitle="{ row }">
        <div class="opp">
          <span>{{ (row as ApplicationRow).opportunityTitle }}</span>
          <small v-if="(row as ApplicationRow).universityName">{{ (row as ApplicationRow).universityName }}</small>
        </div>
      </template>
      <template #cell-currentStatus="{ row }">
        <StatusBadge
          :status="(row as ApplicationRow).currentStatus"
          :map="STATUS_TONES"
          :label="statusLabel((row as ApplicationRow).currentStatus)"
        />
      </template>
      <template #cell-assigneeName="{ row }">
        {{ (row as ApplicationRow).assigneeName ?? t('applications.unassigned') }}
      </template>
      <template #cell-submittedAt="{ row }">
        {{ formatDateTime((row as ApplicationRow).submittedAt) }}
      </template>
      <template #cell-actions="{ row }">
        <el-button
          link
          type="primary"
          @click.stop="router.push(`/applications/${(row as ApplicationRow).id}`)"
        >
          {{ t('applications.view') }}
        </el-button>
        <el-button
          v-if="canDelete"
          link
          type="danger"
          data-test="delete-row"
          @click.stop="askDelete(row as ApplicationRow)"
        >
          {{ t('common.delete') }}
        </el-button>
      </template>
    </DataTable>

    <Pagination
      v-model:page="page"
      v-model:size="size"
      :total="total"
      @change="load"
    />

    <DeleteWithReasonDialog
      v-model="deleteOpen"
      :title="t('applications.deleteTitle')"
      :message="t('applications.deleteBody', { ref: target?.reference ?? '' })"
      :busy="deleting"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import Pagination from '@/components/ui/Pagination.vue'
import DeleteWithReasonDialog from '@/components/ui/DeleteWithReasonDialog.vue'
import { ElMessage } from 'element-plus'
import { usePagedList } from '@/composables/usePagedList'
import { deleteApplication, listApplications, listAssignees, type ApplicationRow, type StaffUser } from '@/api/application'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/date'
import { APPLICATION_TYPES, STATUSES, STATUS_TONES } from './vocabulary'

const { t, te } = useI18n()
const router = useRouter()
const staff = ref<StaffUser[]>([])

const emptyFilters = () => ({
  q: '', applicationType: '', status: '', assigneeUserId: undefined as number | undefined,
})
const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<ApplicationRow, ReturnType<typeof emptyFilters>>({
    emptyFilters,
    firstPage: 0,
    size: 20,
    fetch: (f, { index, size }) => listApplications({
      q: f.q || undefined,
      applicationType: f.applicationType || undefined,
      status: f.status || undefined,
      assigneeUserId: f.assigneeUserId || undefined,
      page: index,
      size,
    }),
  })

const columns = computed(() => [
  { prop: 'reference', label: t('applications.ref'), width: 160 },
  { prop: 'applicantName', label: t('applications.applicant'), minWidth: 170 },
  { prop: 'opportunityTitle', label: t('applications.opportunity'), minWidth: 240 },
  { prop: 'currentStageName', label: t('applications.stage'), minWidth: 170 },
  { prop: 'currentStatus', label: t('applications.status'), width: 180, align: 'center' as const },
  { prop: 'assigneeName', label: t('applications.assignee'), minWidth: 150 },
  { prop: 'submittedAt', label: t('applications.submitted'), minWidth: 170 },
  { prop: 'actions', label: t('common.actions'), width: 130, align: 'right' as const },
])

const statusLabel = (status: string | null) =>
  status && te(`applications.statusMap.${status}`) ? t(`applications.statusMap.${status}`) : (status ?? '')

const canDelete = computed(() => useUserStore().hasPerm('nad:application:remove'))
const deleteOpen = ref(false)
const deleting = ref(false)
const target = ref<ApplicationRow | null>(null)

function askDelete(row: ApplicationRow) {
  target.value = row
  deleteOpen.value = true
}

async function doDelete(reason: string) {
  if (!target.value) return
  deleting.value = true
  try {
    await deleteApplication(target.value.id, reason)
    ElMessage.success(t('applications.deleted'))
    deleteOpen.value = false
    await load()
  }
  finally {
    deleting.value = false
  }
}

// the assignee filter lists staff only for those allowed to assign; everyone else filters by the table
onMounted(async () => {
  load()
  if (useUserStore().hasPerm('nad:application:assign')) staff.value = await listAssignees().catch(() => [])
})
</script>

<style scoped>
.opp { display: grid; gap: 2px; }
.opp small { color: var(--nad-ink-soft, #64748b); }
</style>
