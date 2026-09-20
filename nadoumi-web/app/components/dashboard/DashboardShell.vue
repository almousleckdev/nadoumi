<script setup lang="ts">
import logoUrl from '~/assets/images/logo.jpg'

const localePath = useLocalePath()

const STORAGE_KEY = 'nadoumi-dashboard-sidebar-collapsed'
function readCollapsed(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === '1'
  }
  catch {
    return false
  }
}

const collapsed = ref(false)
const mobileOpen = ref(false)
onMounted(() => { collapsed.value = readCollapsed() })
watch(collapsed, (v: boolean) => {
  try {
    localStorage.setItem(STORAGE_KEY, v ? '1' : '0')
  }
  catch {
    /* private mode — ignore */
  }
})
</script>

<template>
  <div class="min-h-screen bg-[var(--surface-subtle)] lg:flex">
    <div
      v-if="mobileOpen"
      class="fixed inset-0 z-30 bg-black/40 lg:hidden"
      @click="mobileOpen = false"
    />

    <aside
      class="fixed inset-y-0 start-0 z-40 flex w-[244px] -translate-x-full flex-col bg-[var(--ink-900)] transition-transform duration-200 lg:static lg:translate-x-0 lg:shrink-0 rtl:translate-x-full rtl:lg:translate-x-0"
      :class="[mobileOpen ? 'translate-x-0 rtl:translate-x-0' : '', collapsed ? 'lg:w-[76px]' : 'lg:w-[244px]']"
    >
      <div class="flex h-16 shrink-0 items-center justify-center border-b border-white/10 px-4">
        <NuxtLink :to="localePath('/dashboard')" class="block">
          <img :src="logoUrl" alt="Nadoumi" class="h-9 w-auto rounded-md bg-white p-1">
        </NuxtLink>
      </div>
      <DashboardSidebar :collapsed="collapsed && !mobileOpen" @navigate="mobileOpen = false" />
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="h-16 shrink-0 border-b border-slate-200 bg-white">
        <DashboardHeader :collapsed="collapsed" @toggle-collapse="collapsed = !collapsed" @toggle-mobile="mobileOpen = !mobileOpen" />
      </header>
      <main class="min-w-0 flex-1 px-4 py-6 sm:px-6 sm:py-8"><slot /></main>
    </div>
  </div>
</template>
