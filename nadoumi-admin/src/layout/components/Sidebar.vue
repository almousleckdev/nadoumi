<template>
  <nav
    class="side"
    :class="{ 'side--collapsed': collapsed }"
    :aria-label="t('nav.aria.primary')"
  >
    <div
      v-for="(group, gi) in groups"
      :key="gi"
      class="side__group"
    >
      <p
        v-if="group.key && !collapsed"
        class="side__heading"
      >
        {{ t(`nav.groups.${group.key}`) }}
      </p>

      <RouterLink
        v-for="item in group.items"
        :key="item.key"
        :to="item.path"
        class="side__link"
        :class="{ 'side__link--active': isActive(item.path) }"
        :title="collapsed ? t(`nav.items.${item.key}`) : undefined"
        @click="emit('navigate')"
      >
        <el-icon class="side__icon">
          <component :is="item.icon" />
        </el-icon>
        <span
          v-show="!collapsed"
          class="side__label"
        >{{ t(`nav.items.${item.key}`) }}</span>
      </RouterLink>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { NAV } from '@/config/nav'

defineProps<{ collapsed?: boolean }>()
const emit = defineEmits<{ navigate: [] }>()

const { t } = useI18n()
const route = useRoute()
const userStore = useUserStore()

// Only implemented items the user is allowed to reach are shown. Planned modules
// are excluded from active navigation entirely (docs/ADMIN_ARCHITECTURE.md §2.2).
// A group renders only when it has at least one visible item.
const groups = computed(() =>
  NAV.map(g => ({
    ...g,
    items: g.items.filter(i => i.status === 'implemented' && userStore.hasPerm(i.perm)),
  })).filter(g => g.items.length > 0),
)

function isActive(path: string): boolean {
  return route.path === path || route.path.startsWith(`${path}/`)
}
</script>

<style scoped>
.side {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px 24px;
}
.side__group + .side__group {
  margin-top: 4px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}
.side__heading {
  margin: 12px 8px 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--nad-sidebar-fg-muted);
}
.side__link {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 9px 10px;
  margin: 2px 0;
  border-radius: 9px;
  color: var(--nad-sidebar-fg);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.2;
  transition: background 0.14s ease, color 0.14s ease;
}
.side__link:hover {
  background: var(--nad-sidebar-bg-soft);
  color: #fff;
}
.side__link--active {
  background: var(--nad-brand-600);
  color: #fff;
}
.side__link--active:hover {
  background: var(--nad-brand-600);
}
.side__icon {
  font-size: 17px;
  flex-shrink: 0;
}
.side__label {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.side--collapsed .side__link {
  justify-content: center;
  padding: 10px 0;
}
</style>
