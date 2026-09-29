<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useConfirm } from '@/composables/useConfirm'
import { assetUrl } from '@/utils/asset'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()

const initial = computed(() =>
  (userStore.nickName || userStore.name || '?').trim().charAt(0).toUpperCase())

async function onCommand(cmd: string) {
  if (cmd === 'profile') {
    router.push('/profile')
  }
  else if (cmd === 'logout') {
    if (!(await confirm({
      title: t('nav.logoutTitle'),
      message: t('nav.logoutConfirm'),
      confirmText: t('common.signOut'),
    }))) return
    await userStore.logout()
    router.push('/login')
  }
}

defineExpose({ onCommand })
</script>

<template>
  <el-dropdown
    trigger="click"
    @command="onCommand"
  >
    <button
      class="topbar__user"
      type="button"
    >
      <img
        v-if="userStore.avatar"
        :src="assetUrl(userStore.avatar)"
        class="topbar__avatar topbar__avatar--img"
        alt=""
      >
      <span
        v-else
        class="topbar__avatar"
      >{{ initial }}</span>
      <span class="topbar__name">{{ userStore.nickName || userStore.name }}</span>
      <el-icon class="topbar__caret">
        <ArrowDown />
      </el-icon>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-if="userStore.mustChangePassword"
          command="profile"
        >
          <el-icon><WarningFilled /></el-icon>{{ t('profile.changePassword') }}
        </el-dropdown-item>
        <el-dropdown-item command="profile">
          {{ t('nav.profile') }}
        </el-dropdown-item>
        <el-dropdown-item
          divided
          command="logout"
        >
          {{ t('common.signOut') }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
.topbar__user {
  display: flex;
  align-items: center;
  gap: 9px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 999px;
  transition: background 0.14s ease;
}
.topbar__user:hover {
  background: var(--nad-canvas);
}
.topbar__avatar {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--nad-brand-500), var(--nad-brand-600));
}
.topbar__avatar--img {
  object-fit: cover;
  background: none;
}
.topbar__name {
  font-size: 14px;
  font-weight: 550;
  color: var(--nad-ink);
}
.topbar__caret {
  font-size: 13px;
  color: var(--nad-ink-faint);
}

@media (max-width: 900px) {
  .topbar__name {
    display: none;
  }
}
</style>
