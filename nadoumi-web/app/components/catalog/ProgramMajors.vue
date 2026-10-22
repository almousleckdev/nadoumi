<script setup lang="ts">
import type { ProgramMajor } from '~/types/catalog'
import { groupMajors } from '~/utils/programDisplay'

const props = defineProps<{ majors: ProgramMajor[] }>()

const { t } = useI18n()
const majorGroups = computed(() => groupMajors(props.majors))
</script>

<template>
  <DetailCard :title="t('program.majors')" :description="t('program.majorsIntro')">
    <div v-if="majorGroups.length" class="space-y-5">
      <div v-for="g in majorGroups" :key="g.dept">
        <p v-if="g.dept" class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ g.dept }}</p>
        <ul class="mt-2 grid gap-2 sm:grid-cols-2">
          <li
            v-for="m in g.majors"
            :key="m.id"
            class="flex items-start gap-2 rounded-lg bg-slate-50 px-3 py-2.5 text-sm text-slate-700"
          >
            <span class="mt-0.5 shrink-0 text-brand-600"><DashboardIcon name="check" :size="16" /></span>
            <span class="min-w-0 break-words">{{ m.name }}<span v-if="m.nameCn" class="text-slate-500"> · {{ m.nameCn }}</span></span>
          </li>
        </ul>
      </div>
    </div>
    <p v-else class="text-sm text-slate-500">{{ t('program.noMajors') }}</p>
  </DetailCard>
</template>
