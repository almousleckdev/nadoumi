<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { updatePassword } from '@/api/profile'
import { useUserStore } from '@/stores/user'

const MIN_PASSWORD_LENGTH = 5
const MAX_PASSWORD_LENGTH = 20

const { t } = useI18n()
const userStore = useUserStore()

const pwdRef = ref<FormInstance>()
const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdBusy = ref(false)

const pwdRules = {
  oldPassword: [{ required: true, trigger: 'blur', message: t('common.required') }],
  newPassword: [{ required: true, min: MIN_PASSWORD_LENGTH, max: MAX_PASSWORD_LENGTH, trigger: 'blur', message: t('profile.newPassword') }],
  confirmPassword: [
    {
      validator: (_r: unknown, v: string, cb: (e?: Error) => void) =>
        v === pwd.newPassword ? cb() : cb(new Error(t('profile.mismatch'))),
      trigger: 'blur',
    },
  ],
}

async function submitPwd() {
  await pwdRef.value?.validate()
  pwdBusy.value = true
  try {
    await updatePassword(pwd.oldPassword, pwd.newPassword)
    ElMessage.success(t('profile.updated'))
    userStore.mustChangePassword = false
    pwd.oldPassword = pwd.newPassword = pwd.confirmPassword = ''
  }
  finally {
    pwdBusy.value = false
  }
}
</script>

<template>
  <el-card>
    <template #header>
      {{ t('profile.changePassword') }}
    </template>
    <el-form
      ref="pwdRef"
      :model="pwd"
      :rules="pwdRules"
      label-width="150px"
    >
      <el-form-item
        :label="t('profile.oldPassword')"
        prop="oldPassword"
      >
        <el-input
          v-model="pwd.oldPassword"
          type="password"
          show-password
        />
      </el-form-item>
      <el-form-item
        :label="t('profile.newPassword')"
        prop="newPassword"
      >
        <el-input
          v-model="pwd.newPassword"
          type="password"
          show-password
        />
      </el-form-item>
      <el-form-item
        :label="t('profile.confirmPassword')"
        prop="confirmPassword"
      >
        <el-input
          v-model="pwd.confirmPassword"
          type="password"
          show-password
        />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          :loading="pwdBusy"
          @click="submitPwd"
        >
          {{ t('common.save') }}
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>
