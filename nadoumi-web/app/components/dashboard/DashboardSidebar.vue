<script setup lang="ts">
import type { DashboardIconName } from './icons'

defineProps<{ collapsed?: boolean }>()
const emit = defineEmits<{ navigate: [] }>()

const { t } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { ask: askSignOut } = useSignOutConfirm()

const { count: unreadMessages, refresh: refreshUnreadMessages } = useChatUnread()
const { unreadCount: getUnreadNotifs } = useNotifications()

const { data: notificationCount } = useAsyncData('sidebar-badges', async () => {
  const [, notifs] = await Promise.all([
    refreshUnreadMessages(),
    getUnreadNotifs().catch(() => ({ count: 0 })),
  ])
  return notifs.count
})
const badgeCounts = computed(() => ({ messages: unreadMessages.value, notifications: notificationCount.value ?? 0 }))

function badgeFor(to: string): number {
  if (to === '/dashboard/messages') return badgeCounts.value.messages
  if (to === '/dashboard/notifications') return badgeCounts.value.notifications
  return 0
}


interface NavItem { to: string, key: string, icon: DashboardIconName, badge?: number }
interface NavGroup { heading?: string, items: NavItem[] }

const groups: NavGroup[] = [
  {
    items: [
      { to: '/dashboard', key: 'dashboard.nav.overview', icon: 'overview' },
      { to: '/dashboard/applications', key: 'dashboard.nav.applications', icon: 'applications' },
      { to: '/dashboard/documents', key: 'dashboard.nav.documents', icon: 'documents' },
      { to: '/dashboard/messages', key: 'dashboard.nav.messages', icon: 'messages' },
      { to: '/dashboard/notifications', key: 'dashboard.nav.notifications', icon: 'bell' },
      { to: '/dashboard/support', key: 'dashboard.nav.support', icon: 'support' },
    ],
  },
  {
    heading: 'dashboard.nav.groupProfile',
    items: [
      { to: '/dashboard/profile', key: 'dashboard.nav.profile', icon: 'profile' },
      { to: '/dashboard/education', key: 'dashboard.nav.education', icon: 'education' },
      { to: '/dashboard/interests', key: 'dashboard.nav.interests', icon: 'interests' },
      { to: '/dashboard/location', key: 'dashboard.nav.location', icon: 'location' },
      { to: '/dashboard/work', key: 'dashboard.nav.work', icon: 'work' },
      { to: '/dashboard/contacts', key: 'dashboard.nav.contacts', icon: 'contacts' },
      { to: '/dashboard/family', key: 'dashboard.nav.family', icon: 'profile' },
    ],
  },
  {
    items: [
      { to: '/dashboard/account', key: 'dashboard.nav.settings', icon: 'settings' },
    ],
  },
]

function isActive(to: string): boolean {
  const path = localePath(to)
  return route.path === path || (to !== '/dashboard' && route.path.startsWith(`${path}/`))
}

</script>

<template>
  <nav :aria-label="t('dashboard.nav.aria')" class="flex flex-1 flex-col overflow-y-auto px-3 py-2">
    <div v-for="(group, gi) in groups" :key="gi" :class="gi > 0 ? 'mt-2 border-t border-white/5 pt-2' : ''">
      <p v-if="group.heading && !collapsed" class="mb-1 mt-2 px-2.5 text-[11px] font-semibold uppercase tracking-wide text-[var(--sidebar-fg-muted)]">
        {{ t(group.heading) }}
      </p>
      <SidebarItem
        v-for="item in group.items"
        :key="item.to"
        :to="localePath(item.to)"
        :active="isActive(item.to)"
        :collapsed="collapsed"
        :title="collapsed ? t(item.key) : undefined"
        @click="emit('navigate')"
      >
        <template #icon><DashboardIcon :name="item.icon" :size="18" class="shrink-0" /></template>
        {{ t(item.key) }}
        <template v-if="!collapsed && badgeFor(item.to)" #trailing>
          <span class="inline-flex h-5 items-center justify-center rounded-full bg-brand-600 px-2 text-[10px] font-bold text-white">{{ badgeFor(item.to) }}</span>
        </template>
      </SidebarItem>
    </div>

    <div class="flex-1" />

    <SidebarItem
      data-test="sidebar-sign-out"
      :collapsed="collapsed"
      :title="collapsed ? t('common.signOut') : undefined"
      @click="askSignOut(); emit('navigate')"
    >
      <template #icon><DashboardIcon name="logout" :size="18" class="shrink-0" /></template>
      {{ t('common.signOut') }}
    </SidebarItem>
  </nav>
</template>
