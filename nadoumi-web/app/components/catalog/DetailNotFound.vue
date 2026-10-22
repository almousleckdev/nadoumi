<script setup lang="ts">
defineProps<{ title: string, message: string, backTo: string, backLabel: string }>()
</script>

<template>
  <div class="bg-white">
    <PageHero :title="title" />
    <NContainer>
      <p class="py-10 text-slate-600">{{ message }}</p>
      <NButton :to="backTo" variant="secondary">← {{ backLabel }}</NButton>
    </NContainer>
  </div>
</template>
