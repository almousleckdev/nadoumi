<template>
  <div class="nad-page">
    <PageHeader
      :title="t('config.title')"
      :subtitle="t('config.subtitle')"
    >
      <template #actions>
        <el-button
          v-if="userStore.hasPerm('system:config:remove')"
          :icon="Refresh"
          @click="refreshCache"
        >
          {{ t('config.refreshCache') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('system:config:add')"
          type="primary"
          :icon="Plus"
          @click="open()"
        >
          {{ t('config.new') }}
        </el-button>
      </template>
    </PageHeader>

    <FilterBar
      :dirty="Boolean(filters.configName || filters.configKey)"
      @clear="clearFilters"
    >
      <el-input
        v-model="filters.configName"
        :placeholder="t('config.name')"
        clearable
        style="width: 200px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-input
        v-model="filters.configKey"
        :placeholder="t('config.key')"
        clearable
        style="width: 200px"
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
      row-key="configId"
      @retry="load"
    >
      <template #cell-configType="{ row }">
        {{ t(row.configType === 'Y' ? 'config.builtin' : 'config.custom') }}
      </template>
      <template #cell-actions="{ row }">
        <el-button
          link
          type="primary"
          @click="open(row.configId)"
        >
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="userStore.hasPerm('system:config:remove') && row.configType !== 'Y'"
          link
          type="danger"
          @click="doDelete(row as SysConfig)"
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

    <Drawer
      v-model="drawerOpen"
      :title="t(editing ? 'config.edit' : 'config.new')"
      :saving="saving"
      @save="save"
    >
      <el-form
        v-if="drawerOpen"
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="130px"
      >
        <el-form-item
          :label="t('config.name')"
          prop="configName"
        >
          <el-input v-model="form.configName" />
        </el-form-item>
        <el-form-item
          :label="t('config.key')"
          prop="configKey"
        >
          <el-input v-model="form.configKey" />
        </el-form-item>
        <el-form-item
          :label="t('config.value')"
          prop="configValue"
        >
          <el-input
            v-model="form.configValue"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
        <el-form-item :label="t('config.builtinLabel')">
          <el-switch
            v-model="form.configType"
            active-value="Y"
            inactive-value="N"
          />
        </el-form-item>
        <el-form-item :label="t('config.remark')">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
      </el-form>
    </Drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import Drawer from '@/components/ui/Drawer.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import {
  listConfigs, getConfig, createConfig, updateConfig, deleteConfigs, refreshConfigCache,
  type SysConfig,
} from '@/api/system'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const rows = ref<SysConfig[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)
const filters = reactive({ configName: '', configKey: '', pageNum: 1, pageSize: 10 })

const drawerOpen = ref(false)
const saving = ref(false)
const editing = ref<number | undefined>()
const formRef = ref<FormInstance>()
const blank = () => ({ configName: '', configKey: '', configValue: '', configType: 'N', remark: '' })
const form = reactive<Partial<SysConfig>>(blank())
const rules = {
  configName: [{ required: true, trigger: 'blur', message: t('common.required') }],
  configKey: [{ required: true, trigger: 'blur', message: t('common.required') }],
  configValue: [{ required: true, trigger: 'blur', message: t('common.required') }],
}

const columns = computed(() => [
  { prop: 'configName', label: t('config.name'), minWidth: 180 },
  { prop: 'configKey', label: t('config.key'), minWidth: 200 },
  { prop: 'configValue', label: t('config.value'), minWidth: 200 },
  { prop: 'configType', label: t('config.type'), width: 100, align: 'center' as const },
  { prop: 'actions', label: t('common.actions'), width: 150, align: 'right' as const },
])

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await listConfigs({
      configName: filters.configName || undefined,
      configKey: filters.configKey || undefined,
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
function reload() { filters.pageNum = 1; load() }
function clearFilters() { filters.configName = ''; filters.configKey = ''; reload() }

async function open(id?: number) {
  editing.value = id
  Object.assign(form, blank())
  drawerOpen.value = true
  if (id != null) Object.assign(form, (await getConfig(id)).data)
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editing.value != null) await updateConfig(form)
    else await createConfig(form)
    ElMessage.success(t('common.saved'))
    drawerOpen.value = false
    load()
  }
  finally {
    saving.value = false
  }
}

async function doDelete(row: SysConfig) {
  if (!(await confirm({
    title: t('config.deleteTitle'),
    message: t('config.deleteConfirm', { name: row.configName }),
    tone: 'danger',
  }))) return
  await deleteConfigs([row.configId])
  ElMessage.success(t('common.deleted'))
  load()
}

async function refreshCache() {
  await refreshConfigCache()
  ElMessage.success(t('config.cacheRefreshed'))
}

onMounted(load)
</script>
