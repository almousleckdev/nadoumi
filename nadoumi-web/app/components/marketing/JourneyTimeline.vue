<script setup lang="ts">
defineProps<{ steps: { title: string, body?: string }[] }>()
</script>

<template>
  <ol class="grid gap-x-6 gap-y-8 sm:grid-cols-2 lg:grid-cols-4">
    <li
      v-for="(step, i) in steps"
      :key="step.title"
      class="relative flex gap-4 sm:block"
    >
      <!-- connector -->
      <span
        v-if="i < steps.length - 1"
        class="absolute left-[15px] top-9 h-[calc(100%+2rem)] w-px bg-slate-200 sm:left-0 sm:top-4 sm:h-px sm:w-full"
        aria-hidden="true"
      />
      <span
        class="relative z-10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-600 font-display text-sm font-bold text-white sm:h-9 sm:w-9"
      >{{ i + 1 }}</span>
      <div class="sm:mt-4">
        <h3 class="font-display text-base font-semibold text-slate-900">{{ step.title }}</h3>
        <p v-if="step.body" class="mt-1 text-sm text-slate-600">{{ step.body }}</p>
      </div>
    </li>
  </ol>
</template>
