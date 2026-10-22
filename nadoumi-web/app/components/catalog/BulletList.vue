<script setup lang="ts">
defineProps<{ items: { text: string, detail?: string | null }[], columns?: 1 | 2 }>()
</script>

<template>
  <ul class="grid gap-3" :class="columns === 2 ? 'sm:grid-cols-2' : ''">
    <li v-for="(it, i) in items" :key="i" class="flex items-start gap-2.5 text-slate-700">
      <span class="mt-0.5 shrink-0 text-brand-600"><DashboardIcon name="check" :size="18" /></span>
      <span class="min-w-0 break-words">
        {{ it.text }}
        <span v-if="it.detail" class="block text-sm text-slate-500">{{ it.detail }}</span>
      </span>
    </li>
  </ul>
</template>
