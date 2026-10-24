<script setup lang="ts">
import { TRACKER_STEPS, trackerIndex } from '~/utils/applicationView'

/** A compact six-segment bar for table rows: how many steps of the path an application has reached. */
const props = defineProps<{ status: string | null }>()

const { t } = useI18n()
const reached = computed(() => trackerIndex(props.status) + 1)
const unsuccessful = computed(() => props.status === 'CLOSED_UNSUCCESSFUL')
</script>

<template>
  <div class="flex items-center gap-2" :title="t('dashboard.applications.stepOf', { n: reached, total: TRACKER_STEPS.length })">
    <span class="flex w-24 gap-0.5" aria-hidden="true">
      <span
        v-for="(step, i) in TRACKER_STEPS" :key="step" class="h-1.5 flex-1 rounded-full"
        :class="unsuccessful ? 'bg-red-200' : i < reached ? 'bg-slate-900' : 'bg-slate-200'"
      />
    </span>
    <span class="text-xs tabular-nums text-slate-500">{{ reached > 0 ? `${reached}/${TRACKER_STEPS.length}` : '' }}</span>
  </div>
</template>
