<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { status, user, signOut } = useSession()
const open = ref(false)

// Programs are discovered through a university, not a top-level product area
// (docs/PLATFORM_ARCHITECTURE.md §3, C1). "How it works" is homepage content.
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

// close the mobile sheet on navigation
watch(() => route.fullPath, () => { open.value = false })
</script>

<template>
  <header class="sticky top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur supports-[backdrop-filter]:bg-white/75">
    <NContainer>
      <div class="flex h-16 items-center justify-between gap-4">
        <NuxtLink :to="localePath('/')" class="flex items-center gap-2 no-underline">
          <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 font-display text-base font-bold text-white">N</span>
          <span class="site-brand font-display text-lg font-bold tracking-tight text-slate-900">Nadoumi</span>
        </NuxtLink>

        <nav class="hidden items-center gap-7 md:flex" :aria-label="t('nav.aria')">
          <NuxtLink
            v-for="l in links"
            :key="l.to"
            :to="localePath(l.to)"
            class="relative py-5 text-sm no-underline transition-colors hover:no-underline"
            :class="isActive(l.to) ? 'font-semibold text-slate-900' : 'text-slate-600 hover:text-slate-900'"
          >
            {{ t(l.key) }}
            <span
              v-if="isActive(l.to)"
              class="absolute inset-x-0 -bottom-px h-0.5 rounded-full bg-brand-600"
              aria-hidden="true"
            />
          </NuxtLink>
        </nav>

        <div class="flex items-center gap-2 sm:gap-3">
          <NLocaleSwitcher class="hidden sm:block" />
          <template v-if="status === 'authed'">
            <NDropdown :label="user?.nickName ?? t('nav.dashboard')" trigger-test-id="user-menu">
              <NuxtLink :to="localePath('/dashboard')" class="block px-3 py-2 text-sm no-underline hover:bg-slate-50">{{ t('nav.dashboard') }}</NuxtLink>
              <NuxtLink :to="localePath('/dashboard/account')" class="block px-3 py-2 text-sm no-underline hover:bg-slate-50">{{ t('dashboard.nav.account') }}</NuxtLink>
              <button type="button" data-test="sign-out" class="block w-full px-3 py-2 text-start text-sm text-red-600 hover:bg-slate-50" @click="signOut">{{ t('common.signOut') }}</button>
            </NDropdown>
          </template>
          <template v-else>
            <NuxtLink :to="localePath('/login')" class="hidden text-sm font-medium text-slate-700 no-underline hover:text-slate-900 hover:no-underline sm:inline">{{ t('nav.signIn') }}</NuxtLink>
            <NButton :to="localePath('/register')" size="sm">{{ t('nav.createAccount') }}</NButton>
          </template>
          <button
            type="button"
            class="inline-flex h-9 w-9 items-center justify-center rounded-md text-slate-700 hover:bg-slate-100 md:hidden"
            aria-controls="site-nav"
            :aria-expanded="open ? 'true' : 'false'"
            :aria-label="t('nav.aria')"
            @click="open = !open"
          >
            <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" aria-hidden="true">
              <path v-if="!open" d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" />
              <path v-else d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" />
            </svg>
          </button>
        </div>
      </div>

      <nav v-show="open" id="site-nav" class="grid gap-1 border-t border-slate-100 py-3 md:hidden">
        <NuxtLink
          v-for="l in links"
          :key="l.to"
          :to="localePath(l.to)"
          class="rounded-md px-3 py-2.5 text-sm no-underline hover:bg-slate-50 hover:no-underline"
          :class="isActive(l.to) ? 'bg-brand-50 font-semibold text-brand-800' : 'text-slate-700'"
        >
          {{ t(l.key) }}
        </NuxtLink>
        <div class="mt-2 flex items-center justify-between border-t border-slate-100 px-3 pt-3">
          <NLocaleSwitcher />
          <NuxtLink v-if="status !== 'authed'" :to="localePath('/login')" class="text-sm font-medium text-slate-700 no-underline">{{ t('nav.signIn') }}</NuxtLink>
        </div>
      </nav>
    </NContainer>
  </header>
</template>
