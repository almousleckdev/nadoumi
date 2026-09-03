<script setup lang="ts">
withDefaults(defineProps<{
  eyebrow?: string
  title: string
  description?: string
  as?: 'h2' | 'h3'
}>(), { eyebrow: undefined, description: undefined, as: 'h2' })
</script>

<template>
  <div class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
    <div class="max-w-2xl">
      <p v-if="eyebrow" class="text-xs font-semibold uppercase tracking-[0.14em] text-brand-600">
        {{ eyebrow }}
      </p>
      <component
        :is="as"
        class="mt-1 font-display text-2xl font-bold tracking-tight text-slate-900 sm:text-[1.75rem]"
      >
        {{ title }}
      </component>
      <p v-if="description" class="mt-2 text-slate-600">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="flex shrink-0 items-center gap-2">
      <slot name="actions" />
    </div>
  </div>
</template>
