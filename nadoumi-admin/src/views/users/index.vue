<template>
  <div class="nad-page">
    <PageHeader
      :title="t(isStaff ? 'users.staffTitle' : 'users.studentTitle')"
      :subtitle="t(isStaff ? 'users.staffSubtitle' : 'users.studentSubtitle')"
    >
      <template #actions>
        <el-button
          v-if="canEdit"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          {{ t(isStaff ? 'users.newStaff' : 'users.newStudent') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="dirty"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.userName"
        :placeholder="t('users.searchPlaceholder')"
        clearable
        style="width: 240px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select
        v-model="filters.status"
        :placeholder="t('users.status')"
        clearable
        style="width: 150px"
        @change="reload"
      >
        <el-option
          :label="t('users.statusActive')"
          value="0"
        />
        <el-option
          :label="t('users.statusDisabled')"
          value="1"
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
      row-key="userId"
      :empty-title="t('users.emptyTitle')"
      @retry="reload"
    >
      <template #cell-status="{ row }">
        <el-switch
          :model-value="(row as SysUserRow).status === '0'"
          :disabled="!canEdit || (row as SysUserRow).userId === 1"
          @change="(v) => toggleStatus(row as SysUserRow, Boolean(v))"
        />
      </template>
      <template #cell-actions="{ row }">
        <el-button
          link
          type="primary"
          @click="openEdit(row as SysUserRow)"
        >
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="canEdit"
          link
          type="primary"
          @click="doResetPwd(row as SysUserRow)"
        >
          {{ t('users.resetPwd') }}
        </el-button>
        <el-button
          v-if="canEdit && (row as SysUserRow).userId !== 1"
          link
          type="danger"
          @click="doDelete(row as SysUserRow)"
        >
          {{ t('common.delete') }}
        </el-button>
      </template>
    </DataTable>

    <Pagination
      v-model:page="filters.pageNum"
      v-model:size="filters.pageSize"
      :total="total"
      @change="load"
    />

    <UserDrawer
      v-model="drawerOpen"
      :user-id="editingId"
      :user-type="userType"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import UserDrawer from './UserDrawer.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import {
  listUsers, deleteUsers, resetUserPwd, changeUserStatus, type SysUserRow,
} from '@/api/system'

const { t } = useI18n()
const route = useRoute()
const { confirm } = useConfirm()
const userStore = useUserStore()

const userType = computed(() => (route.meta.userType as string) || '00')
const isStaff = computed(() => userType.value === '00')
const canEdit = computed(() => userStore.hasPerm('system:user:add') || userStore.hasPerm('system:user:edit'))

const rows = ref<SysUserRow[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)
const drawerOpen = ref(false)
const editingId = ref<number | undefined>()

const filters = reactive({ userName: '', status: '', pageNum: 1, pageSize: 10 })
const dirty = computed(() => Boolean(filters.userName || filters.status))

const columns = computed(() => [
  { prop: 'userName', label: t('users.userName'), minWidth: 130 },
  { prop: 'nickName', label: t('users.nickName'), minWidth: 130 },
  { prop: 'email', label: t('users.email'), minWidth: 160 },
  { prop: 'phonenumber', label: t('users.phone'), width: 140 },
  { prop: 'status', label: t('users.status'), width: 90, align: 'center' as const },
  { prop: 'createTime', label: t('users.created'), width: 170 },
  { prop: 'actions', label: t('common.actions'), width: 210, align: 'right' as const },
])

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await listUsers({
      userType: userType.value,
      userName: filters.userName || undefined,
      status: filters.status || undefined,
      pageNum: filters.pageNum,
      pageSize: filters.pageSize,
    })
    rows.value = res.rows
    total.value = res.total
  }
  catch (e) {
    error.value = (e as Error)?.message || 'Could not load'
  }
  finally {
    loading.value = false
  }
}

function reload() {
  filters.pageNum = 1
  load()
}

function clearFilters() {
  filters.userName = ''
  filters.status = ''
  reload()
}

function openCreate() {
  editingId.value = undefined
  drawerOpen.value = true
}
function openEdit(row: SysUserRow) {
  editingId.value = row.userId
  drawerOpen.value = true
}
function onSaved() {
  drawerOpen.value = false
  load()
}

async function toggleStatus(row: SysUserRow, active: boolean) {
  const next = active ? '0' : '1'
  try {
    await changeUserStatus(row.userId, next)
    row.status = next
    ElMessage.success(t('common.saved'))
  }
  catch {
    /* toast shown by request.ts */
  }
}

async function doResetPwd(row: SysUserRow) {
  try {
    const { value } = await ElMessageBox.prompt(
      t('users.resetPwdPrompt', { name: row.userName }), t('users.resetPwd'),
      { inputType: 'password', inputPattern: /^.{5,20}$/, inputErrorMessage: t('users.pwdRule') },
    )
    await resetUserPwd(row.userId, value)
    ElMessage.success(t('users.pwdReset'))
  }
  catch {
    /* cancelled */
  }
}

async function doDelete(row: SysUserRow) {
  if (!(await confirm({
    title: t('users.deleteTitle'),
    message: t('users.deleteConfirm', { name: row.userName }),
    tone: 'danger',
  }))) return
  await deleteUsers([row.userId])
  ElMessage.success(t('common.deleted'))
  load()
}

watch(userType, reload, { immediate: true })
</script>
