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
      <div
        v-if="hideableColumns.length > 1"
        class="dt__toolbar"
      >
        <el-dropdown
          trigger="click"
          :hide-on-click="false"
        >
          <el-button
            size="small"
            :icon="Operation"
          >
            {{ t('table.columns') }}
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="col in hideableColumns"
                :key="col.prop"
              >
                <el-checkbox
                  :model-value="!hidden.has(col.prop)"
                  @change="(v) => toggle(col.prop, Boolean(v))"
                >
                  {{ col.label }}
                </el-checkbox>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        :row-key="rowKey"
        class="dt__table"
        :row-class-name="clickableRows ? 'dt__row--clickable' : ''"
        @row-click="onRowClick"
        @sort-change="onSortChange"
      >
        <el-table-column
          v-for="col in visibleColumns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :align="col.align || 'left'"
          :sortable="col.sortable ? 'custom' : false"
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
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Operation } from '@element-plus/icons-vue'
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
  /** persist column visibility per screen */
  storageKey?: string
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
  storageKey: undefined,
})

const emit = defineEmits<{
  'update:page': [page: number]
  'retry': []
  'row-click': [row: Row]
  'sort-change': [sort: { prop: string, order: 'asc' | 'desc' | null }]
}>()

const { t } = useI18n()

// ---- column visibility ----
const hideableColumns = computed(() => props.columns.filter(c => c.prop && c.prop !== 'actions'))
const lsKey = computed(() => (props.storageKey ? `nad.dt.cols.${props.storageKey}` : ''))

function loadHidden(): Set<string> {
  if (!lsKey.value) return new Set()
  try {
    const raw = localStorage.getItem(lsKey.value)
    return raw ? new Set<string>(JSON.parse(raw)) : new Set()
  }
  catch {
    return new Set()
  }
}
const hidden = ref<Set<string>>(loadHidden())

function toggle(prop: string, visible: boolean) {
  const next = new Set(hidden.value)
  if (visible) next.delete(prop)
  else next.add(prop)
  hidden.value = next
}
watch(hidden, (v) => {
  if (!lsKey.value) return
  try {
    localStorage.setItem(lsKey.value, JSON.stringify([...v]))
  }
  catch { /* private mode — visibility just won't persist */ }
})

const visibleColumns = computed(() => props.columns.filter(c => !hidden.value.has(c.prop)))

function onRowClick(row: Row) {
  if (props.clickableRows) emit('row-click', row)
}

function onSortChange(e: { prop: string | null, order: 'ascending' | 'descending' | null }) {
  if (!e.prop) return
  emit('sort-change', {
    prop: e.prop,
    order: e.order === 'ascending' ? 'asc' : e.order === 'descending' ? 'desc' : null,
  })
}

function format(v: unknown): string {
  if (v === null || v === undefined || v === '') return '—'
  return String(v)
}
</script>

<style scoped>
.dt__toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}
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
