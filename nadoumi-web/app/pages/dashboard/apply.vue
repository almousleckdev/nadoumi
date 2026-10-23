<script setup lang="ts">
// The application moved out of the dashboard into its own step-by-step flow at /apply; old links and emails land here.
definePageMeta({ middleware: [(to) => navigateTo({ path: '/apply', query: to.query }, { replace: true })] })
</script>

<template>
  <div />
</template>
