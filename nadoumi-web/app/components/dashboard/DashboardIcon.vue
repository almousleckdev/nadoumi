<script setup lang="ts">
import { DASHBOARD_ICON_PATHS, type DashboardIconName } from './icons'

const props = defineProps<{ name: DashboardIconName, size?: number }>()
</script>

<template>
  <svg
    :width="props.size ?? 20" :height="props.size ?? 20"
    viewBox="0 0 24 24" fill="none" stroke="currentColor"
    stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"
    aria-hidden="true"
  >
    <path :d="DASHBOARD_ICON_PATHS[props.name]" />
  </svg>
</template>
