<template>
  <div class="navbar">
    <span class="navbar__crumb">{{ pageTitle }}</span>
    <div class="spacer" />
    <el-dropdown @command="onCommand">
      <span class="navbar__user">
        {{ userStore.nickName || userStore.name }}
        <el-icon><ArrowDown /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile">{{ t('nav.profile') }}</el-dropdown-item>
          <el-dropdown-item command="locale">{{ locale === 'en' ? '中文' : 'English' }}</el-dropdown-item>
          <el-dropdown-item divided command="logout">{{ t('common.signOut') }}</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { setLocale } from '@/lang'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const pageTitle = computed(() => {
  const m = route.meta as any
  if (!m?.title) return ''
  return m.i18n ? t(m.title) : m.title
})

async function onCommand(cmd: string) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'locale') setLocale(locale.value === 'en' ? 'zh' : 'en')
  else if (cmd === 'logout') {
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 0 16px;
}
.navbar__crumb {
  font-weight: 600;
}
.navbar__user {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
