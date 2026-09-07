<template>
  <Drawer
    :model-value="modelValue"
    :title="drawerTitle"
    :saving="saving"
    size="480"
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
        v-if="!isEdit"
        :label="t('users.userName')"
        prop="userName"
      >
        <el-input v-model="form.userName" />
      </el-form-item>
      <el-form-item
        v-if="!isEdit"
        :label="t('users.password')"
        prop="password"
      >
        <el-input
          v-model="form.password"
          type="password"
          show-password
        />
      </el-form-item>
      <el-form-item
        :label="t('users.nickName')"
        prop="nickName"
      >
        <el-input v-model="form.nickName" />
      </el-form-item>
      <el-form-item :label="t('users.dept')">
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
      <el-form-item
        :label="t('users.phone')"
        prop="phonenumber"
      >
        <el-input v-model="form.phonenumber" />
      </el-form-item>
      <el-form-item
        :label="t('users.email')"
        prop="email"
      >
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item :label="t('users.sex')">
        <el-radio-group v-model="form.sex">
          <el-radio value="0">
            {{ t('users.male') }}
          </el-radio>
          <el-radio value="1">
            {{ t('users.female') }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('users.statusLabel')">
        <el-switch
          v-model="form.status"
          active-value="0"
          inactive-value="1"
        />
      </el-form-item>
      <el-form-item
        v-if="isStaff"
        :label="t('users.postsField')"
      >
        <el-select
          v-model="form.postIds"
          multiple
          style="width: 100%"
        >
          <el-option
            v-for="p in posts"
            :key="p.postId"
            :label="p.postName"
            :value="p.postId"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        v-if="isStaff"
        :label="t('users.rolesField')"
      >
        <el-select
          v-model="form.roleIds"
          multiple
          style="width: 100%"
        >
          <el-option
            v-for="r in roles"
            :key="r.roleId"
            :label="r.roleName"
            :value="r.roleId"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('users.remark')">
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
import Drawer from '@/components/ui/Drawer.vue'
import {
  getUser, createUser, updateUser, userDeptTree,
  type SysRole, type SysPost, type SysDept, type SysUserForm,
} from '@/api/system'

const props = defineProps<{ modelValue: boolean, userId?: number, userType: string }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()

const { t } = useI18n()

const isEdit = computed(() => props.userId != null)
const isStaff = computed(() => props.userType === '00')
const drawerTitle = computed(() =>
  t(isEdit.value ? 'users.editTitle' : (isStaff.value ? 'users.newStaff' : 'users.newStudent')))

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const roles = ref<SysRole[]>([])
const posts = ref<SysPost[]>([])
const deptTree = ref<Array<{ id: number, label: string, children?: unknown[] }>>([])

const blank = (): SysUserForm => ({
  nickName: '', userName: '', password: '', email: '', phonenumber: '',
  sex: '0', status: '0', deptId: null, postIds: [], roleIds: [], remark: '',
  userType: props.userType,
})
const form = reactive<SysUserForm>(blank())

const rules = {
  userName: [{ required: true, min: 2, max: 20, trigger: 'blur', message: t('users.userNameRule') }],
  password: [{ required: true, min: 5, max: 20, trigger: 'blur', message: t('users.pwdRule') }],
  nickName: [{ required: true, trigger: 'blur', message: t('common.required') }],
  email: [{ type: 'email' as const, trigger: 'blur', message: t('users.emailInvalid') }],
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
    const res = await getUser(props.userId)
    roles.value = (res.roles || []).filter(r => !r.admin)
    posts.value = res.posts || []
    if (isEdit.value && res.data) {
      const u = res.data as unknown as SysUserForm & { roleIds?: number[], postIds?: number[] }
      Object.assign(form, {
        userId: props.userId,
        nickName: res.data.nickName,
        email: res.data.email,
        phonenumber: res.data.phonenumber,
        sex: res.data.sex || '0',
        status: res.data.status || '0',
        deptId: res.data.deptId ?? null,
        remark: (res.data as unknown as { remark?: string }).remark ?? '',
        roleIds: (res as unknown as { roleIds?: number[] }).roleIds || [],
        postIds: (res as unknown as { postIds?: number[] }).postIds || [],
        userType: props.userType,
      })
      void u
    }
    const treeRes = await userDeptTree()
    deptTree.value = mapTree((treeRes as unknown as { data: SysDept[] }).data)
  }
  finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const body: SysUserForm = { ...form, userType: props.userType }
    if (isEdit.value) await updateUser(body)
    else await createUser(body)
    ElMessage.success(t('common.saved'))
    emit('saved')
  }
  finally {
    saving.value = false
  }
}

function reset() {
  Object.assign(form, blank())
}

watch(() => props.modelValue, (open_) => { if (open_) open() })
</script>
