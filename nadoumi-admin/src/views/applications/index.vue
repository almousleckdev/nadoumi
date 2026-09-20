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
      <el-input-number
        v-model="filters.assigneeUserId"
        :min="1"
        :controls="false"
        :placeholder="t('applications.assigneeId')"
        style="width: 150px"
        @change="reload"
      />
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
      @row-click="(r) => (openId = (r as Application).id)"
    >
      <template #cell-applicationType="{ row }">
        {{ typeLabel((row as Application).applicationType) }}
      </template>
      <template #cell-currentStatus="{ row }">
        <StatusBadge
          :status="(row as Application).currentStatus"
          :map="STATUS_TONES"
          :label="statusLabel((row as Application).currentStatus)"
        />
      </template>
      <template #cell-assigneeUserId="{ row }">
        {{ (row as Application).assigneeUserId ?? t('applications.unassigned') }}
      </template>
    </DataTable>

    <Pagination
      v-model:page="page"
      v-model:size="size"
      :total="total"
      @change="load"
    />

    <ApplicationDrawer
      v-model="drawerOpen"
      :application-id="openId"
      @changed="load"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import Pagination from '@/components/ui/Pagination.vue'
import ApplicationDrawer from './ApplicationDrawer.vue'
import { usePagedList } from '@/composables/usePagedList'
import { listApplications, type Application } from '@/api/application'
import { APPLICATION_TYPES, STATUSES, STATUS_TONES } from './vocabulary'

const { t, te } = useI18n()

const emptyFilters = () => ({
  q: '', applicationType: '', status: '', assigneeUserId: undefined as number | undefined,
})
const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<Application, ReturnType<typeof emptyFilters>>({
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

const openId = ref<number | undefined>()
const drawerOpen = ref(false)
watch(openId, (id) => { drawerOpen.value = id !== undefined })
watch(drawerOpen, (open) => { if (!open) openId.value = undefined })

const columns = computed(() => [
  { prop: 'id', label: t('applications.ref'), width: 90 },
  { prop: 'applicantId', label: t('applications.applicant'), width: 110 },
  { prop: 'applicationType', label: t('applications.type'), minWidth: 210 },
  { prop: 'currentStageName', label: t('applications.stage'), minWidth: 190 },
  { prop: 'currentStatus', label: t('applications.status'), width: 200, align: 'center' as const },
  { prop: 'assigneeUserId', label: t('applications.assignee'), width: 130 },
  { prop: 'submittedAt', label: t('applications.submitted'), minWidth: 170 },
])

const typeLabel = (type: string | null) =>
  type && te(`applications.typeMap.${type}`) ? t(`applications.typeMap.${type}`) : (type ?? '')
const statusLabel = (status: string | null) =>
  status && te(`applications.statusMap.${status}`) ? t(`applications.statusMap.${status}`) : (status ?? '')

onMounted(load)
</script>
