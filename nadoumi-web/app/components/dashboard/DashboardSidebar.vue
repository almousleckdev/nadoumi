<script setup lang="ts">
import type { DashboardIconName } from './icons'

const props = defineProps<{ collapsed?: boolean }>()
const emit = defineEmits<{ navigate: [] }>()

const { t } = useI18n()
const localePath = useLocalePath()
const route = useRoute()

const nav: { to: string, key: string, icon: DashboardIconName }[] = [
  { to: '/dashboard', key: 'dashboard.nav.overview', icon: 'overview' },
  { to: '/dashboard/profile', key: 'dashboard.nav.profile', icon: 'profile' },
  { to: '/dashboard/documents', key: 'dashboard.nav.documents', icon: 'documents' },
  { to: '/dashboard/education', key: 'dashboard.nav.education', icon: 'education' },
  { to: '/dashboard/interests', key: 'dashboard.nav.interests', icon: 'interests' },
  { to: '/dashboard/location', key: 'dashboard.nav.location', icon: 'location' },
  { to: '/dashboard/work', key: 'dashboard.nav.work', icon: 'work' },
  { to: '/dashboard/contacts', key: 'dashboard.nav.contacts', icon: 'contacts' },
  { to: '/dashboard/account', key: 'dashboard.nav.account', icon: 'account' },
]

function isActive(to: string): boolean {
  const path = localePath(to)
  return route.path === path || (to !== '/dashboard' && route.path.startsWith(`${path}/`))
}
</script>

<template>
  <nav :aria-label="t('dashboard.nav.aria')" class="flex-1 overflow-y-auto px-3 py-2">
    <NuxtLink
      v-for="item in nav"
      :key="item.to"
      :to="localePath(item.to)"
      :title="props.collapsed ? t(item.key) : undefined"
      class="my-0.5 flex items-center gap-3 rounded-lg px-2.5 py-2 text-sm font-medium transition-colors"
      :class="[
        isActive(item.to) ? 'bg-brand-600 text-white' : 'text-[var(--sidebar-fg)] hover:bg-[var(--ink-800)] hover:text-white',
        props.collapsed ? 'justify-center' : '',
      ]"
      @click="emit('navigate')"
    >
      <DashboardIcon :name="item.icon" :size="18" class="shrink-0" />
      <span v-show="!props.collapsed" class="truncate">{{ t(item.key) }}</span>
    </NuxtLink>
  </nav>
</template>
