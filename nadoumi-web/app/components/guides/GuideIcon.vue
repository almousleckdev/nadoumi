<script setup lang="ts">
import { ICON_PATHS, type GuideIconName } from './icons'

const props = defineProps<{ name: GuideIconName, size?: number }>()
</script>

<template>
  <svg
    :width="props.size ?? 22" :height="props.size ?? 22"
    viewBox="0 0 24 24" fill="none" stroke="currentColor"
    stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"
    aria-hidden="true"
  >
    <path :d="ICON_PATHS[props.name]" />
  </svg>
</template>
