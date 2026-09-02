<script setup lang="ts">
defineProps<{ title: string, subtitle?: string }>()
</script>

<template>
  <section class="border-b border-slate-200 bg-slate-50">
    <NContainer>
      <div class="py-12 sm:py-16">
        <h1 class="font-display text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
          {{ title }}
        </h1>
        <p v-if="subtitle" class="mt-3 max-w-2xl text-lg text-slate-600">{{ subtitle }}</p>
        <slot />
      </div>
    </NContainer>
  </section>
</template>
