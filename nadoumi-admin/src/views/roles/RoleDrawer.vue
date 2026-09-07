<template>
  <Drawer
    :model-value="modelValue"
    :title="t(isEdit ? 'roles.edit' : 'roles.new')"
    :saving="saving"
    size="520"
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
      label-width="130px"
    >
      <el-form-item
        :label="t('roles.name')"
        prop="roleName"
      >
        <el-input v-model="form.roleName" />
      </el-form-item>
      <el-form-item
        :label="t('roles.key')"
        prop="roleKey"
      >
        <el-input
          v-model="form.roleKey"
          :disabled="isEdit"
        />
      </el-form-item>
      <el-form-item :label="t('roles.sort')">
        <el-input-number
          v-model="form.roleSort"
          :min="0"
        />
      </el-form-item>
      <el-form-item :label="t('roles.dataScope')">
        <el-select
          v-model="form.dataScope"
          style="width: 100%"
        >
          <el-option
            v-for="s in ['1', '2', '3', '4', '5']"
            :key="s"
            :label="t(`roles.scope.${s}`)"
            :value="s"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('roles.statusLabel')">
        <el-switch
          v-model="form.status"
          active-value="0"
          inactive-value="1"
        />
      </el-form-item>
      <el-form-item :label="t('roles.menus')">
        <div class="role-tree">
          <el-checkbox v-model="form.menuCheckStrictly">
            {{ t('roles.checkStrictly') }}
          </el-checkbox>
          <el-tree
            ref="treeRef"
            :data="menuTree"
            :props="{ label: 'label', children: 'children' }"
            show-checkbox
            node-key="id"
            :check-strictly="!form.menuCheckStrictly"
            :default-checked-keys="checkedKeys"
            style="max-height: 320px; overflow: auto"
          />
        </div>
      </el-form-item>
      <el-form-item :label="t('roles.remark')">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
        />
      </el-form-item>
    </el-form>
  </Drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import type { ElTree } from 'element-plus'
import Drawer from '@/components/ui/Drawer.vue'
import {
  getRole, createRole, updateRole, roleMenuTreeselect, menuTreeselect,
  type SysMenu, type SysRole,
} from '@/api/system'

const props = defineProps<{ modelValue: boolean, roleId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()
const { t } = useI18n()

const isEdit = computed(() => props.roleId != null)
const formRef = ref<FormInstance>()
const treeRef = ref<InstanceType<typeof ElTree>>()
const loading = ref(false)
const saving = ref(false)
const menuTree = ref<Array<{ id: number, label: string, children?: unknown[] }>>([])
const checkedKeys = ref<number[]>([])

interface RoleForm {
  roleId?: number
  roleName: string
  roleKey: string
  roleSort: number
  dataScope: string
  status: string
  menuCheckStrictly: boolean
  remark: string
}
const blank = (): RoleForm => ({
  roleName: '', roleKey: '', roleSort: 0, dataScope: '1', status: '0',
  menuCheckStrictly: true, remark: '',
})
const form = reactive<RoleForm>(blank())

const rules = {
  roleName: [{ required: true, trigger: 'blur', message: t('common.required') }],
  roleKey: [{ required: true, trigger: 'blur', message: t('common.required') }],
}

function mapTree(nodes: SysMenu[]): Array<{ id: number, label: string, children?: unknown[] }> {
  return (nodes || []).map(n => ({
    id: (n as unknown as { id?: number }).id ?? n.menuId,
    label: (n as unknown as { label?: string }).label ?? n.menuName,
    children: n.children ? mapTree(n.children) : undefined,
  }))
}

async function open() {
  Object.assign(form, blank())
  checkedKeys.value = []
  loading.value = true
  try {
    if (isEdit.value) {
      const [detail, tree] = await Promise.all([getRole(props.roleId!), roleMenuTreeselect(props.roleId!)])
      const r = detail.data
      Object.assign(form, {
        roleId: r.roleId, roleName: r.roleName, roleKey: r.roleKey, roleSort: r.roleSort,
        dataScope: r.dataScope, status: r.status,
        menuCheckStrictly: r.menuCheckStrictly ?? true, remark: r.remark ?? '',
      })
      menuTree.value = mapTree((tree as unknown as { menus: SysMenu[] }).menus)
      checkedKeys.value = (tree as unknown as { checkedKeys: number[] }).checkedKeys || []
    }
    else {
      const tree = await menuTreeselect()
      menuTree.value = mapTree((tree as unknown as { data: SysMenu[] }).data)
    }
  }
  finally {
    loading.value = false
  }
}

function collectMenuIds(): number[] {
  const checked = (treeRef.value?.getCheckedKeys(false) ?? []) as number[]
  const half = (treeRef.value?.getHalfCheckedKeys() ?? []) as number[]
  return [...checked, ...half]
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const body: Partial<SysRole> = {
      roleId: form.roleId,
      roleName: form.roleName,
      roleKey: form.roleKey,
      roleSort: form.roleSort,
      dataScope: form.dataScope,
      status: form.status,
      menuCheckStrictly: form.menuCheckStrictly,
      remark: form.remark,
      menuIds: collectMenuIds(),
    }
    if (isEdit.value) await updateRole(body)
    else await createRole(body)
    ElMessage.success(t('common.saved'))
    emit('saved')
  }
  finally {
    saving.value = false
  }
}

function reset() {
  Object.assign(form, blank())
  checkedKeys.value = []
}

watch(() => props.modelValue, (o) => { if (o) open() })
</script>

<style scoped>
.role-tree {
  width: 100%;
  border: 1px solid var(--nad-line, #e2e8f0);
  border-radius: 8px;
  padding: 8px 10px;
}
</style>
