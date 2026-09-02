<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { status, user, signOut } = useSession()
const open = ref(false)

// Programs are discovered through a university, not as a top-level product area
// (docs/PLATFORM_ARCHITECTURE.md §3, C1).
const links = [
  { to: '/', key: 'nav.home' },
  { to: '/scholarships', key: 'nav.scholarships' },
  { to: '/universities', key: 'nav.universities' },
  { to: '/about', key: 'nav.about' },
  { to: '/contact', key: 'nav.contact' },
]

function isActive(to: string): boolean {
  const target = localePath(to)
  return to === '/' ? route.path === target : route.path.startsWith(target)
}
</script>

<template>
  <header class="border-b border-slate-200 bg-white">
    <NContainer>
      <div class="flex h-16 items-center justify-between gap-4">
        <NuxtLink :to="localePath('/')" class="flex items-center">
          <!-- swap this span for <img src="/logo.svg" alt="Nadoumi"> later; same box -->
          <span class="site-brand font-display text-lg font-bold text-slate-900">Nadoumi</span>
        </NuxtLink>

        <nav class="hidden items-center gap-6 md:flex">
          <NuxtLink
            v-for="l in links"
            :key="l.to"
            :to="localePath(l.to)"
            class="relative py-4 text-sm no-underline transition-colors hover:no-underline"
            :class="isActive(l.to) ? 'font-medium text-slate-900' : 'text-slate-600 hover:text-slate-900'"
          >
            {{ t(l.key) }}
            <span
              v-if="isActive(l.to)"
              class="absolute inset-x-0 -bottom-px h-0.5 rounded-full bg-brand-500"
              aria-hidden="true"
            />
          </NuxtLink>
        </nav>

        <div class="flex items-center gap-3">
          <NLocaleSwitcher class="hidden sm:block" />
          <template v-if="status === 'authed'">
            <NDropdown
              :label="user?.nickName ?? t('nav.dashboard')"
              trigger-test-id="user-menu"
            >
              <NuxtLink :to="localePath('/dashboard')" class="block px-3 py-2 text-sm hover:bg-slate-50">{{ t('nav.dashboard') }}</NuxtLink>
              <NuxtLink :to="localePath('/dashboard/account')" class="block px-3 py-2 text-sm hover:bg-slate-50">{{ t('dashboard.nav.account') }}</NuxtLink>
              <button type="button" data-test="sign-out" class="block w-full px-3 py-2 text-start text-sm text-red-600 hover:bg-slate-50" @click="signOut">{{ t('common.signOut') }}</button>
            </NDropdown>
          </template>
          <template v-else>
            <NuxtLink :to="localePath('/login')" class="text-sm font-medium text-slate-700 no-underline hover:text-slate-900 hover:no-underline">{{ t('nav.signIn') }}</NuxtLink>
            <NButton :to="localePath('/register')" size="sm">{{ t('nav.createAccount') }}</NButton>
          </template>
          <button
            type="button"
            class="md:hidden rounded-md p-2 hover:bg-slate-100"
            aria-controls="site-nav"
            :aria-expanded="open ? 'true' : 'false'"
            @click="open = !open"
          >☰</button>
        </div>
      </div>

      <nav v-show="open" id="site-nav" class="grid gap-1 pb-3 md:hidden">
        <NuxtLink
          v-for="l in links"
          :key="l.to"
          :to="localePath(l.to)"
          class="rounded px-2 py-2 text-sm no-underline hover:bg-slate-50 hover:no-underline"
          :class="isActive(l.to)
            ? 'border-s-2 border-brand-500 font-medium text-slate-900'
            : 'text-slate-700'"
          @click="open = false"
        >
          {{ t(l.key) }}
        </NuxtLink>
      </nav>
    </NContainer>
  </header>
</template>
