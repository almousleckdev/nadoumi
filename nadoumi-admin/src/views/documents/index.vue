<template>
  <div class="nad-page">
    <PageHeader
      :title="t('documents.title')"
      :subtitle="t('documents.subtitle')"
    />

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <el-select
        v-model="filters.status"
        :placeholder="t('documents.status')"
        clearable
        style="width: 200px"
        @change="reload"
      >
        <el-option
          v-for="s in STATUSES"
          :key="s"
          :label="t(`documents.statusMap.${s}`)"
          :value="s"
        />
      </el-select>
      <el-select
        v-model="filters.docType"
        :placeholder="t('documents.type')"
        clearable
        filterable
        style="width: 220px"
        @change="reload"
      >
        <el-option
          v-for="o in typeOptions"
          :key="o.value"
          :label="o.label"
          :value="o.value"
        />
      </el-select>
      <el-input
        v-model="filters.applicantId"
        :placeholder="t('documents.applicantId')"
        inputmode="numeric"
        clearable
        style="width: 160px"
        @keyup.enter="reload"
        @clear="reload"
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
      :empty-title="t('documents.emptyTitle')"
      :empty-description="t('documents.emptyDescription')"
      clickable-rows
      @retry="load"
      @row-click="(r) => openDetail(r as StaffDocument)"
    >
      <template #cell-docType="{ row }">
        {{ typeLabel(row.docType) }}
      </template>
      <template #cell-applicantId="{ row }">
        #{{ row.applicantId }}
      </template>
      <template #cell-status="{ row }">
        <StatusBadge
          :status="row.status"
          :label="t(`documents.statusMap.${row.status}`, row.status)"
        />
      </template>
      <template #cell-versions="{ row }">
        {{ row.versions.length }}
      </template>
    </DataTable>

    <Pagination
      v-model:page="page"
      v-model:size="size"
      :total="total"
      @change="load"
    />

    <DocumentDrawer
      v-model="drawerOpen"
      :doc="selected"
      :type-label="selected ? typeLabel(selected.docType) : ''"
      @changed="onChanged"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import DocumentDrawer from './DocumentDrawer.vue'
import {
  getStaffDocument, listDocumentTypes, listStaffDocuments,
  type DocumentLifecycle, type DocumentTypeEntry, type StaffDocument,
} from '@/api/document'
import { usePagedList } from '@/composables/usePagedList'

const { t } = useI18n()

const STATUSES: DocumentLifecycle[] = ['DRAFT', 'SUBMITTED', 'IN_REVIEW', 'VERIFIED', 'REJECTED', 'EXPIRED']

const typeEntries = ref<DocumentTypeEntry[]>([])
const typeOptions = computed(() => typeEntries.value.map(e => ({ value: e.dictValue, label: e.dictLabel })))
const typeLabel = (code: string) => typeEntries.value.find(e => e.dictValue === code)?.dictLabel ?? code

const emptyFilters = () => ({ status: '', docType: '', applicantId: '' })

// The endpoint returns every match unpaged and only filters by applicant, so status/type
// filtering and paging happen here, over the one response.
const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<StaffDocument, ReturnType<typeof emptyFilters>>({
    emptyFilters,
    firstPage: 1,
    size: 20,
    fetch: async (f, { index, size: pageSize }) => {
      const applicantId = Number(f.applicantId)
      const all = await listStaffDocuments(Number.isInteger(applicantId) && applicantId > 0 ? { applicantId } : {})
      const matching = all.filter(d => (!f.status || d.status === f.status) && (!f.docType || d.docType === f.docType))
      return { content: matching.slice(index * pageSize, (index + 1) * pageSize), totalElements: matching.length }
    },
  })

const columns = computed(() => [
  { prop: 'docType', label: t('documents.type'), minWidth: 180 },
  { prop: 'applicantId', label: t('documents.applicant'), width: 110 },
  { prop: 'status', label: t('documents.status'), width: 170 },
  { prop: 'versions', label: t('documents.versions'), width: 100, align: 'center' as const },
])

const selected = ref<StaffDocument | null>(null)
const drawerOpen = ref(false)

function openDetail(row: StaffDocument) {
  selected.value = row
  drawerOpen.value = true
}

async function onChanged() {
  const id = selected.value?.id
  if (id) selected.value = await getStaffDocument(id)
  await load()
}

onMounted(async () => {
  // Best effort: without the dictionary the screen still works, labels fall back to the raw code.
  listDocumentTypes().then((res) => { typeEntries.value = res.data ?? [] }).catch(() => {})
  await load()
})
</script>
