<script setup lang="ts">
/**
 * One row of a student sidebar: a link when `to` is given, otherwise a button. The active row uses the brand
 * colour like the admin sidebar; every other row is neutral and lightens on hover.
 */
const props = defineProps<{
  to?: string
  active?: boolean
  collapsed?: boolean
  title?: string
}>()

const classes = computed(() => [
  'my-0.5 flex w-full items-center gap-3 rounded-lg px-2.5 py-2 text-start text-sm font-medium transition-colors',
  'focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-400',
  props.active ? 'bg-brand-600 text-white' : 'text-[var(--sidebar-fg)] hover:bg-[var(--sidebar-bg-soft)] hover:text-white',
  props.collapsed ? 'justify-center' : '',
])
</script>

<template>
  <NuxtLink v-if="to" :to="to" :title="title" :class="classes" :aria-current="active ? 'page' : undefined">
    <slot name="icon" />
    <span v-show="!collapsed" class="min-w-0 flex-1 truncate"><slot /></span>
    <slot name="trailing" />
  </NuxtLink>
  <button v-else type="button" :title="title" :class="classes" :aria-current="active ? 'step' : undefined">
    <slot name="icon" />
    <span v-show="!collapsed" class="min-w-0 flex-1 truncate"><slot /></span>
    <slot name="trailing" />
  </button>
</template>
