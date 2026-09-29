<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useUnreadCount } from '@/composables/useUnreadCount'
import LocaleToggle from './LocaleToggle.vue'
import UserMenu from './UserMenu.vue'

defineProps<{ collapsed?: boolean }>()
const emit = defineEmits<{ 'toggle-collapse': [], 'toggle-mobile': [] }>()

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { unread, clear } = useUnreadCount()

const pageTitle = computed(() => {
  const m = route.meta as { title?: string, i18n?: boolean }
  if (!m?.title) return ''
  return m.i18n ? t(m.title) : m.title
})

function goNotifications() {
  router.push('/notifications')
  clear()
}
</script>

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

    <LocaleToggle />

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

    <UserMenu />
  </div>
</template>

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

@media (max-width: 900px) {
  .topbar__icon-btn--desktop {
    display: none;
  }
  .topbar__icon-btn--mobile {
    display: grid;
  }
}
</style>
