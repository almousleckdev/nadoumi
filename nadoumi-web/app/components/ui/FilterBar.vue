<script setup lang="ts">
defineProps<{ count?: number, countLabel?: string, resultsPending?: boolean }>()
const open = ref(false)
</script>

<template>
  <div class="rounded-xl border border-slate-200 bg-white p-4 shadow-xs">
    <div class="flex flex-col gap-3 md:flex-row md:items-center">
      <div class="flex-1">
        <slot name="search" />
      </div>

      <div class="hidden flex-wrap items-center gap-2 md:flex">
        <slot name="filters" />
      </div>

      <button
        type="button"
        class="inline-flex items-center justify-center gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 md:hidden"
        :aria-expanded="open ? 'true' : 'false'"
        @click="open = !open"
      >
        <svg viewBox="0 0 20 20" class="h-4 w-4" fill="none" aria-hidden="true">
          <path d="M3 5h14M6 10h8M9 15h2" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" />
        </svg>
        <slot name="filtersLabel">Filters</slot>
      </button>

      <div v-if="$slots.sort" class="shrink-0">
        <slot name="sort" />
      </div>
    </div>

    <div v-if="open" class="mt-3 grid gap-2 border-t border-slate-100 pt-3 md:hidden">
      <slot name="filters" />
    </div>

    <div class="mt-3 flex items-center justify-between gap-3 border-t border-slate-100 pt-3">
      <p class="text-sm text-slate-500">
        <span v-if="resultsPending">…</span>
        <span v-else-if="count != null">{{ countLabel || `${count} results` }}</span>
      </p>
      <div><slot name="chips" /></div>
    </div>
  </div>
</template>
