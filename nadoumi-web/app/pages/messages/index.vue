<script setup lang="ts">
definePageMeta({ middleware: ['auth', 'onboarding'] })

const localePath = useLocalePath()
const route = useRoute()

await navigateTo(localePath({ path: '/dashboard/messages', query: route.query }), { replace: true })
</script>

<template>
  <div />
</template>
