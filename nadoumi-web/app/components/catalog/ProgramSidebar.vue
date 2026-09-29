<script setup lang="ts">
import type { ProgramDetail } from '~/types/catalog'
import { programTuition } from '~/utils/programDisplay'

const props = defineProps<{ program: ProgramDetail, applyTo: string, universityTo: string }>()

const { t } = useI18n()
const tuition = computed(() => programTuition(props.program))
</script>

<template>
  <aside class="space-y-5 lg:sticky lg:top-24 lg:self-start">
      <div v-if="tuition.cny" class="rounded-2xl border border-slate-200 bg-white p-5">
        <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ t('program.tuition') }}</p>
        <p class="mt-1 font-display text-2xl font-bold text-slate-900">{{ tuition.cny }}</p>
        <p v-if="tuition.usd" class="text-sm text-slate-500">≈ {{ tuition.usd }}</p>
        <p class="mt-2 text-xs text-slate-400">{{ t('program.tuitionNote') }}</p>
      </div>

      <div v-if="program.universityName" class="rounded-2xl border border-slate-200 bg-white p-5">
        <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ t('program.universityLabel') }}</p>
        <NuxtLink :to="universityTo" class="mt-1 block font-display text-base font-semibold text-slate-900 hover:text-brand-800">
          {{ program.universityName }}
        </NuxtLink>
        <NuxtLink :to="universityTo" class="mt-2 inline-flex items-center gap-1 text-sm font-medium text-brand-700 hover:text-brand-800">
          {{ t('program.view') }} <span aria-hidden="true">→</span>
        </NuxtLink>
      </div>

      <div class="rounded-2xl border border-brand-200 bg-brand-50 p-5">
        <p class="text-sm text-brand-900">{{ t('program.applyVia', { university: program.universityName ?? '' }) }}</p>
        <NButton :to="applyTo" size="sm" class="mt-3">{{ t('program.applyNow') }}</NButton>
      </div>
  </aside>
</template>
