<template>
  <div class="login">
    <el-card class="login__card">
      <h1 class="login__title">{{ t('login.title') }}</h1>
      <p class="login__subtitle">{{ t('login.subtitle') }}</p>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" :placeholder="t('common.username')" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="t('common.password')"
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item v-if="captchaEnabled" prop="code">
          <div class="login__captcha">
            <el-input v-model="form.code" :placeholder="t('common.captcha')" />
            <img :src="captchaImg" alt="captcha" @click="loadCaptcha" />
          </div>
        </el-form-item>
        <el-button type="primary" class="login__submit" :loading="loading" @click="submit">
          {{ t('common.signIn') }}
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { type FormInstance } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const form = reactive({ username: 'almousleck', password: '', code: '', uuid: '' })
const rules = {
  username: [{ required: true, message: t('common.username'), trigger: 'blur' }],
  password: [{ required: true, message: t('common.password'), trigger: 'blur' }],
}
const loading = ref(false)
const captchaEnabled = ref(false)
const captchaImg = ref('')

async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    captchaEnabled.value = res.captchaEnabled !== false
    if (captchaEnabled.value) {
      captchaImg.value = `data:image/gif;base64,${res.img}`
      form.uuid = res.uuid
    }
  } catch {
    captchaEnabled.value = false
  }
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await userStore.login({ ...form })
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch {
    // the request interceptor already surfaced the error message; just recover state
    if (captchaEnabled.value) loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.login {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(160deg, #0b5fff 0%, #1f2a44 100%);
}
.login__card {
  width: 360px;
}
.login__title {
  margin: 0 0 4px;
  font-size: 20px;
}
.login__subtitle {
  margin: 0 0 20px;
  color: #909399;
  font-size: 13px;
}
.login__captcha {
  display: flex;
  gap: 8px;
}
.login__captcha img {
  height: 40px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.login__submit {
  width: 100%;
}
</style>
