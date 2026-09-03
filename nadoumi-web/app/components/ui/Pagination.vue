<script setup lang="ts">
const props = defineProps<{ page: number, pageCount: number }>()
const emit = defineEmits<{ 'update:page': [value: number] }>()

/** windowed 0-based page numbers with gaps as -1 */
const pages = computed<number[]>(() => {
  const n = props.pageCount
  if (n <= 7) return Array.from({ length: n }, (_, i) => i)
  const cur = props.page
  const set = new Set([0, n - 1, cur, cur - 1, cur + 1])
  const sorted = [...set].filter(p => p >= 0 && p < n).sort((a, b) => a - b)
  const out: number[] = []
  let prev = -2
  for (const p of sorted) {
    if (p - prev > 1) out.push(-1)
    out.push(p)
    prev = p
  }
  return out
})

function go(p: number) {
  if (p < 0 || p >= props.pageCount || p === props.page) return
  emit('update:page', p)
}
</script>

<template>
  <nav v-if="pageCount > 1" class="flex items-center justify-center gap-1" aria-label="Pagination">
    <button
      type="button"
      class="rounded-md px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:hover:bg-transparent"
      :disabled="page === 0"
      @click="go(page - 1)"
    >
      ‹
    </button>
    <template v-for="(p, i) in pages" :key="i">
      <span v-if="p === -1" class="px-1.5 text-slate-400">…</span>
      <button
        v-else
        type="button"
        class="min-w-9 rounded-md px-2.5 py-1.5 text-sm"
        :class="p === page ? 'bg-brand-600 font-semibold text-white' : 'text-slate-600 hover:bg-slate-100'"
        :aria-current="p === page ? 'page' : undefined"
        @click="go(p)"
      >
        {{ p + 1 }}
      </button>
    </template>
    <button
      type="button"
      class="rounded-md px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:hover:bg-transparent"
      :disabled="page >= pageCount - 1"
      @click="go(page + 1)"
    >
      ›
    </button>
  </nav>
</template>
