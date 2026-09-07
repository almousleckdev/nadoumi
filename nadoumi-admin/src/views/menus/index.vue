<template>
  <div class="nad-page">
    <PageHeader
      :title="t('menus.title')"
      :subtitle="t('menus.subtitle')"
    >
      <template #actions>
        <el-input
          v-model="q"
          :placeholder="t('menus.searchPlaceholder')"
          clearable
          :prefix-icon="Search"
          style="width: 220px"
        />
        <el-button
          v-if="userStore.hasPerm('system:menu:add')"
          type="primary"
          :icon="Plus"
          @click="openCreate()"
        >
          {{ t('menus.new') }}
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
      :key="q ? 'filtered' : 'all'"
      v-loading="loading"
      :data="filteredTree"
      row-key="menuId"
      :tree-props="{ children: 'children' }"
      default-expand-all
    >
      <el-table-column
        :label="t('menus.name')"
        prop="menuName"
        min-width="200"
      />
      <el-table-column
        :label="t('menus.icon')"
        width="80"
        align="center"
      >
        <template #default="{ row }">
          <el-icon v-if="(row as SysMenu).icon && (row as SysMenu).icon !== '#'">
            <component :is="(row as SysMenu).icon" />
          </el-icon>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('menus.orderNum')"
        prop="orderNum"
        width="80"
        align="center"
      />
      <el-table-column
        :label="t('menus.perms')"
        prop="perms"
        min-width="180"
      />
      <el-table-column
        :label="t('menus.type')"
        width="90"
        align="center"
      >
        <template #default="{ row }">
          {{ t(`menus.typeMap.${row.menuType}`) }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('menus.visible')"
        width="90"
        align="center"
      >
        <template #default="{ row }">
          <StatusBadge
            :status="row.visible === '0' ? 'ACTIVE' : 'CLOSED'"
            :label="t(row.visible === '0' ? 'menus.shown' : 'menus.hidden')"
          />
        </template>
      </el-table-column>
      <el-table-column
        :label="t('common.actions')"
        width="200"
        align="right"
      >
        <template #default="{ row }">
          <el-button
            v-if="userStore.hasPerm('system:menu:add')"
            link
            type="primary"
            @click="openCreate(row.menuId)"
          >
            {{ t('menus.addChild') }}
          </el-button>
          <el-button
            link
            type="primary"
            @click="openEdit(row.menuId)"
          >
            {{ t('common.edit') }}
          </el-button>
          <el-button
            v-if="userStore.hasPerm('system:menu:remove')"
            link
            type="danger"
            @click="doDelete(row as SysMenu)"
          >
            {{ t('common.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <MenuDrawer
      v-model="drawerOpen"
      :menu-id="editingId"
      :parent-id="parentId"
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
import ErrorState from '@/components/ui/ErrorState.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import MenuDrawer from './MenuDrawer.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import { listMenus, deleteMenu, type SysMenu } from '@/api/system'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const tree = ref<SysMenu[]>([])
const q = ref('')
const loading = ref(false)

// keep a branch when it — or any descendant — matches the name / permission query
const filteredTree = computed<SysMenu[]>(() => {
  const term = q.value.trim().toLowerCase()
  if (!term) return tree.value
  const prune = (nodes: SysMenu[]): SysMenu[] =>
    nodes.reduce<SysMenu[]>((acc, n) => {
      const kids = n.children?.length ? prune(n.children) : []
      const hit = n.menuName.toLowerCase().includes(term)
        || (n.perms ?? '').toLowerCase().includes(term)
      if (hit || kids.length) acc.push({ ...n, children: kids })
      return acc
    }, [])
  return prune(tree.value)
})
const error = ref<string | null>(null)
const drawerOpen = ref(false)
const editingId = ref<number | undefined>()
const parentId = ref<number | undefined>()

function buildTree(flat: SysMenu[]): SysMenu[] {
  const byId = new Map<number, SysMenu & { children: SysMenu[] }>()
  flat.forEach(m => byId.set(m.menuId, { ...m, children: [] }))
  const roots: SysMenu[] = []
  byId.forEach((m) => {
    const parent = byId.get(m.parentId)
    if (parent) parent.children.push(m)
    else roots.push(m)
  })
  return roots
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await listMenus()
    tree.value = buildTree(res.data)
  }
  catch (e) {
    error.value = (e as Error)?.message || 'Could not load'
  }
  finally {
    loading.value = false
  }
}
function openCreate(pid?: number) { editingId.value = undefined; parentId.value = pid; drawerOpen.value = true }
function openEdit(id: number) { editingId.value = id; parentId.value = undefined; drawerOpen.value = true }
function onSaved() { drawerOpen.value = false; load() }

async function doDelete(row: SysMenu) {
  if (row.children?.length) {
    ElMessage.warning(t('menus.hasChildren'))
    return
  }
  if (!(await confirm({
    title: t('menus.deleteTitle'),
    message: t('menus.deleteConfirm', { name: row.menuName }),
    tone: 'danger',
  }))) return
  await deleteMenu(row.menuId)
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(load)
</script>
