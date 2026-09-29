<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import Drawer from '@/components/ui/Drawer.vue'
import DataTable from '@/components/ui/DataTable.vue'
import type { DataTableColumn } from '@/components/ui/types'
import { useConfirm } from '@/composables/useConfirm'
import { useUserStore } from '@/stores/user'
import {
  listDictTypes, getDictType, createDictType, updateDictType, deleteDictTypes,
  type SysDictType,
} from '@/api/system'

const TYPE_PAGE_SIZE = 100

const props = defineProps<{ selectedId: number | null }>()
const emit = defineEmits<{ select: [type: SysDictType | null] }>()

const { t } = useI18n()
const { confirm } = useConfirm()
const userStore = useUserStore()

const typeColumns: DataTableColumn[] = [
  { prop: 'dictName', label: t('dict.name'), minWidth: 120 },
  { prop: 'dictType', label: t('dict.type'), minWidth: 140 },
  { prop: 'actions', label: t('common.actions'), width: 120, align: 'right' },
]

const types = ref<SysDictType[]>([])
const typesLoading = ref(false)
const typeQuery = ref('')

async function loadTypes() {
  typesLoading.value = true
  try {
    const res = await listDictTypes({ dictName: typeQuery.value || undefined, pageNum: 1, pageSize: TYPE_PAGE_SIZE })
    types.value = res.rows
  }
  finally {
    typesLoading.value = false
  }
}

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
  if (props.selectedId === row.dictId) emit('select', null)
  loadTypes()
}

onMounted(loadTypes)
</script>

<template>
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
      @row-click="(row) => emit('select', row as SysDictType)"
    >
      <template #cell-dictType="{ row }">
        <span :class="{ 'dict__sel': selectedId === (row as SysDictType).dictId }">
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
</template>

<style scoped src="./dict.css" />
