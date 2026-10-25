<template>
  <Drawer
    :model-value="modelValue"
    :title="t(isEdit ? 'roles.edit' : copying ? 'roles.copy' : 'roles.new')"
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
          <el-input
            v-model="filterText"
            :placeholder="t('roles.filterMenus')"
            clearable
            size="small"
            class="role-tree__filter"
            data-test="menu-filter"
            @input="treeRef?.filter(filterText)"
          />
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
            :filter-node-method="matchMenu"
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
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ElTree } from 'element-plus'
import Drawer from '@/components/ui/Drawer.vue'
import { useDrawerForm } from '@/composables/useDrawerForm'
import {
  getRole, createRole, updateRole, roleMenuTreeselect, menuTreeselect,
  type SysMenu, type SysRole,
} from '@/api/system'

/** `copyFromId` opens a new role pre-filled from an existing one (its permissions and scope), to be saved under a new key. */
const props = defineProps<{ modelValue: boolean, roleId?: number, copyFromId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()
const { t } = useI18n()

const isEdit = computed(() => props.roleId != null)
const copying = computed(() => !isEdit.value && props.copyFromId != null)
const filterText = ref('')
const treeRef = ref<InstanceType<typeof ElTree>>()
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

const { formRef, form, loading, saving, save, reset } = useDrawerForm<RoleForm>({
  isOpen: () => props.modelValue,
  blank,
  load: async (form) => {
    const sourceId = props.roleId ?? props.copyFromId
    if (sourceId != null) {
      const [detail, tree] = await Promise.all([getRole(sourceId), roleMenuTreeselect(sourceId)])
      const r = detail.data
      Object.assign(form, {
        roleId: isEdit.value ? r.roleId : undefined,
        roleName: copying.value ? t('roles.copyOf', { name: r.roleName }) : r.roleName,
        roleKey: copying.value ? '' : r.roleKey, roleSort: r.roleSort,
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
  },
  submit: async (form) => {
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
  },
  onSaved: () => emit('saved'),
  onReset: () => { checkedKeys.value = []; filterText.value = '' },
})

function matchMenu(value: string, data: { label?: string }): boolean {
  return !value || (data.label ?? '').toLowerCase().includes(value.toLowerCase())
}

function collectMenuIds(): number[] {
  const checked = (treeRef.value?.getCheckedKeys(false) ?? []) as number[]
  const half = (treeRef.value?.getHalfCheckedKeys() ?? []) as number[]
  return [...checked, ...half]
}
</script>

<style scoped>
.role-tree__filter { margin-bottom: 8px; }
.role-tree {
  width: 100%;
  border: 1px solid var(--nad-line, #e2e8f0);
  border-radius: 8px;
  padding: 8px 10px;
}
</style>
