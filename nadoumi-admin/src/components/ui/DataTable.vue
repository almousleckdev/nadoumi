<template>
  <div class="dt">
    <ErrorState
      v-if="error"
      :message="error"
      @retry="emit('retry')"
    />

    <LoadingState
      v-else-if="loading && rows.length === 0"
      :rows="6"
    />

    <template v-else-if="rows.length === 0">
      <slot name="empty">
        <EmptyState
          :title="emptyTitle || t('table.emptyTitle')"
          :description="emptyDescription"
          icon="Document"
        />
      </slot>
    </template>

    <template v-else>
      <el-table
        v-loading="loading"
        :data="rows"
        :row-key="rowKey"
        class="dt__table"
        :row-class-name="clickableRows ? 'dt__row--clickable' : ''"
        @row-click="onRowClick"
      >
        <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :align="col.align || 'left'"
          :show-overflow-tooltip="col.tooltip ?? true"
        >
          <template #default="{ row }">
            <slot
              :name="`cell-${col.prop}`"
              :row="row"
              :value="row[col.prop]"
            >
              {{ format(row[col.prop]) }}
            </slot>
          </template>
        </el-table-column>
        <slot name="columns" />
      </el-table>

      <div
        v-if="total > pageSize"
        class="dt__footer"
      >
        <span class="dt__count">{{ t('table.total', { n: total }) }}</span>
        <el-pagination
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="page + 1"
          @current-change="(p: number) => emit('update:page', p - 1)"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import ErrorState from './ErrorState.vue'
import LoadingState from './LoadingState.vue'
import EmptyState from './EmptyState.vue'
import type { DataTableColumn } from './types'

/** A generic table shell — rows are untyped here; each screen keeps its own typed array. */
type Row = Record<string, any>

const props = withDefaults(defineProps<{
  columns: DataTableColumn[]
  rows: Row[]
  rowKey?: string
  loading?: boolean
  error?: string | null
  total?: number
  page?: number
  pageSize?: number
  emptyTitle?: string
  emptyDescription?: string
  clickableRows?: boolean
}>(), {
  rowKey: 'id',
  loading: false,
  error: null,
  total: 0,
  page: 0,
  pageSize: 20,
  emptyTitle: undefined,
  emptyDescription: undefined,
  clickableRows: false,
})

const emit = defineEmits<{
  'update:page': [page: number]
  'retry': []
  'row-click': [row: Row]
}>()

const { t } = useI18n()

function onRowClick(row: Row) {
  if (props.clickableRows) emit('row-click', row)
}

function format(v: unknown): string {
  if (v === null || v === undefined || v === '') return '—'
  return String(v)
}
</script>

<style scoped>
.dt__table {
  width: 100%;
}
.dt__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
}
.dt__count {
  font-size: 13px;
  color: var(--nad-ink-faint);
}
:deep(.dt__row--clickable) {
  cursor: pointer;
}
:deep(.dt__row--clickable:hover) td {
  background: var(--nad-brand-50);
}
</style>
