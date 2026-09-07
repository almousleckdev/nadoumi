<template>
  <div
    class="shell"
    :class="{ 'shell--collapsed': collapsed, 'shell--mobile-open': mobileOpen }"
  >
    <div
      v-if="mobileOpen"
      class="shell__scrim"
      @click="mobileOpen = false"
    />

    <aside class="shell__aside">
      <div class="shell__brand">
        <img
          class="shell__brand-logo"
          src="/logo/logo.jpg"
          alt="Nadoumi"
        >
      </div>
      <Sidebar
        :collapsed="collapsed"
        @navigate="mobileOpen = false"
      />
    </aside>

    <div class="shell__body">
      <header class="shell__header">
        <Navbar
          :collapsed="collapsed"
          @toggle-collapse="collapsed = !collapsed"
          @toggle-mobile="mobileOpen = !mobileOpen"
        />
      </header>
      <main class="shell__main">
        <router-view v-slot="{ Component }">
          <transition
            name="fade-up"
            mode="out-in"
          >
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'

const STORAGE_KEY = 'nadoumi-admin-sidebar-collapsed'

function readCollapsed(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === '1'
  }
  catch {
    return false
  }
}

const collapsed = ref(readCollapsed())
const mobileOpen = ref(false)

watch(collapsed, (v) => {
  try {
    localStorage.setItem(STORAGE_KEY, v ? '1' : '0')
  }
  catch {
    /* private mode — ignore */
  }
})
</script>

<style scoped>
.shell {
  --aside: var(--nad-aside-w);
  display: grid;
  grid-template-columns: var(--aside) 1fr;
  height: 100%;
  transition: grid-template-columns 0.2s ease;
}
.shell--collapsed {
  --aside: var(--nad-aside-w-collapsed);
}
.shell__aside {
  background: var(--nad-sidebar-bg);
  color: var(--nad-sidebar-fg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.04);
}
.shell__brand {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px 16px;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.shell__brand-logo {
  display: block;
  height: 60px;
  width: auto;
  max-width: 100%;
  object-fit: contain;
  border-radius: 10px;
  background: #fff;
  padding: 8px 14px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.18);
}
.shell--collapsed .shell__brand {
  padding: 14px 8px;
}
.shell--collapsed .shell__brand-logo {
  height: 44px;
  width: 44px;
  padding: 5px;
  object-fit: contain;
}
.shell__body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--nad-canvas);
}
.shell__header {
  height: var(--nad-header-h);
  flex-shrink: 0;
  background: var(--nad-surface);
  border-bottom: 1px solid var(--nad-line);
}
.shell__main {
  flex: 1;
  overflow-y: auto;
}
.shell__scrim {
  display: none;
}

.fade-up-enter-active,
.fade-up-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}
.fade-up-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.fade-up-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 900px) {
  .shell {
    grid-template-columns: 1fr;
  }
  .shell__aside {
    position: fixed;
    z-index: 40;
    inset: 0 auto 0 0;
    width: var(--nad-aside-w);
    transform: translateX(-100%);
    transition: transform 0.22s ease;
  }
  .shell--mobile-open .shell__aside {
    transform: translateX(0);
  }
  .shell--mobile-open .shell__scrim {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 30;
    background: rgba(16, 25, 47, 0.45);
  }
}

@media (prefers-reduced-motion: reduce) {
  .shell,
  .shell__aside,
  .fade-up-enter-active,
  .fade-up-leave-active {
    transition: none;
  }
}
</style>
