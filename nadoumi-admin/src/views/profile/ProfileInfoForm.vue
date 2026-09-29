<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { updateProfile, type ProfileUser } from '@/api/profile'
import { useUserStore } from '@/stores/user'

const props = defineProps<{ user: Partial<ProfileUser> }>()

const { t } = useI18n()
const userStore = useUserStore()

const infoRef = ref<FormInstance>()
const info = reactive({ nickName: '', phonenumber: '', email: '', sex: '0' })
const infoBusy = ref(false)

const infoRules = {
  nickName: [{ required: true, trigger: 'blur', message: t('common.required') }],
  email: [{ type: 'email' as const, trigger: 'blur', message: t('profile.emailInvalid') }],
  phonenumber: [
    { pattern: /^$|^1[3-9]\d{9}$|^\+?[0-9 ()-]{6,20}$/, trigger: 'blur', message: t('profile.phoneInvalid') },
  ],
}

watch(() => props.user, (user) => {
  info.nickName = user.nickName || ''
  info.phonenumber = user.phonenumber || ''
  info.email = user.email || ''
  info.sex = user.sex || '0'
}, { immediate: true })

async function submitInfo() {
  await infoRef.value?.validate()
  infoBusy.value = true
  try {
    await updateProfile({ ...info })
    ElMessage.success(t('common.saved'))
    userStore.nickName = info.nickName
  }
  finally {
    infoBusy.value = false
  }
}
</script>

<template>
  <el-card>
    <template #header>
      {{ t('profile.basicInfo') }}
    </template>
    <el-form
      ref="infoRef"
      :model="info"
      :rules="infoRules"
      label-width="150px"
    >
      <el-form-item
        :label="t('profile.nickName')"
        prop="nickName"
      >
        <el-input v-model="info.nickName" />
      </el-form-item>
      <el-form-item
        :label="t('profile.phone')"
        prop="phonenumber"
      >
        <el-input v-model="info.phonenumber" />
      </el-form-item>
      <el-form-item
        :label="t('profile.email')"
        prop="email"
      >
        <el-input v-model="info.email" />
      </el-form-item>
      <el-form-item :label="t('profile.sex')">
        <el-radio-group v-model="info.sex">
          <el-radio value="0">
            {{ t('profile.male') }}
          </el-radio>
          <el-radio value="1">
            {{ t('profile.female') }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          :loading="infoBusy"
          @click="submitInfo"
        >
          {{ t('common.save') }}
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>
