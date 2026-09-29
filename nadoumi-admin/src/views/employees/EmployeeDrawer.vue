<script setup lang="ts">
import { computed, provide, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Drawer from '@/components/ui/Drawer.vue'
import { useDrawerForm } from '@/composables/useDrawerForm'
import {
  getEmployee, createEmployee, updateEmployee, type EmployeeInput,
} from '@/api/hr'
import {
  listUsers, listPosts, userDeptTree, listRoles,
  type SysPost, type SysDept, type SysRole, type SysUserRow,
} from '@/api/system'
import AccountSection from './form/AccountSection.vue'
import EmploymentSection from './form/EmploymentSection.vue'
import CompensationSection from './form/CompensationSection.vue'
import OtherSection from './form/OtherSection.vue'
import {
  blankEmployeeForm, employeeFormKey, formFromEmployee, mapDeptTree, type DeptOption,
} from './employeeForm'

const PICK_LIST_SIZE = 200

const props = defineProps<{ modelValue: boolean, employeeId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()
const { t } = useI18n()

const isEdit = computed(() => props.employeeId != null)
const compensationVisible = ref(true)
const positions = ref<SysPost[]>([])
const managers = ref<SysUserRow[]>([])
const roles = ref<SysRole[]>([])
const deptTree = ref<DeptOption[]>([])

const rules = {
  userName: [{ required: true, min: 2, max: 20, trigger: 'blur', message: t('employees.userNameRule') }],
  password: [{ required: true, min: 5, max: 20, trigger: 'blur', message: t('employees.pwdRule') }],
  nickName: [{ required: true, trigger: 'blur', message: t('common.required') }],
  email: [{ type: 'email' as const, trigger: 'blur', message: t('employees.emailInvalid') }],
  startDate: [{ required: true, trigger: 'change', message: t('common.required') }],
}

const { formRef, form, loading, saving, save, reset } = useDrawerForm<EmployeeInput>({
  isOpen: () => props.modelValue,
  blank: blankEmployeeForm,
  load: async (form) => {
    const [posts, mgrs, roleRes, treeRes] = await Promise.all([
      listPosts({ pageNum: 1, pageSize: PICK_LIST_SIZE }),
      listUsers({ userType: '00', pageNum: 1, pageSize: PICK_LIST_SIZE }),
      listRoles({ pageNum: 1, pageSize: PICK_LIST_SIZE }),
      userDeptTree(),
    ])
    positions.value = posts.rows
    managers.value = mgrs.rows
    roles.value = roleRes.rows.filter(r => !r.admin)
    deptTree.value = mapDeptTree((treeRes as unknown as { data: SysDept[] }).data)

    // new hires get the baseline "Staff" role by default so they aren't locked out
    if (!isEdit.value) {
      const staff = roles.value.find(r => r.roleKey === 'staff')
      if (staff) form.roleIds = [staff.roleId]
    }

    if (isEdit.value) {
      const e = await getEmployee(props.employeeId!)
      compensationVisible.value = e.compensationVisible
      Object.assign(form, formFromEmployee(e))
    }
  },
  submit: async (form) => {
    const body: EmployeeInput = { ...form }
    if (isEdit.value) await updateEmployee(props.employeeId!, body)
    else await createEmployee(body)
  },
  onSaved: () => emit('saved'),
})

provide(employeeFormKey, form)
</script>

<template>
  <Drawer
    :model-value="modelValue"
    :title="t(isEdit ? 'employees.edit' : 'employees.new')"
    :saving="saving"
    size="560"
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
      label-width="150px"
    >
      <AccountSection
        :roles="roles"
        :is-edit="isEdit"
      />
      <EmploymentSection
        :positions="positions"
        :managers="managers"
        :dept-tree="deptTree"
      />
      <CompensationSection v-if="compensationVisible" />
      <OtherSection />
    </el-form>
  </Drawer>
</template>
