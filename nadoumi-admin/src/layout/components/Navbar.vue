<template>
  <div class="topbar">
    <button
      class="topbar__icon-btn topbar__icon-btn--desktop"
      type="button"
      :aria-label="t('nav.aria.toggleSidebar')"
      @click="emit('toggle-collapse')"
    >
      <el-icon>
        <component :is="collapsed ? 'Expand' : 'Fold'" />
      </el-icon>
    </button>
    <button
      class="topbar__icon-btn topbar__icon-btn--mobile"
      type="button"
      :aria-label="t('nav.aria.toggleSidebar')"
      @click="emit('toggle-mobile')"
    >
      <el-icon><Menu /></el-icon>
    </button>

    <h1 class="topbar__title">
      {{ pageTitle }}
    </h1>

    <div class="topbar__spacer" />

    <button
      class="topbar__locale"
      type="button"
      :aria-label="localeButtonLabel"
      @click="toggleLocale"
    >
      <Transition
        name="locale-swap"
        mode="out-in"
      >
        <span :key="locale">{{ locale === 'en' ? '中文' : 'EN' }}</span>
      </Transition>
    </button>

    <button
      class="topbar__icon-btn"
      type="button"
      :aria-label="t('notifications.mine')"
      @click="goNotifications"
    >
      <el-badge
        :value="unread"
        :hidden="unread === 0"
        :max="99"
      >
        <el-icon><Bell /></el-icon>
      </el-badge>
    </button>

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
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { myUnreadCount } from '@/api/notification'
import { useConfirm } from '@/composables/useConfirm'
import { assetUrl } from '@/utils/asset'
import { setLocale } from '@/lang'

defineProps<{ collapsed?: boolean }>()
const emit = defineEmits<{ 'toggle-collapse': []; 'toggle-mobile': [] }>()

const { t, locale } = useI18n()
const localeButtonLabel = computed(() => (locale.value === 'en' ? 'Switch to Chinese' : '切换为英文'))
function toggleLocale() {
  setLocale(locale.value === 'en' ? 'zh' : 'en')
}
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()

const unread = ref(0)
const POLL_MS = 60_000
let timer: ReturnType<typeof setInterval> | undefined

async function refreshUnread() {
  try {
    unread.value = (await myUnreadCount()).count
  }
  catch {
    /* silent — the bell is non-critical */
  }
}
function goNotifications() {
  router.push('/notifications')
  unread.value = 0
}

onMounted(() => {
  refreshUnread()
  timer = setInterval(refreshUnread, POLL_MS)
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})

const pageTitle = computed(() => {
  const m = route.meta as { title?: string, i18n?: boolean }
  if (!m?.title) return ''
  return m.i18n ? t(m.title) : m.title
})

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
</script>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 100%;
  padding: 0 18px;
}
.topbar__icon-btn {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--nad-ink-soft);
  cursor: pointer;
  font-size: 17px;
  transition: background 0.14s ease;
}
.topbar__icon-btn:hover {
  background: var(--nad-canvas);
}
.topbar__icon-btn--mobile {
  display: none;
}
.topbar__title {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
  color: var(--nad-ink);
}
.topbar__spacer {
  flex: 1;
}
.topbar__locale {
  display: grid;
  place-items: center;
  min-width: 44px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--nad-border, #e2e5ea);
  border-radius: 999px;
  background: transparent;
  color: var(--nad-ink-soft);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.14s ease, border-color 0.14s ease;
}
.topbar__locale:hover {
  background: var(--nad-canvas);
  border-color: var(--nad-brand-500);
  color: var(--nad-ink);
}
.locale-swap-enter-active,
.locale-swap-leave-active {
  transition: opacity 140ms ease, transform 140ms ease;
}
.locale-swap-enter-from {
  opacity: 0;
  transform: translateY(3px);
}
.locale-swap-leave-to {
  opacity: 0;
  transform: translateY(-3px);
}
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
  .topbar__icon-btn--desktop {
    display: none;
  }
  .topbar__icon-btn--mobile {
    display: grid;
  }
  .topbar__name {
    display: none;
  }
}
</style>
