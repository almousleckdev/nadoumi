<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import Drawer from '@/components/ui/Drawer.vue'
import DataTable from '@/components/ui/DataTable.vue'
import type { DataTableColumn } from '@/components/ui/types'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import {
  listDictData, getDictData, createDictData, updateDictData, deleteDictData,
  type SysDictType, type SysDictData,
} from '@/api/system'

const DATA_PAGE_SIZE = 100

const props = defineProps<{ type: SysDictType | null }>()

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const dataColumns: DataTableColumn[] = [
  { prop: 'dictSort', label: t('dict.sort'), width: 70, align: 'center' },
  { prop: 'dictLabel', label: t('dict.label'), minWidth: 120 },
  { prop: 'dictValue', label: t('dict.value'), minWidth: 120 },
  { prop: 'isDefault', label: t('dict.default'), width: 80, align: 'center' },
  { prop: 'actions', label: t('common.actions'), width: 120, align: 'right' },
]

const data = ref<SysDictData[]>([])
const dataLoading = ref(false)

async function loadData() {
  if (!props.type) return
  dataLoading.value = true
  try {
    const res = await listDictData({ dictType: props.type.dictType, pageNum: 1, pageSize: DATA_PAGE_SIZE })
    data.value = res.rows
  }
  finally {
    dataLoading.value = false
  }
}

watch(() => props.type, (type) => {
  if (type) loadData()
  else data.value = []
})

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
    const body = { ...dataForm, dictType: props.type?.dictType }
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
</script>

<template>
  <section class="dict__pane">
    <div class="dict__pane-head">
      <h3>{{ type ? t('dict.dataFor', { type: type.dictType }) : t('dict.data') }}</h3>
      <el-button
        v-if="type && userStore.hasPerm('system:dict:add')"
        size="small"
        type="primary"
        :icon="Plus"
        @click="openData()"
      >
        {{ t('dict.newData') }}
      </el-button>
    </div>
    <DataTable
      :rows="type ? data : []"
      :columns="dataColumns"
      :loading="dataLoading"
      row-key="dictCode"
      :empty-title="type ? t('dict.noEntries') : t('dict.pickType')"
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
</template>

<style scoped src="./dict.css" />
