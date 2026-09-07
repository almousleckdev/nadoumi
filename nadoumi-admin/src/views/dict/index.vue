<template>
  <div class="nad-page">
    <PageHeader
      :title="t('dict.title')"
      :subtitle="t('dict.subtitle')"
    />

    <div class="dict__split">
      <section class="dict__pane">
        <div class="dict__pane-head">
          <h3>{{ t('dict.types') }}</h3>
          <el-button
            v-if="userStore.hasPerm('system:dict:add')"
            size="small"
            type="primary"
            :icon="Plus"
            @click="openType()"
          >
            {{ t('dict.newType') }}
          </el-button>
        </div>
        <el-input
          v-model="typeQuery"
          :placeholder="t('dict.searchType')"
          clearable
          size="small"
          style="margin-bottom: 8px"
          @keyup.enter="loadTypes"
          @clear="loadTypes"
        />
        <DataTable
          :rows="types"
          :columns="typeColumns"
          :loading="typesLoading"
          row-key="dictId"
          clickable-rows
          :empty-title="t('dict.noTypes')"
          @row-click="(row) => selectType(row as SysDictType)"
        >
          <template #cell-dictType="{ row }">
            <span :class="{ 'dict__sel': selected && selected.dictId === (row as SysDictType).dictId }">
              {{ (row as SysDictType).dictType }}
            </span>
          </template>
          <template #cell-actions="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click.stop="openType((row as SysDictType).dictId)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              v-if="userStore.hasPerm('system:dict:remove')"
              link
              type="danger"
              size="small"
              @click.stop="removeType(row as SysDictType)"
            >
              {{ t('common.delete') }}
            </el-button>
          </template>
        </DataTable>
      </section>

      <section class="dict__pane">
        <div class="dict__pane-head">
          <h3>{{ selected ? t('dict.dataFor', { type: selected.dictType }) : t('dict.data') }}</h3>
          <el-button
            v-if="selected && userStore.hasPerm('system:dict:add')"
            size="small"
            type="primary"
            :icon="Plus"
            @click="openData()"
          >
            {{ t('dict.newData') }}
          </el-button>
        </div>
        <DataTable
          :rows="selected ? data : []"
          :columns="dataColumns"
          :loading="dataLoading"
          row-key="dictCode"
          :empty-title="selected ? t('dict.noEntries') : t('dict.pickType')"
        >
          <template #cell-isDefault="{ row }">
            <el-tag
              v-if="(row as SysDictData).isDefault === 'Y'"
              size="small"
            >
              {{ t('common.yes') }}
            </el-tag>
          </template>
          <template #cell-actions="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click.stop="openData((row as SysDictData).dictCode)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              v-if="userStore.hasPerm('system:dict:remove')"
              link
              type="danger"
              size="small"
              @click.stop="removeData(row as SysDictData)"
            >
              {{ t('common.delete') }}
            </el-button>
          </template>
        </DataTable>
      </section>
    </div>

    <Drawer
      v-model="typeDrawer"
      :title="t(editingType ? 'dict.editType' : 'dict.newType')"
      :saving="savingType"
      @save="saveType"
    >
      <el-form
        v-if="typeDrawer"
        ref="typeFormRef"
        :model="typeForm"
        :rules="typeRules"
        label-width="110px"
      >
        <el-form-item
          :label="t('dict.name')"
          prop="dictName"
        >
          <el-input v-model="typeForm.dictName" />
        </el-form-item>
        <el-form-item
          :label="t('dict.type')"
          prop="dictType"
        >
          <el-input v-model="typeForm.dictType" />
        </el-form-item>
        <el-form-item :label="t('dict.statusLabel')">
          <el-switch
            v-model="typeForm.status"
            active-value="0"
            inactive-value="1"
          />
        </el-form-item>
        <el-form-item :label="t('dict.remark')">
          <el-input
            v-model="typeForm.remark"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
      </el-form>
    </Drawer>

    <Drawer
      v-model="dataDrawer"
      :title="t(editingData ? 'dict.editData' : 'dict.newData')"
      :saving="savingData"
      @save="saveData"
    >
      <el-form
        v-if="dataDrawer"
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRules"
        label-width="110px"
      >
        <el-form-item
          :label="t('dict.label')"
          prop="dictLabel"
        >
          <el-input v-model="dataForm.dictLabel" />
        </el-form-item>
        <el-form-item
          :label="t('dict.value')"
          prop="dictValue"
        >
          <el-input v-model="dataForm.dictValue" />
        </el-form-item>
        <el-form-item :label="t('dict.sort')">
          <el-input-number
            v-model="dataForm.dictSort"
            :min="0"
          />
        </el-form-item>
        <el-form-item :label="t('dict.default')">
          <el-switch
            v-model="dataForm.isDefault"
            active-value="Y"
            inactive-value="N"
          />
        </el-form-item>
        <el-form-item :label="t('dict.statusLabel')">
          <el-switch
            v-model="dataForm.status"
            active-value="0"
            inactive-value="1"
          />
        </el-form-item>
        <el-form-item :label="t('dict.remark')">
          <el-input
            v-model="dataForm.remark"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
      </el-form>
    </Drawer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import Drawer from '@/components/ui/Drawer.vue'
