<template>
  <Drawer
    :model-value="modelValue"
    :title="t(isEdit ? 'departments.edit' : 'departments.new')"
    :saving="saving"
    size="440"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
    @save="save"
    @closed="reset"
  >
    <el-form
      v-if="modelValue"
      ref="formRef"
      v-loading="loading"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item :label="t('departments.parent')">
        <el-tree-select
          v-model="form.parentId"
          :data="parentTree"
          :props="{ label: 'label', children: 'children' }"
          value-key="id"
          node-key="id"
          check-strictly
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item
        :label="t('departments.name')"
        prop="deptName"
      >
        <el-input v-model="form.deptName" />
      </el-form-item>
      <el-form-item :label="t('departments.order')">
        <el-input-number
          v-model="form.orderNum"
          :min="0"
        />
      </el-form-item>
      <el-form-item :label="t('departments.leader')">
        <el-input v-model="form.leader" />
      </el-form-item>
      <el-form-item :label="t('departments.phone')">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item :label="t('departments.email')">
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item :label="t('departments.statusLabel')">
        <el-switch
          v-model="form.status"
          active-value="0"
          inactive-value="1"
        />
      </el-form-item>
    </el-form>
  </Drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import Drawer from '@/components/ui/Drawer.vue'
import {
  getDept, createDept, updateDept, deptTreeExcludeChild, listDepts, type SysDept,
} from '@/api/system'

const props = defineProps<{ modelValue: boolean, deptId?: number, parentId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()
const { t } = useI18n()

const isEdit = computed(() => props.deptId != null)
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const parentTree = ref<Array<{ id: number, label: string, children?: unknown[] }>>([])

type DeptForm = Partial<SysDept> & { parentId: number }
const blank = (): DeptForm => ({
  parentId: props.parentId ?? 0,
  deptName: '', orderNum: 0, leader: '', phone: '', email: '', status: '0',
})
const form = reactive<DeptForm>(blank())
const rules = { deptName: [{ required: true, trigger: 'blur', message: t('common.required') }] }

function toNodes(flat: SysDept[]): SysDept[] {
  const byId = new Map<number, SysDept & { children: SysDept[] }>()
  flat.forEach(d => byId.set(d.deptId, { ...d, children: [] }))
  const roots: SysDept[] = []
  byId.forEach((d) => {
    const p = byId.get(d.parentId)
    if (p) p.children.push(d)
    else roots.push(d)
  })
  return roots
}
function mapTree(nodes: SysDept[]): Array<{ id: number, label: string, children?: unknown[] }> {
  return (nodes || []).map(n => ({
    id: n.deptId, label: n.deptName, children: n.children ? mapTree(n.children) : undefined,
  }))
}

async function open() {
  Object.assign(form, blank())
  loading.value = true
  try {
    const flat = isEdit.value
      ? (await deptTreeExcludeChild(props.deptId!)).data
      : (await listDepts()).data
    parentTree.value = [{ id: 0, label: t('departments.root'), children: mapTree(toNodes(flat)) }]
    if (isEdit.value) {
      const res = await getDept(props.deptId!)
      Object.assign(form, res.data)
    }
  }
  finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value) await updateDept(form)
    else await createDept(form)
    ElMessage.success(t('common.saved'))
    emit('saved')
  }
  finally {
    saving.value = false
  }
}

function reset() { Object.assign(form, blank()) }
watch(() => props.modelValue, (o) => { if (o) open() })
</script>
