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
      <FormSection :title="t('employees.secAccount')">
        <el-form-item
          v-if="!isEdit"
          :label="t('employees.userName')"
          prop="userName"
        >
          <el-input v-model="form.userName" />
        </el-form-item>
        <el-form-item
          v-if="!isEdit"
          :label="t('employees.password')"
          prop="password"
        >
          <el-input
            v-model="form.password"
            type="password"
            show-password
          />
        </el-form-item>
        <el-form-item
          :label="t('employees.displayName')"
          prop="nickName"
        >
          <el-input v-model="form.nickName" />
        </el-form-item>
        <el-form-item
          :label="t('employees.email')"
          prop="email"
        >
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item :label="t('employees.phone')">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item :label="t('employees.accountActive')">
          <el-switch
            v-model="form.userStatus"
            active-value="0"
            inactive-value="1"
          />
        </el-form-item>
        <el-form-item :label="t('employees.roles')">
          <el-select
            v-model="form.roleIds"
            multiple
            style="width: 100%"
            :placeholder="t('employees.rolesPlaceholder')"
          >
            <el-option
              v-for="r in roles"
              :key="r.roleId"
              :label="r.roleName"
              :value="r.roleId"
            />
          </el-select>
          <div class="nad-field-hint">
            {{ t('employees.rolesHint') }}
          </div>
        </el-form-item>
      </FormSection>

      <FormSection :title="t('employees.secEmployment')">
        <el-form-item :label="t('employees.position')">
          <el-select
            v-model="form.positionId"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="p in positions"
              :key="p.postId"
              :label="p.postName"
              :value="p.postId"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('employees.positionTitle')">
          <el-input
            v-model="form.positionTitle"
            :placeholder="t('employees.positionTitleHint')"
          />
        </el-form-item>
        <el-form-item :label="t('employees.dept')">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTree"
            :props="{ label: 'label', children: 'children' }"
            value-key="id"
            node-key="id"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('employees.manager')">
          <el-select
            v-model="form.managerUserId"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="m in managers"
              :key="m.userId"
              :label="`${m.nickName} (${m.userName})`"
              :value="m.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('employees.type')">
          <el-select
            v-model="form.employmentType"
            style="width: 100%"
          >
            <el-option
              v-for="ty in TYPES"
              :key="ty"
              :label="t(`employees.typeMap.${ty}`)"
              :value="ty"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('employees.status')">
          <el-select
            v-model="form.employmentStatus"
            style="width: 100%"
          >
            <el-option
              v-for="s in STATUSES"
              :key="s"
              :label="t(`employees.statusMap.${s}`)"
              :value="s"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="t('employees.startDate')"
          prop="startDate"
        >
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('employees.probationEnd')">
          <el-date-picker
            v-model="form.probationEndDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('employees.endDate')">
          <el-date-picker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('employees.workLocation')">
          <el-input v-model="form.workLocation" />
        </el-form-item>
      </FormSection>

      <FormSection
        v-if="compensationVisible"
        :title="t('employees.secCompensation')"
      >
        <el-form-item :label="t('employees.salary')">
          <el-input-number
            v-model="form.salaryAmount"
            :min="0"
            :controls="false"
            style="width: 160px"
          />
          <el-input
            v-model="form.salaryCurrency"
            :placeholder="t('employees.currency')"
            maxlength="3"
            style="width: 90px; margin-left: 8px"
          />
        </el-form-item>
        <el-form-item :label="t('employees.payFrequency')">
          <el-select
            v-model="form.payFrequency"
            style="width: 100%"
          >
            <el-option
              v-for="f in FREQS"
              :key="f"
              :label="t(`employees.freqMap.${f}`)"
              :value="f"
            />
          </el-select>
        </el-form-item>
      </FormSection>

      <FormSection :title="t('employees.secOther')">
        <div class="row2">
          <el-form-item :label="t('employees.emergencyContactName')">
            <el-input
              v-model="form.emergencyContact"
              maxlength="200"
            />
          </el-form-item>
          <el-form-item :label="t('employees.emergencyContactRelationship')">
            <el-input
              v-model="form.emergencyContactRelationship"
              maxlength="60"
              :placeholder="t('employees.emergencyContactRelationshipHint')"
            />
          </el-form-item>
        </div>
        <div class="row2">
          <el-form-item :label="t('employees.emergencyContactPhone')">
            <el-input
              v-model="form.emergencyContactPhone"
              maxlength="32"
            />
          </el-form-item>
          <el-form-item :label="t('employees.emergencyContactEmail')">
            <el-input
              v-model="form.emergencyContactEmail"
              maxlength="120"
              :placeholder="t('common.optional')"
            />
          </el-form-item>
        </div>
        <el-form-item :label="t('employees.notes')">
          <el-input
            v-model="form.notes"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
      </FormSection>
    </el-form>
  </Drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import Drawer from '@/components/ui/Drawer.vue'
