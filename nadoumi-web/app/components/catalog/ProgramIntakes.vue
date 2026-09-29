<script setup lang="ts">
import type { ProgramDetail } from '~/types/catalog'

defineProps<{ intakes: ProgramDetail['intakes'] }>()

const { t } = useI18n()
</script>

<template>
  <section v-if="intakes.length">
    <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('program.intakes') }}</h2>
    <div class="mt-3 overflow-x-auto">
      <table class="w-full min-w-[30rem] overflow-hidden rounded-xl border border-slate-200 text-left text-sm">
        <thead class="bg-slate-50 text-xs font-medium uppercase tracking-wide text-slate-500">
          <tr>
            <th class="px-4 py-2.5">{{ t('program.term') }}</th>
            <th class="px-4 py-2.5">{{ t('program.opens') }}</th>
            <th class="px-4 py-2.5">{{ t('program.closes') }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="i in intakes" :key="i.id" class="odd:bg-white even:bg-slate-50/40">
            <td class="px-4 py-3 font-medium text-slate-900">{{ t(`program.intake.${i.term}`, i.term) }}</td>
            <td class="px-4 py-3 text-slate-600">{{ i.applicationOpen || t('program.rolling') }}</td>
            <td class="px-4 py-3 text-slate-600">{{ i.applicationClose || t('program.rolling') }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
