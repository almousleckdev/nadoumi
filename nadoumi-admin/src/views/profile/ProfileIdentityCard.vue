<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { uploadAvatar, type ProfileSession, type ProfileUser } from '@/api/profile'
import { useUserStore } from '@/stores/user'
import { assetUrl } from '@/utils/asset'
import { formatDuration } from '@/utils/date'

const AVATAR_MAX_MB = 2

const props = defineProps<{
  user: Partial<ProfileUser>
  roleGroup: string
  postGroup: string
  session: ProfileSession | null
}>()

const { t } = useI18n()
const userStore = useUserStore()

const localAvatar = ref('')
const avatarBusy = ref(false)

const initial = computed(() =>
  (userStore.nickName || userStore.name || '?').trim().charAt(0).toUpperCase())
const avatarUrl = computed(() => localAvatar.value || assetUrl(props.user.avatar) || '')
const loginTime = computed(() => (props.session ? new Date(props.session.loginTime).toLocaleString() : ''))

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
</script>

<template>
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
          <dd>{{ loginTime }}</dd>
        </div>
        <div>
          <dt>{{ t('profile.activeFor') }}</dt>
          <dd>{{ formatDuration(session.loggedInForSeconds) }}</dd>
        </div>
        <div>
          <dt>{{ t('profile.expiresIn') }}</dt>
          <dd>{{ formatDuration(session.expiresInSeconds) }}</dd>
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
</template>

<style scoped src="./profile.css" />
