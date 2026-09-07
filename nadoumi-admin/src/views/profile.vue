<template>
  <div class="nad-page">
    <PageHeader
      :title="t('profile.title')"
      :subtitle="t('profile.subtitle')"
    />

    <el-alert
      v-if="userStore.mustChangePassword"
      type="warning"
      :closable="false"
      show-icon
      :title="t('profile.initialWarning')"
      style="margin-bottom: 16px"
    />

    <div
      v-loading="loading"
      class="profile__grid"
    >
      <el-card class="profile__identity">
        <div class="profile__avatar-wrap">
          <img
            v-if="avatarUrl"
            :src="avatarUrl"
            class="profile__avatar"
            alt=""
          >
          <div
            v-else
            class="profile__avatar profile__avatar--fallback"
          >
            {{ initial }}
          </div>
          <el-upload
            :show-file-list="false"
            :before-upload="onAvatar"
            accept="image/png,image/jpeg,image/webp"
          >
            <el-button
              size="small"
              :loading="avatarBusy"
            >
              {{ t('profile.changeAvatar') }}
            </el-button>
          </el-upload>
        </div>
        <dl class="profile__meta">
          <div>
            <dt>{{ t('profile.username') }}</dt>
            <dd>{{ user.userName }}</dd>
          </div>
          <div>
            <dt>{{ t('profile.dept') }}</dt>
            <dd>{{ user.deptName || '' }}</dd>
          </div>
          <div>
            <dt>{{ t('profile.roles') }}</dt>
            <dd>{{ roleGroup || '' }}</dd>
          </div>
          <div>
            <dt>{{ t('profile.posts') }}</dt>
            <dd>{{ postGroup || '' }}</dd>
          </div>
        </dl>

        <div
          v-if="session"
          class="profile__session"
        >
          <h4>{{ t('profile.currentSession') }}</h4>
          <dl class="profile__meta">
            <div>
              <dt>{{ t('profile.signedInAt') }}</dt>
              <dd>{{ fmt(session.loginTime) }}</dd>
            </div>
            <div>
              <dt>{{ t('profile.activeFor') }}</dt>
              <dd>{{ dur(session.loggedInForSeconds) }}</dd>
            </div>
            <div>
              <dt>{{ t('profile.expiresIn') }}</dt>
              <dd>{{ dur(session.expiresInSeconds) }}</dd>
            </div>
            <div>
              <dt>{{ t('profile.ip') }}</dt>
              <dd>{{ session.ipaddr || '' }}</dd>
            </div>
            <div>
              <dt>{{ t('profile.location') }}</dt>
              <dd>{{ session.location || '' }}</dd>
            </div>
            <div>
              <dt>{{ t('profile.client') }}</dt>
              <dd>{{ [session.browser, session.os].filter(Boolean).join(' · ') || '' }}</dd>
            </div>
          </dl>
        </div>
      </el-card>

      <div class="profile__forms">
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
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  getProfile, updateProfile, updatePassword, uploadAvatar, getSession,
  type ProfileUser, type ProfileSession,
} from '@/api/profile'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const user = ref<Partial<ProfileUser>>({})
const roleGroup = ref('')
const postGroup = ref('')
const localAvatar = ref('')
const session = ref<ProfileSession | null>(null)

function fmt(ms: number): string {
  return new Date(ms).toLocaleString()
}
function dur(seconds: number): string {
  if (seconds < 60) return `${seconds}s`
  const m = Math.floor(seconds / 60)
  if (m < 60) return `${m}m`
  const h = Math.floor(m / 60)
  const rm = m % 60
  if (h < 24) return rm ? `${h}h ${rm}m` : `${h}h`
  const d = Math.floor(h / 24)
  return `${d}d ${h % 24}h`
}

