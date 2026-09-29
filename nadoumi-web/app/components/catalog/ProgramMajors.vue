<script setup lang="ts">
import type { ProgramMajor } from '~/types/catalog'
import { groupMajors } from '~/utils/programDisplay'

const props = defineProps<{ majors: ProgramMajor[] }>()

const { t } = useI18n()
const majorGroups = computed(() => groupMajors(props.majors))
</script>

<template>
  <section>
    <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('program.majors') }}</h2>
    <p class="mt-1 text-sm text-slate-500">{{ t('program.majorsIntro') }}</p>
    <div v-if="majorGroups.length" class="mt-4 space-y-5">
      <div v-for="g in majorGroups" :key="g.dept">
        <p v-if="g.dept" class="text-xs font-semibold uppercase tracking-wide text-brand-600">{{ g.dept }}</p>
        <ul class="mt-2 grid gap-2 sm:grid-cols-2">
          <li
            v-for="m in g.majors"
            :key="m.id"
            class="flex items-start gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-700"
          >
            <span class="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" aria-hidden="true" />
            <span>{{ m.name }}<span v-if="m.nameCn" class="text-slate-400"> · {{ m.nameCn }}</span></span>
          </li>
        </ul>
      </div>
    </div>
    <p v-else class="mt-3 text-sm text-slate-500">{{ t('program.noMajors') }}</p>
  </section>
</template>
