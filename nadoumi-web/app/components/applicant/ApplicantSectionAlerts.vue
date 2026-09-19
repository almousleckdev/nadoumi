<script setup lang="ts">
/** The error and success notices every applicant section shows above its content. */
defineProps<{ error?: string, notice?: string }>()
</script>

<template>
  <div class="grid gap-3 empty:hidden">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
    <NAlert v-if="notice" tone="success">{{ notice }}</NAlert>
  </div>
</template>
