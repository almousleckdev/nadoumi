<script setup lang="ts">
/** A text link: neutral and underlined. The brand colour is reserved for buttons and icons. */
defineProps<{ to: string }>()
</script>

<template>
  <NuxtLink :to="to" class="text-sm font-medium text-slate-900 underline underline-offset-2 hover:text-slate-600">
    <slot />
  </NuxtLink>
</template>
