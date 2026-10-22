<script setup lang="ts">
import type { ProgramDetail } from '~/types/catalog'

defineProps<{ intakes: ProgramDetail['intakes'] }>()

const { t } = useI18n()
</script>

<template>
  <DetailCard v-if="intakes.length" :title="t('program.intakes')">
    <ul class="divide-y divide-slate-100">
      <li
        v-for="i in intakes"
        :key="i.id"
        class="flex flex-col gap-1 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-baseline sm:justify-between sm:gap-6"
      >
        <span class="font-medium text-slate-900">{{ t(`program.intake.${i.term}`, i.term) }}</span>
        <span class="text-sm text-slate-600">
          {{ t('program.opens') }}: {{ i.applicationOpen || t('program.rolling') }}
          <span class="mx-1 text-slate-300" aria-hidden="true">/</span>
          {{ t('program.closes') }}: {{ i.applicationClose || t('program.rolling') }}
        </span>
      </li>
    </ul>
  </DetailCard>
</template>
