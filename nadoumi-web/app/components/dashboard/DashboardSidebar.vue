<script setup lang="ts">
import type { DashboardIconName } from './icons'

const props = defineProps<{ collapsed?: boolean }>()
const emit = defineEmits<{ navigate: [] }>()

const { t } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { signOut } = useSession()

interface NavItem { to: string, key: string, icon: DashboardIconName }
interface NavGroup { heading?: string, items: NavItem[] }

const groups: NavGroup[] = [
  {
    items: [
      { to: '/dashboard', key: 'dashboard.nav.overview', icon: 'overview' },
      { to: '/dashboard/applications', key: 'dashboard.nav.applications', icon: 'applications' },
      { to: '/dashboard/documents', key: 'dashboard.nav.documents', icon: 'documents' },
      { to: '/scholarships', key: 'dashboard.nav.scholarships', icon: 'award' },
      { to: '/dashboard/notifications', key: 'dashboard.nav.notifications', icon: 'bell' },
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

const linkClass = (active: boolean) => [
  'my-0.5 flex items-center gap-3 rounded-lg px-2.5 py-2 text-sm font-medium transition-colors',
  active ? 'bg-brand-600 text-white' : 'text-[var(--sidebar-fg)] hover:bg-[var(--ink-800)] hover:text-white',
  props.collapsed ? 'justify-center' : '',
]
</script>

<template>
  <nav :aria-label="t('dashboard.nav.aria')" class="flex flex-1 flex-col overflow-y-auto px-3 py-2">
    <div v-for="(group, gi) in groups" :key="gi" :class="gi > 0 ? 'mt-2 border-t border-white/5 pt-2' : ''">
      <p v-if="group.heading && !collapsed" class="mb-1 mt-2 px-2.5 text-[11px] font-semibold uppercase tracking-wide text-[var(--sidebar-fg-muted)]">
        {{ t(group.heading) }}
      </p>
      <NuxtLink
        v-for="item in group.items"
        :key="item.to"
        :to="localePath(item.to)"
        :title="collapsed ? t(item.key) : undefined"
        :class="linkClass(isActive(item.to))"
        @click="emit('navigate')"
      >
        <DashboardIcon :name="item.icon" :size="18" class="shrink-0" />
        <span v-show="!collapsed" class="truncate">{{ t(item.key) }}</span>
      </NuxtLink>
    </div>

    <div class="flex-1" />

    <button
      type="button"
      data-test="sidebar-sign-out"
      :title="collapsed ? t('common.signOut') : undefined"
      :class="linkClass(false)"
      @click="signOut(); emit('navigate')"
    >
      <DashboardIcon name="logout" :size="18" class="shrink-0" />
      <span v-show="!collapsed" class="truncate">{{ t('common.signOut') }}</span>
    </button>
  </nav>
</template>