import FormSection from '@/components/ui/FormSection.vue'
import {
  getEmployee, createEmployee, updateEmployee, type EmployeeInput,
} from '@/api/hr'
import {
  listUsers, listPosts, userDeptTree, listRoles,
  type SysPost, type SysDept, type SysRole, type SysUserRow,
} from '@/api/system'

const props = defineProps<{ modelValue: boolean, employeeId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()
const { t } = useI18n()

const TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN', 'TEMPORARY']
const STATUSES = ['PROBATION', 'ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED']
const FREQS = ['MONTHLY', 'ANNUAL', 'WEEKLY', 'HOURLY']

const isEdit = computed(() => props.employeeId != null)
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const compensationVisible = ref(true)
const positions = ref<SysPost[]>([])
const managers = ref<SysUserRow[]>([])
const roles = ref<SysRole[]>([])
const deptTree = ref<Array<{ id: number, label: string, children?: unknown[] }>>([])

const blank = (): EmployeeInput => ({
  userName: '', password: '', nickName: '', email: '', phone: '', userStatus: '0', roleIds: [],
  positionId: null, positionTitle: '', deptId: null, managerUserId: null,
  employmentType: 'FULL_TIME', employmentStatus: 'PROBATION',
  startDate: new Date().toISOString().slice(0, 10),
  probationEndDate: null, endDate: null, workLocation: '',
  salaryAmount: null, salaryCurrency: 'CNY', payFrequency: 'MONTHLY',
  emergencyContact: '', emergencyContactRelationship: '', emergencyContactPhone: '', emergencyContactEmail: '',
  notes: '',
})
const form = reactive<EmployeeInput>(blank())

const rules = {
  userName: [{ required: true, min: 2, max: 20, trigger: 'blur', message: t('employees.userNameRule') }],
  password: [{ required: true, min: 5, max: 20, trigger: 'blur', message: t('employees.pwdRule') }],
  nickName: [{ required: true, trigger: 'blur', message: t('common.required') }],
  email: [{ type: 'email' as const, trigger: 'blur', message: t('employees.emailInvalid') }],
  startDate: [{ required: true, trigger: 'change', message: t('common.required') }],
}

function mapTree(nodes: SysDept[]): Array<{ id: number, label: string, children?: unknown[] }> {
  return (nodes || []).map(n => ({
    id: (n as unknown as { id?: number }).id ?? n.deptId,
    label: (n as unknown as { label?: string }).label ?? n.deptName,
    children: n.children ? mapTree(n.children) : undefined,
  }))
}

async function open() {
  Object.assign(form, blank())
  loading.value = true
  try {
    const [posts, mgrs, roleRes, treeRes] = await Promise.all([
      listPosts({ pageNum: 1, pageSize: 200 }),
      listUsers({ userType: '00', pageNum: 1, pageSize: 200 }),
      listRoles({ pageNum: 1, pageSize: 200 }),
      userDeptTree(),
    ])
    positions.value = posts.rows
    managers.value = mgrs.rows
    roles.value = roleRes.rows.filter(r => !r.admin)
    deptTree.value = mapTree((treeRes as unknown as { data: SysDept[] }).data)

    // new hires get the baseline "Staff" role by default so they aren't locked out
    if (!isEdit.value) {
      const staff = roles.value.find(r => r.roleKey === 'staff')
      if (staff) form.roleIds = [staff.roleId]
    }

    if (isEdit.value) {
      const e = await getEmployee(props.employeeId!)
      compensationVisible.value = e.compensationVisible
      Object.assign(form, {
        nickName: e.nickName, email: e.email ?? '', phone: e.phone ?? '',
        userStatus: e.userStatus, roleIds: e.roleIds ?? [],
        positionId: e.positionId, positionTitle: e.positionTitle ?? '',
        deptId: e.deptId, managerUserId: e.managerUserId,
        employmentType: e.employmentType, employmentStatus: e.employmentStatus,
        startDate: e.startDate, probationEndDate: e.probationEndDate, endDate: e.endDate,
        workLocation: e.workLocation ?? '',
        salaryAmount: e.salaryAmount, salaryCurrency: e.salaryCurrency ?? 'CNY',
        payFrequency: e.payFrequency,
        emergencyContact: e.emergencyContact ?? '',
        emergencyContactRelationship: e.emergencyContactRelationship ?? '',
        emergencyContactPhone: e.emergencyContactPhone ?? '',
        emergencyContactEmail: e.emergencyContactEmail ?? '',
        notes: e.notes ?? '',
      })
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
    const body: EmployeeInput = { ...form }
    if (isEdit.value) await updateEmployee(props.employeeId!, body)
    else await createEmployee(body)
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

<style scoped>
.nad-field-hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--nad-ink-faint, #9ca3af);
}
.row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
@media (max-width: 640px) {
  .row2 { grid-template-columns: 1fr; }
}
</style>