import DataTable from '@/components/ui/DataTable.vue'
import type { DataTableColumn } from '@/components/ui/types'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import {
  listDictTypes, getDictType, createDictType, updateDictType, deleteDictTypes,
  listDictData, getDictData, createDictData, updateDictData, deleteDictData,
  type SysDictType, type SysDictData,
} from '@/api/system'

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const typeColumns: DataTableColumn[] = [
  { prop: 'dictName', label: t('dict.name'), minWidth: 120 },
  { prop: 'dictType', label: t('dict.type'), minWidth: 140 },
  { prop: 'actions', label: t('common.actions'), width: 120, align: 'right' },
]
const dataColumns: DataTableColumn[] = [
  { prop: 'dictSort', label: t('dict.sort'), width: 70, align: 'center' },
  { prop: 'dictLabel', label: t('dict.label'), minWidth: 120 },
  { prop: 'dictValue', label: t('dict.value'), minWidth: 120 },
  { prop: 'isDefault', label: t('dict.default'), width: 80, align: 'center' },
  { prop: 'actions', label: t('common.actions'), width: 120, align: 'right' },
]

const types = ref<SysDictType[]>([])
const typesLoading = ref(false)
const typeQuery = ref('')
const selected = ref<SysDictType | null>(null)

const data = ref<SysDictData[]>([])
const dataLoading = ref(false)

async function loadTypes() {
  typesLoading.value = true
  try {
    const res = await listDictTypes({ dictName: typeQuery.value || undefined, pageNum: 1, pageSize: 100 })
    types.value = res.rows
  }
  finally {
    typesLoading.value = false
  }
}
function selectType(row: SysDictType | null) {
  selected.value = row
  if (row) loadData()
}
async function loadData() {
  if (!selected.value) return
  dataLoading.value = true
  try {
    const res = await listDictData({ dictType: selected.value.dictType, pageNum: 1, pageSize: 100 })
    data.value = res.rows
  }
  finally {
    dataLoading.value = false
  }
}

// ---- type drawer ----
const typeDrawer = ref(false)
const savingType = ref(false)
const editingType = ref<number | undefined>()
const typeFormRef = ref<FormInstance>()
const typeBlank = () => ({ dictName: '', dictType: '', status: '0', remark: '' })
const typeForm = reactive<Partial<SysDictType>>(typeBlank())
const typeRules = {
  dictName: [{ required: true, trigger: 'blur', message: t('common.required') }],
  dictType: [{ required: true, trigger: 'blur', message: t('common.required') }],
}
async function openType(id?: number) {
  editingType.value = id
  Object.assign(typeForm, typeBlank())
  typeDrawer.value = true
  if (id != null) Object.assign(typeForm, (await getDictType(id)).data)
}
async function saveType() {
  await typeFormRef.value?.validate()
  savingType.value = true
  try {
    if (editingType.value != null) await updateDictType(typeForm)
    else await createDictType(typeForm)
    ElMessage.success(t('common.saved'))
    typeDrawer.value = false
    loadTypes()
  }
  finally {
    savingType.value = false
  }
}
async function removeType(row: SysDictType) {
  if (!(await confirm({ title: t('dict.deleteTypeTitle'), message: t('dict.deleteTypeConfirm', { name: row.dictName }), tone: 'danger' }))) return
  await deleteDictTypes([row.dictId])
  ElMessage.success(t('common.deleted'))
  if (selected.value?.dictId === row.dictId) selected.value = null
  loadTypes()
}

// ---- data drawer ----
const dataDrawer = ref(false)
const savingData = ref(false)
const editingData = ref<number | undefined>()
const dataFormRef = ref<FormInstance>()
const dataBlank = () => ({ dictLabel: '', dictValue: '', dictSort: 0, isDefault: 'N', status: '0', remark: '' })
const dataForm = reactive<Partial<SysDictData>>(dataBlank())
const dataRules = {
  dictLabel: [{ required: true, trigger: 'blur', message: t('common.required') }],
  dictValue: [{ required: true, trigger: 'blur', message: t('common.required') }],
}
async function openData(code?: number) {
  editingData.value = code
  Object.assign(dataForm, dataBlank())
  dataDrawer.value = true
  if (code != null) Object.assign(dataForm, (await getDictData(code)).data)
}
async function saveData() {
  await dataFormRef.value?.validate()
  savingData.value = true
  try {
    const body = { ...dataForm, dictType: selected.value?.dictType }
    if (editingData.value != null) await updateDictData(body)
    else await createDictData(body)
    ElMessage.success(t('common.saved'))
    dataDrawer.value = false
    loadData()
  }
  finally {
    savingData.value = false
  }
}
async function removeData(row: SysDictData) {
  if (!(await confirm({ title: t('dict.deleteDataTitle'), message: t('dict.deleteDataConfirm', { name: row.dictLabel }), tone: 'danger' }))) return
  await deleteDictData([row.dictCode])
  ElMessage.success(t('common.deleted'))
  loadData()
}

onMounted(loadTypes)
</script>

<style scoped>
.dict__split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.dict__pane-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.dict__pane-head h3 {
  margin: 0;
  font-size: 15px;
}
.dict__sel {
  font-weight: 700;
  color: var(--nad-brand-600, #4f46e5);
}
@media (max-width: 1000px) {
  .dict__split {
    grid-template-columns: 1fr;
  }
}
</style>
