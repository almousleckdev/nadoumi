<script setup lang="ts">
/** A titled group of form fields on the shared two-column grid. */
defineProps<{ title: string }>()
const headingId = useId()
</script>

<template>
  <section class="grid gap-4 sm:grid-cols-2" :aria-labelledby="headingId">
    <h3 :id="headingId" class="text-sm font-semibold uppercase tracking-wide text-slate-500 sm:col-span-2">
      {{ title }}
    </h3>
    <slot />
  </section>
</template>
