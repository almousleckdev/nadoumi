<script setup lang="ts" generic="Row extends object">
/**
 * A plain data table. Wide screens get a real table; narrow screens stack each row into a labelled card so the page
 * never scrolls sideways. Cells are filled through `cell-<key>` slots, anything else prints `row[key]`.
 */
export interface NTableColumn { key: string, label: string, class?: string }

defineProps<{ columns: NTableColumn[], rows: Row[], rowKey: string }>()
</script>

<template>
  <div class="min-w-0 overflow-hidden rounded-lg border border-slate-200 bg-white">
    <table class="block w-full text-sm md:table">
      <thead class="hidden border-b border-slate-200 bg-slate-50 text-start text-xs font-semibold uppercase tracking-wide text-slate-500 md:table-header-group">
        <tr>
          <th v-for="c in columns" :key="c.key" scope="col" class="px-4 py-3 text-start font-semibold" :class="c.class">{{ c.label }}</th>
        </tr>
      </thead>
      <tbody class="block divide-y divide-slate-200 md:table-row-group">
        <tr v-for="row in rows" :key="String((row as Record<string, unknown>)[rowKey])" class="block px-4 py-3 md:table-row md:p-0" data-test="table-row">
          <td
            v-for="c in columns"
            :key="c.key"
            :data-label="c.label"
            class="flex min-w-0 items-baseline justify-between gap-4 py-1 before:shrink-0 before:text-xs before:font-medium before:text-slate-500 before:content-[attr(data-label)] md:table-cell md:px-4 md:py-3 md:align-middle md:before:content-none"
            :class="c.class"
          >
            <span class="min-w-0 text-end md:text-start">
              <slot :name="`cell-${c.key}`" :row="row">{{ (row as Record<string, unknown>)[c.key] }}</slot>
            </span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
