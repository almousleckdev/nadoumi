<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const { status, user } = useSession()
const open = ref(false)

const links = [
  { to: '/scholarships', key: 'nav.scholarships' },
  { to: '/universities', key: 'nav.universities' },
  { to: '/programs', key: 'nav.programs' },
  { to: '/destinations', key: 'nav.destinations' },
  { to: '/about', key: 'nav.about' },
  { to: '/contact', key: 'nav.contact' },
]
</script>

<template>
  <header class="border-b border-slate-200 bg-white">
    <NContainer>
      <div class="flex h-16 items-center justify-between gap-4">
        <NuxtLink :to="localePath('/')" class="flex items-center">
          <!-- swap this span for <img src="/logo.svg" alt="Nadoumi"> later; same box -->
          <span class="site-brand font-display text-lg font-bold text-slate-900">Nadoumi</span>
        </NuxtLink>

        <nav id="site-nav" class="hidden items-center gap-6 md:flex">
          <NuxtLink v-for="l in links" :key="l.to" :to="localePath(l.to)" class="text-sm text-slate-600 hover:text-slate-900">
            {{ t(l.key) }}
          </NuxtLink>
        </nav>

        <div class="flex items-center gap-3">
          <NLocaleSwitcher class="hidden sm:block" />
          <template v-if="status === 'authed'">
            <NDropdown :label="user?.nickName ?? t('nav.dashboard')">
              <NuxtLink :to="localePath('/dashboard')" class="block px-3 py-2 text-sm hover:bg-slate-50">{{ t('nav.dashboard') }}</NuxtLink>
              <NuxtLink :to="localePath('/dashboard/account')" class="block px-3 py-2 text-sm hover:bg-slate-50">{{ t('dashboard.nav.account') }}</NuxtLink>
            </NDropdown>
          </template>
          <template v-else>
            <NuxtLink :to="localePath('/login')" class="text-sm font-medium text-slate-700 hover:text-slate-900">{{ t('nav.signIn') }}</NuxtLink>
            <NButton :to="localePath('/register')" size="sm">{{ t('nav.getStarted') }}</NButton>
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

      <nav v-if="open" class="grid gap-1 pb-3 md:hidden">
        <NuxtLink v-for="l in links" :key="l.to" :to="localePath(l.to)" class="rounded px-2 py-2 text-sm text-slate-700 hover:bg-slate-50" @click="open = false">
          {{ t(l.key) }}
        </NuxtLink>
      </nav>
    </NContainer>
  </header>
</template>
