<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'

defineProps<{ collapsed: boolean }>()
const emit = defineEmits<{ 'toggle-collapse': [], 'toggle-mobile': [] }>()

const { t } = useI18n()
const localePath = useLocalePath()
const { user, applicants, signOut } = useSession()
const { unreadCount } = useNotifications()
const { primary } = useMyApplicant()
const { photoUrl } = useApplicant()

const unread = ref(0)
const POLL_MS = 60_000
let timer: ReturnType<typeof setInterval> | undefined

async function refreshUnread() {
  try {
    unread.value = (await unreadCount()).count
  }
  catch {
    /* silent — the bell is non-critical */
  }
}

const avatarUrl = ref('')
watch(primary, async (applicant: ApplicantDto | null) => {
  avatarUrl.value = applicant ? await photoUrl(applicant.id).then(r => r.url).catch(() => '') : ''
}, { immediate: true })

onMounted(() => {
  refreshUnread()
  timer = setInterval(refreshUnread, POLL_MS)
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})

const displayName = computed(() => primary.value?.givenName ?? user.value?.nickName ?? user.value?.username ?? '')
</script>

<template>
  <div class="flex h-full items-center gap-2 px-4 sm:px-5">
    <button
      type="button"
      class="grid h-9 w-9 place-items-center rounded-lg text-slate-500 hover:bg-slate-100 lg:hidden"
      :aria-label="t('dashboard.nav.toggleSidebar')"
      @click="emit('toggle-mobile')"
    >
      <DashboardIcon name="menu" />
    </button>
    <button
      type="button"
      class="hidden h-9 w-9 place-items-center rounded-lg text-slate-500 hover:bg-slate-100 lg:grid"
      :aria-label="t('dashboard.nav.toggleSidebar')"
      @click="emit('toggle-collapse')"
    >
      <DashboardIcon :name="collapsed ? 'chevronRight' : 'chevronLeft'" />
    </button>

    <div class="flex-1" />

    <NLocaleSwitcher />
    <ApplicantSwitcher v-if="applicants.length > 1" />

    <NuxtLink
      :to="localePath('/dashboard/notifications')"
      class="relative grid h-9 w-9 place-items-center rounded-lg text-slate-500 hover:bg-slate-100"
      :aria-label="t('dashboard.notifications.aria')"
    >
      <DashboardIcon name="bell" />
      <span
        v-if="unread > 0"
        class="absolute right-1 top-1 grid h-4 min-w-4 place-items-center rounded-full bg-brand-600 px-1 text-[10px] font-semibold leading-none text-white"
      >{{ unread > 99 ? '99+' : unread }}</span>
    </NuxtLink>

    <NDropdown trigger-test-id="account-menu">
      <template #trigger>
        <span class="flex items-center gap-2 rounded-full py-1 pe-2 ps-1 hover:bg-slate-100">
          <NAvatar :name="displayName" :src="avatarUrl" />
          <span class="hidden text-sm font-medium text-slate-700 sm:inline">{{ displayName }}</span>
        </span>
      </template>
      <div class="px-3 py-2 text-xs text-slate-500">{{ user?.username }}</div>
      <NuxtLink :to="localePath('/dashboard/account')" class="block px-3 py-2 text-sm hover:bg-slate-50">{{ t('dashboard.nav.account') }}</NuxtLink>
      <button data-test="sign-out" type="button" class="block w-full px-3 py-2 text-start text-sm text-red-600 hover:bg-slate-50" @click="signOut">
        {{ t('common.signOut') }}
      </button>
    </NDropdown>
  </div>
</template>
