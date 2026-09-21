<template>
  <div class="nad-page">
    <PageHeader
      :title="t('support.title')"
      :subtitle="t('support.subtitle')"
    />

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <el-select
        v-model="filters.status"
        :placeholder="t('support.status')"
        clearable
        style="width: 170px"
        @change="reload"
      >
        <el-option
          v-for="s in TICKET_STATUSES"
          :key="s"
          :label="t(`support.statusMap.${s}`)"
          :value="s"
        />
      </el-select>
      <el-select
        v-model="filters.priority"
        :placeholder="t('support.priority')"
        clearable
        style="width: 140px"
        @change="reload"
      >
        <el-option
          v-for="p in TICKET_PRIORITIES"
          :key="p"
          :label="t(`support.priorityMap.${p}`)"
          :value="p"
        />
      </el-select>
      <el-select
        v-model="filters.category"
        :placeholder="t('support.category')"
        clearable
        style="width: 160px"
        @change="reload"
      >
        <el-option
          v-for="c in TICKET_CATEGORIES"
          :key="c"
          :label="t(`support.categoryMap.${c}`)"
          :value="c"
        />
      </el-select>
      <el-select
        v-model="filters.assigneeId"
        :placeholder="t('support.assignee')"
        clearable
        filterable
        style="width: 200px"
        @change="reload"
      >
        <el-option
          v-for="m in staff"
          :key="m.userId"
          :label="`${m.nickName} (${m.userName})`"
          :value="m.userId"
        />
      </el-select>
    </FilterBar>

    <DataTable
      :rows="rows"
      :columns="columns"
      :loading="loading"
      :error="error"
      row-key="id"
      :empty-title="t('support.emptyTitle')"
      clickable-rows
      @retry="load"
      @row-click="(r) => openDetail((r as StaffTicketSummary).id)"
    >
      <template #cell-priority="{ row }">
        <StatusBadge
          :status="priorityTone((row as StaffTicketSummary).priority)"
          :label="t(`support.priorityMap.${(row as StaffTicketSummary).priority}`)"
        />
      </template>
      <template #cell-status="{ row }">
        <StatusBadge
          :status="statusTone((row as StaffTicketSummary).status)"
          :label="t(`support.statusMap.${(row as StaffTicketSummary).status}`)"
        />
      </template>
      <template #cell-category="{ row }">
        {{ t(`support.categoryMap.${(row as StaffTicketSummary).category}`) }}
      </template>
      <template #cell-assignedStaffId="{ row }">
        {{ staffName((row as StaffTicketSummary).assignedStaffId) || t('support.unassigned') }}
      </template>
    </DataTable>

    <Pagination
      v-model:page="page"
      v-model:size="size"
      :total="total"
      @change="load"
    />

    <SupportTicketDrawer
      v-model="drawerOpen"
      :ticket-id="selectedId"
      :staff="staff"
      @changed="load"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import Pagination from '@/components/ui/Pagination.vue'
import SupportTicketDrawer from './SupportTicketDrawer.vue'
import { usePagedList } from '@/composables/usePagedList'
import {
  listTickets, TICKET_CATEGORIES, TICKET_PRIORITIES, TICKET_STATUSES,
  type StaffTicketSummary, type TicketCategory, type TicketPriority, type TicketStatus,
} from '@/api/support'
import { listUsers, type SysUserRow } from '@/api/system'
import { priorityTone, statusTone } from './ticketWorkflow'

const { t } = useI18n()

interface Filters {
  status: TicketStatus | ''
  priority: TicketPriority | ''
  category: TicketCategory | ''
  assigneeId: number | ''
}
const emptyFilters = (): Filters => ({ status: '', priority: '', category: '', assigneeId: '' })

// The queue is a plain array with no total, so a full page means "there may be another one".
const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<StaffTicketSummary, Filters>({
    emptyFilters,
    firstPage: 1,
    size: 20,
    fetch: async (f, { index, size: pageSize }) => {
      const content = await listTickets({
        status: f.status || undefined,
        priority: f.priority || undefined,
        category: f.category || undefined,
        assigneeId: f.assigneeId === '' ? undefined : f.assigneeId,
        page: index,
        size: pageSize,
      })
      const before = index * pageSize
      return { content, totalElements: before + content.length + (content.length === pageSize ? 1 : 0) }
    },
  })

const staff = ref<SysUserRow[]>([])
const staffName = (id: number | null) => {
  const m = staff.value.find(s => s.userId === id)
  return m ? m.nickName : ''
}

const drawerOpen = ref(false)
const selectedId = ref<number | null>(null)

const columns = computed(() => [
  { prop: 'subject', label: t('support.subject'), minWidth: 240 },
  { prop: 'category', label: t('support.category'), width: 150 },
  { prop: 'priority', label: t('support.priority'), width: 110, align: 'center' as const },
  { prop: 'status', label: t('support.status'), width: 170, align: 'center' as const },
  { prop: 'assignedStaffId', label: t('support.assignee'), minWidth: 150 },
  { prop: 'updateTime', label: t('support.updated'), width: 170 },
])

function openDetail(id: number) {
  selectedId.value = id
  drawerOpen.value = true
}

onMounted(async () => {
  load()
  try {
    staff.value = (await listUsers({ userType: '00', pageNum: 1, pageSize: 200 })).rows
  }
  catch {
    // The picker just stays empty; the queue itself is unaffected.
    staff.value = []
  }
})
</script>
