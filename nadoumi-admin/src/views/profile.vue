<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import PageHeader from '@/components/PageHeader.vue'
import { getProfile, getSession, type ProfileUser, type ProfileSession } from '@/api/profile'
import { useUserStore } from '@/stores/user'
import ProfileIdentityCard from './profile/ProfileIdentityCard.vue'
import ProfileInfoForm from './profile/ProfileInfoForm.vue'
import ChangePasswordForm from './profile/ChangePasswordForm.vue'

const { t } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const user = ref<Partial<ProfileUser>>({})
const roleGroup = ref('')
const postGroup = ref('')
const session = ref<ProfileSession | null>(null)

async function load() {
  loading.value = true
  try {
    const res = await getProfile()
    user.value = res.data
    roleGroup.value = res.roleGroup
    postGroup.value = res.postGroup
    try {
      session.value = await getSession()
    }
    catch {
      /* session detail is non-critical */
    }
  }
  finally {
    loading.value = false
  }
}

onMounted(load)
</script>

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
      <ProfileIdentityCard
        :user="user"
        :role-group="roleGroup"
        :post-group="postGroup"
        :session="session"
      />
      <div class="profile__forms">
        <ProfileInfoForm :user="user" />
        <ChangePasswordForm />
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile__grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
  align-items: start;
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
