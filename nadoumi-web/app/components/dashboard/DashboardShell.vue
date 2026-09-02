<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const { user, applicants, signOut } = useSession()
const open = ref(false)

const nav = [
  { to: '/dashboard', key: 'dashboard.nav.overview' },
  { to: '/dashboard/profile', key: 'dashboard.nav.profile' },
  { to: '/dashboard/education', key: 'dashboard.nav.education' },
  { to: '/dashboard/test-scores', key: 'dashboard.nav.testScores' },
  { to: '/dashboard/contacts', key: 'dashboard.nav.contacts' },
  { to: '/dashboard/account', key: 'dashboard.nav.account' },
]
</script>

<template>
  <div class="min-h-screen bg-slate-50">
    <header class="border-b border-slate-200 bg-white">
      <div class="mx-auto flex h-16 max-w-app items-center justify-between px-5">
        <div class="flex items-center gap-3">
          <button type="button" class="lg:hidden rounded p-2 hover:bg-slate-100" @click="open = !open">☰</button>
          <NuxtLink :to="localePath('/')" class="font-display text-lg font-bold">Nadoumi</NuxtLink>
        </div>
        <div class="flex items-center gap-3">
          <ApplicantSwitcher v-if="applicants.length > 1" />
          <NDropdown>
            <template #trigger>
              <NAvatar :name="user?.nickName ?? user?.username" />
            </template>
            <div class="px-3 py-2 text-xs text-slate-500">{{ user?.username }}</div>
            <NuxtLink :to="localePath('/dashboard/account')" class="block px-3 py-2 text-sm hover:bg-slate-50">{{ t('dashboard.nav.account') }}</NuxtLink>
            <button data-test="sign-out" type="button" class="block w-full px-3 py-2 text-start text-sm text-red-600 hover:bg-slate-50" @click="signOut">
              {{ t('common.signOut') }}
            </button>
          </NDropdown>
        </div>
      </div>
    </header>

    <div class="mx-auto flex max-w-app gap-6 px-5 py-8">
      <aside class="w-52 shrink-0" :class="open ? 'block' : 'hidden lg:block'">
        <nav class="grid gap-1">
          <NuxtLink
            v-for="n in nav"
            :key="n.to"
            :to="localePath(n.to)"
            exact-active-class="bg-brand-50 text-brand-700 font-semibold"
            class="rounded-md px-3 py-2 text-sm text-slate-700 hover:bg-slate-100"
            @click="open = false"
          >
            {{ t(n.key) }}
          </NuxtLink>
        </nav>
      </aside>
      <main class="min-w-0 flex-1"><slot /></main>
    </div>
  </div>
</template>
