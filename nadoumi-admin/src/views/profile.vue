<template>
  <div class="nad-page">
    <el-card style="max-width: 480px">
      <template #header>
        {{ t('profile.changePassword') }}
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="180px"
      >
        <el-form-item
          :label="t('profile.oldPassword')"
          prop="oldPassword"
        >
          <el-input
            v-model="form.oldPassword"
            type="password"
            show-password
          />
        </el-form-item>
        <el-form-item
          :label="t('profile.newPassword')"
          prop="newPassword"
        >
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
          />
        </el-form-item>
        <el-form-item
          :label="t('profile.confirmPassword')"
          prop="confirmPassword"
        >
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="submit"
          >
            {{ t('common.save') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { updatePassword } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const loading = ref(false)

const rules = {
  oldPassword: [{ required: true, trigger: 'blur', message: t('profile.oldPassword') }],
  newPassword: [{ required: true, min: 5, max: 20, trigger: 'blur', message: t('profile.newPassword') }],
  confirmPassword: [
    {
      validator: (_r: unknown, v: string, cb: (e?: Error) => void) =>
        v === form.newPassword ? cb() : cb(new Error(t('profile.mismatch'))),
      trigger: 'blur',
    },
  ],
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await updatePassword(form.oldPassword, form.newPassword)
    ElMessage.success(t('profile.updated'))
    userStore.mustChangePassword = false
    form.oldPassword = form.newPassword = form.confirmPassword = ''
  } finally {
    loading.value = false
  }
}
</script>
