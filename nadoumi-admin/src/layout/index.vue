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
        <span class="shell__brand-mark">N</span>
        <span
          v-show="!collapsed"
          class="shell__brand-word"
        >Nadoumi</span>
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
  height: var(--nad-header-h);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  flex-shrink: 0;
}
.shell__brand-mark {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, var(--nad-brand-500), var(--nad-brand-600));
  flex-shrink: 0;
}
.shell__brand-word {
  font-weight: 700;
  font-size: 17px;
  color: #fff;
  letter-spacing: 0.2px;
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
