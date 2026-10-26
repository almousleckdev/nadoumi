<template>
  <div class="nad-page">
    <PageHeader
      :title="t('payment.title')"
      :subtitle="t('payment.subtitle')"
    />

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.q"
        :placeholder="t('payment.searchPlaceholder')"
        clearable
        style="width: 280px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select
        v-model="filters.kind"
        :placeholder="t('payment.kind')"
        clearable
        style="width: 190px"
        data-test="kind-filter"
        @change="reload"
      >
        <el-option
          v-for="k in PAYMENT_KINDS"
          :key="k"
          :label="t(`payment.kindMap.${k}`)"
          :value="k"
        />
      </el-select>
      <el-select
        v-model="filters.status"
        :placeholder="t('payment.status')"
        clearable
        style="width: 190px"
        @change="reload"
      >
        <el-option
          v-for="s in PAYMENT_STATUSES"
          :key="s"
          :label="t(`payment.statusMap.${s}`)"
          :value="s"
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
      clickable-rows
      :empty-title="t('payment.emptyTitle')"
      @retry="load"
      @row-click="(r: unknown) => openReview(r as StaffPayment)"
    >
      <template #cell-kind="{ row }">
        {{ t(`payment.kindMap.${(row as StaffPayment).kind}`, (row as StaffPayment).kind) }}
      </template>
      <template #cell-amount="{ row }">
        {{ cny((row as StaffPayment).amount) }}
      </template>
      <template #cell-status="{ row }">
        <StatusBadge
          :status="(row as StaffPayment).status"
          :map="PAYMENT_TONES"
          :label="t(`payment.statusMap.${(row as StaffPayment).status}`)"
        />
      </template>
      <template #cell-submittedAt="{ row }">
        {{ formatDateTime((row as StaffPayment).submittedAt) }}
      </template>
      <template #cell-actions="{ row }">
        <el-button
          link
          type="primary"
          @click.stop="openReview(row as StaffPayment)"
        >
          {{ t('payment.review') }}
        </el-button>
        <el-button
          v-if="canDelete && (row as StaffPayment).status !== 'CONFIRMED'"
          link
          type="danger"
          data-test="delete-row"
          @click.stop="askDelete(row as StaffPayment)"
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
      :title="t('payment.deleteTitle')"
      :message="t('payment.deleteBody', { ref: target?.reference ?? '' })"
      :busy="deleting"
      @confirm="doDelete"
    />
    <PaymentDrawer
      v-model="drawerOpen"
      :payment-id="selectedId"
      @decided="load"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import PaymentDrawer from './PaymentDrawer.vue'
import { PAYMENT_TONES } from './paymentTones'
import { usePagedList } from '@/composables/usePagedList'
import { cny } from '@/utils/money'
import { formatDateTime } from '@/utils/date'
import { deletePayment, listPayments, PAYMENT_KINDS, PAYMENT_STATUSES, type StaffPayment } from '@/api/payment'
import { useUserStore } from '@/stores/user'
import DeleteWithReasonDialog from '@/components/ui/DeleteWithReasonDialog.vue'
import { ElMessage } from 'element-plus'

const { t } = useI18n()

const emptyFilters = () => ({ q: '', status: '', kind: '' })
const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<StaffPayment, ReturnType<typeof emptyFilters>>({
    emptyFilters,
    firstPage: 0,
    size: 20,
    fetch: (f, { index, size }) => listPayments({
      q: f.q || undefined,
      status: f.status || undefined,
      kind: f.kind || undefined,
      page: index,
      size,
    }),
  })

const canDelete = computed(() => useUserStore().hasPerm('nad:payment:remove'))
const deleteOpen = ref(false)
const deleting = ref(false)
const target = ref<StaffPayment | null>(null)

function askDelete(p: StaffPayment) {
  target.value = p
  deleteOpen.value = true
}

async function doDelete(reason: string) {
  if (!target.value) return
  deleting.value = true
  try {
    await deletePayment(target.value.id, reason)
    ElMessage.success(t('payment.deleted'))
    deleteOpen.value = false
    await load()
  }
  finally {
    deleting.value = false
  }
}

const drawerOpen = ref(false)
const selectedId = ref<number | null>(null)

const columns = computed(() => [
  { prop: 'reference', label: t('payment.reference'), width: 150 },
  { prop: 'applicantName', label: t('payment.student'), minWidth: 170 },
  { prop: 'applicationRef', label: t('payment.application'), width: 120 },
  { prop: 'kind', label: t('payment.kind'), width: 150 },
  { prop: 'amount', label: t('payment.amount'), width: 110, align: 'right' as const },
  { prop: 'studentTxnNo', label: t('payment.txnNo'), minWidth: 160 },
  { prop: 'status', label: t('payment.status'), width: 170 },
  { prop: 'submittedAt', label: t('payment.submittedAt'), width: 170 },
  { prop: 'actions', label: t('common.actions'), width: 100, align: 'right' as const },
])

function openReview(p: StaffPayment) {
  selectedId.value = p.id
  drawerOpen.value = true
}

onMounted(load)
</script>