const infoRef = ref<FormInstance>()
const pwdRef = ref<FormInstance>()
const info = reactive({ nickName: '', phonenumber: '', email: '', sex: '0' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const infoBusy = ref(false)
const pwdBusy = ref(false)
const avatarBusy = ref(false)

const AVATAR_MAX_MB = 2

const initial = computed(() =>
  (userStore.nickName || userStore.name || '?').trim().charAt(0).toUpperCase())

const avatarUrl = computed(() => localAvatar.value || user.value.avatar || '')

const infoRules = {
  nickName: [{ required: true, trigger: 'blur', message: t('common.required') }],
  email: [{ type: 'email' as const, trigger: 'blur', message: t('profile.emailInvalid') }],
  phonenumber: [
    { pattern: /^$|^1[3-9]\d{9}$|^\+?[0-9 ()-]{6,20}$/, trigger: 'blur', message: t('profile.phoneInvalid') },
  ],
}

const pwdRules = {
  oldPassword: [{ required: true, trigger: 'blur', message: t('common.required') }],
  newPassword: [{ required: true, min: 5, max: 20, trigger: 'blur', message: t('profile.newPassword') }],
  confirmPassword: [
    {
      validator: (_r: unknown, v: string, cb: (e?: Error) => void) =>
        v === pwd.newPassword ? cb() : cb(new Error(t('profile.mismatch'))),
      trigger: 'blur',
    },
  ],
}

async function load() {
  loading.value = true
  try {
    const res = await getProfile()
    user.value = res.data
    roleGroup.value = res.roleGroup
    postGroup.value = res.postGroup
    info.nickName = res.data.nickName || ''
    info.phonenumber = res.data.phonenumber || ''
    info.email = res.data.email || ''
    info.sex = res.data.sex || '0'
    try {
      session.value = await getSession()
    }
    catch {
      /* session detail is non-critical */
    }
  } finally {
    loading.value = false
  }
}

async function submitInfo() {
  await infoRef.value?.validate()
  infoBusy.value = true
  try {
    await updateProfile({ ...info })
    ElMessage.success(t('common.saved'))
    userStore.nickName = info.nickName
  } finally {
    infoBusy.value = false
  }
}

async function submitPwd() {
  await pwdRef.value?.validate()
  pwdBusy.value = true
  try {
    await updatePassword(pwd.oldPassword, pwd.newPassword)
    ElMessage.success(t('profile.updated'))
    userStore.mustChangePassword = false
    pwd.oldPassword = pwd.newPassword = pwd.confirmPassword = ''
  } finally {
    pwdBusy.value = false
  }
}

function onAvatar(file: File): boolean {
  if (!file.type.startsWith('image/')) {
    ElMessage.error(t('imageUpload.badType'))
    return false
  }
  if (file.size > AVATAR_MAX_MB * 1024 * 1024) {
    ElMessage.error(t('imageUpload.tooBig', { mb: AVATAR_MAX_MB }))
    return false
  }
  avatarBusy.value = true
  uploadAvatar(file)
    .then((res) => {
      localAvatar.value = res.imgUrl
      userStore.avatar = res.imgUrl
      ElMessage.success(t('common.saved'))
    })
    .finally(() => { avatarBusy.value = false })
  return false
}

onMounted(load)
</script>

<style scoped>
.profile__grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
  align-items: start;
}
.profile__avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--nad-line, #e2e8f0);
  margin-bottom: 16px;
}
.profile__avatar {
  width: 96px;
  height: 96px;
  border-radius: 999px;
  object-fit: cover;
}
.profile__avatar--fallback {
  display: grid;
  place-items: center;
  font-size: 34px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--nad-brand-500, #6366f1), var(--nad-brand-600, #4f46e5));
}
.profile__meta {
  margin: 0;
  display: grid;
  gap: 10px;
}
.profile__meta dt {
  font-size: 12px;
  color: var(--nad-ink-soft, #64748b);
}
.profile__meta dd {
  margin: 2px 0 0;
  font-size: 14px;
  color: var(--nad-ink, #0f172a);
}
.profile__session {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--nad-line, #e2e8f0);
}
.profile__session h4 {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--nad-ink-soft, #64748b);
}
.profile__forms {
  display: grid;
  gap: 20px;
}
@media (max-width: 900px) {
  .profile__grid {
    grid-template-columns: 1fr;
  }
}
</style>
