<script setup lang="ts">
defineProps<{ steps: { key: string; label: string }[]; current: number }>()
</script>

<template>
  <nav :aria-label="'Onboarding progress'" class="mb-8">
    <ol class="flex flex-wrap items-center gap-x-2 gap-y-3 text-xs font-medium">
      <li v-for="(s, i) in steps" :key="s.key" class="flex items-center gap-2">
        <span
          class="inline-flex h-6 w-6 items-center justify-center rounded-full border transition-colors"
          :class="i < current
            ? 'border-brand-500 bg-brand-500 text-white'
            : i === current
              ? 'border-brand-500 bg-brand-50 text-brand-700'
              : 'border-slate-200 text-slate-400'"
          :aria-current="i === current ? 'step' : undefined"
        >{{ i < current ? '✓' : i + 1 }}</span>
        <span :class="i <= current ? 'text-slate-700' : 'text-slate-400'">{{ s.label }}</span>
        <span v-if="i < steps.length - 1" class="hidden h-px w-6 bg-slate-200 sm:block" />
      </li>
    </ol>
  </nav>
</template>
