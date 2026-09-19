<script setup lang="ts">
/** A link to the Terms or the Privacy Policy, worded from the footer copy. */
defineProps<{ to: 'terms' | 'privacy' }>()
const { t } = useI18n()
const localePath = useLocalePath()
</script>

<template>
  <NuxtLink :to="localePath(`/${to}`)" class="text-brand-700 hover:underline">{{ t(`footer.${to}`) }}</NuxtLink>
</template>
