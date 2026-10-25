<template>
  <div class="nad-page">
    <PageHeader
      :title="t('roles.title')"
      :subtitle="t('roles.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('system:role:add')"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          {{ t('roles.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.roleName"
        :placeholder="t('roles.searchPlaceholder')"
        clearable
        style="width: 240px"
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
      row-key="roleId"
      :empty-title="t('roles.emptyTitle')"
      @retry="load"
    >
      <template #cell-roleName="{ row }">
        <strong>{{ (row as SysRole).roleName }}</strong>
        <div class="role__key">
          {{ (row as SysRole).roleKey }}
        </div>
      </template>
      <template #cell-members="{ row }">
        {{ members('roles', (row as SysRole).roleId) }}
      </template>
      <template #cell-dataScope="{ row }">
        {{ t(`roles.scope.${(row as SysRole).dataScope}`) }}
      </template>
      <template #cell-status="{ row }">
        <el-switch
          :model-value="(row as SysRole).status === '0'"
          :disabled="!userStore.hasPerm('system:role:edit') || (row as SysRole).admin"
          @change="(v) => toggleStatus(row as SysRole, Boolean(v))"
        />
      </template>
      <template #cell-actions="{ row }">
        <template v-if="!(row as SysRole).admin">
          <el-button
            v-if="userStore.hasPerm('system:role:edit')"
            link
            type="primary"
            @click="openEdit(row as SysRole)"
          >
            {{ t('common.edit') }}
          </el-button>
          <el-button
            v-if="userStore.hasPerm('system:role:add')"
            link
            type="primary"
            data-test="copy-role"
            @click="openCopy(row as SysRole)"
          >
            {{ t('roles.duplicate') }}
          </el-button>
          <el-button
            v-if="userStore.hasPerm('system:role:remove')"
            link
            type="danger"
            @click="doDelete(row as SysRole)"
          >
            {{ t('common.delete') }}
          </el-button>
        </template>
        <span
          v-else
          class="nad-muted"
        >{{ t('roles.builtin') }}</span>
      </template>
    </DataTable>

    <Pagination
      v-model:page="page"
      v-model:size="size"
      :total="total"
      @change="load"
    />

    <RoleDrawer
      v-model="drawerOpen"
      :role-id="editingId"
      :copy-from-id="copyFromId"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import RoleDrawer from './RoleDrawer.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import { listRoles, deleteRoles, changeRoleStatus, type SysRole } from '@/api/system'
import { usePagedList } from '@/composables/usePagedList'
import { useOrgOverview } from '@/composables/useOrgOverview'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const { rows, total, loading, error, filters, page, size, dirty, load, reload, clearFilters } =
  usePagedList<SysRole, { roleName: string }>({
    emptyFilters: () => ({ roleName: '' }),
    firstPage: 1,
    size: 10,
    fetch: (f, { page, size }) => listRoles({
      roleName: f.roleName || undefined,
      pageNum: page,
      pageSize: size,
    }),
  })

const drawerOpen = ref(false)
const editingId = ref<number | undefined>()
const copyFromId = ref<number | undefined>()
const { load: loadOrg, members } = useOrgOverview()

const columns = computed(() => [
  { prop: 'roleName', label: t('roles.name'), minWidth: 220 },
  { prop: 'members', label: t('roles.members'), width: 100, align: 'center' as const },
  { prop: 'dataScope', label: t('roles.dataScope'), minWidth: 170 },
  { prop: 'status', label: t('roles.status'), width: 90, align: 'center' as const },
  { prop: 'actions', label: t('common.actions'), width: 210, align: 'right' as const },
])

function openCreate() { editingId.value = undefined; copyFromId.value = undefined; drawerOpen.value = true }
function openEdit(row: SysRole) { editingId.value = row.roleId; copyFromId.value = undefined; drawerOpen.value = true }
function openCopy(row: SysRole) { editingId.value = undefined; copyFromId.value = row.roleId; drawerOpen.value = true }
function onSaved() { drawerOpen.value = false; load(); loadOrg() }

async function toggleStatus(row: SysRole, active: boolean) {
  const next = active ? '0' : '1'
  try {
    await changeRoleStatus(row.roleId, next)
    row.status = next
    ElMessage.success(t('common.saved'))
  }
  catch { /* toast in request.ts */ }
}

async function doDelete(row: SysRole) {
  if (!(await confirm({
    title: t('roles.deleteTitle'),
    message: t('roles.deleteConfirm', { name: row.roleName }),
    tone: 'danger',
  }))) return
  await deleteRoles([row.roleId])
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(() => { load(); loadOrg() })
</script>

<style scoped>
.role__key { font-size: 12px; color: var(--nad-ink-faint, #9ca3af); }
</style>
