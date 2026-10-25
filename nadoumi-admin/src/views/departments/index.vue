<template>
  <div class="nad-page">
    <PageHeader
      :title="t('departments.title')"
      :subtitle="t('departments.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('system:dept:add')"
          type="primary"
          :icon="Plus"
          @click="openCreate()"
        >
          {{ t('departments.new') }}
        </el-button>
      </template>
    </PageHeader>

    <ErrorState
      v-if="error"
      :message="error"
      @retry="load"
    />
    <el-table
      v-else
      v-loading="loading"
      :data="tree"
      row-key="deptId"
      :tree-props="{ children: 'children' }"
      default-expand-all
    >
      <el-table-column
        :label="t('departments.name')"
        prop="deptName"
        min-width="220"
      />
      <el-table-column
        :label="t('departments.leader')"
        min-width="170"
      >
        <template #default="{ row }">
          {{ row.leader }}
          <div
            v-if="row.phone"
            class="dept__sub"
          >
            {{ row.phone }}
          </div>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('departments.members')"
        width="120"
        align="center"
      >
        <template #default="{ row }">
          <span data-test="dept-members">{{ totalMembers(row as SysDept) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('departments.status')"
        width="120"
        align="center"
      >
        <template #default="{ row }">
          <StatusBadge :status="row.status === '0' ? 'ACTIVE' : 'CLOSED'" />
        </template>
      </el-table-column>
      <el-table-column
        :label="t('common.actions')"
        width="270"
        align="right"
        class-name="dept__actions"
      >
        <template #default="{ row }">
          <el-button
            v-if="userStore.hasPerm('system:dept:add')"
            link
            type="primary"
            @click="openCreate(row.deptId)"
          >
            {{ t('departments.addChild') }}
          </el-button>
          <el-button
            v-if="userStore.hasPerm('system:dept:edit')"
            link
            type="primary"
            @click="openEdit(row.deptId)"
          >
            {{ t('common.edit') }}
          </el-button>
          <el-button
            v-if="userStore.hasPerm('system:dept:remove')"
            link
            type="danger"
            @click="doDelete(row as SysDept)"
          >
            {{ t('common.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <DeptDrawer
      v-model="drawerOpen"
      :dept-id="editingId"
      :parent-id="parentId"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import DeptDrawer from './DeptDrawer.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import { listDepts, deleteDept, type SysDept } from '@/api/system'
import { useOrgOverview } from '@/composables/useOrgOverview'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const tree = ref<SysDept[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const drawerOpen = ref(false)
const editingId = ref<number | undefined>()
const parentId = ref<number | undefined>()
const { load: loadOrg, members } = useOrgOverview()

function buildTree(flat: SysDept[]): SysDept[] {
  const byId = new Map<number, SysDept & { children: SysDept[] }>()
  flat.forEach(d => byId.set(d.deptId, { ...d, children: [] }))
  const roots: SysDept[] = []
  byId.forEach((d) => {
    const parent = byId.get(d.parentId)
    if (parent) parent.children.push(d)
    else roots.push(d)
  })
  return roots
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await listDepts()
    tree.value = buildTree(res.data)
  }
  catch (e) {
    error.value = (e as Error)?.message || 'Could not load'
  }
  finally {
    loading.value = false
  }
}
function openCreate(pid?: number) { editingId.value = undefined; parentId.value = pid ?? 0; drawerOpen.value = true }
function openEdit(id: number) { editingId.value = id; parentId.value = undefined; drawerOpen.value = true }
function onSaved() { drawerOpen.value = false; load(); loadOrg() }

/** People in the department and everything under it, so a parent shows its whole team. */
function totalMembers(dept: SysDept): number {
  return members('departments', dept.deptId) + (dept.children ?? []).reduce((sum, child) => sum + totalMembers(child), 0)
}

async function doDelete(row: SysDept) {
  if (row.children?.length) {
    ElMessage.warning(t('departments.hasChildren'))
    return
  }
  if (!(await confirm({
    title: t('departments.deleteTitle'),
    message: t('departments.deleteConfirm', { name: row.deptName }),
    tone: 'danger',
  }))) return
  await deleteDept(row.deptId)
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(() => { load(); loadOrg() })
</script>

<style scoped>
.dept__sub { font-size: 12px; color: var(--nad-ink-faint, #9ca3af); }
:deep(.dept__actions .cell) { white-space: nowrap; }
</style>
