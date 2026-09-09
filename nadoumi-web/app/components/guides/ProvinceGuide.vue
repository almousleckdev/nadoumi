<script setup lang="ts">
import type { Division } from '~/data/guides/china'

defineProps<{ division: Division }>()
</script>

<template>
  <div class="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8">
    <div class="flex flex-wrap items-baseline gap-x-3 gap-y-1">
      <h3 class="font-display text-xl font-bold text-slate-900">{{ division.name }}</h3>
      <span class="text-lg text-slate-400">{{ division.cn }}</span>
      <span class="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600">{{ division.kind }}</span>
    </div>

    <p class="mt-3 leading-7 text-slate-700">{{ division.overview }}</p>
    <p class="mt-3 leading-7 text-slate-700">{{ division.culture }}</p>

    <div class="mt-6 grid gap-4 sm:grid-cols-2">
      <div
        v-for="city in division.cities"
        :key="city.name"
        class="rounded-xl border border-slate-200 bg-slate-50/70 p-4"
      >
        <p class="font-semibold text-slate-900">
          {{ city.name }} <span class="font-normal text-slate-400">{{ city.cn }}</span>
        </p>
        <p class="mt-1 text-sm leading-6 text-slate-600">{{ city.note }}</p>
        <ul class="mt-3 space-y-1.5">
          <li v-for="u in city.universities" :key="u.name" class="text-sm text-slate-700">
            <span class="mr-1.5 inline-block h-1.5 w-1.5 rounded-full bg-brand-500 align-middle" aria-hidden="true" />
            {{ u.name }}<span v-if="u.cn" class="text-slate-400"> · {{ u.cn }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>
