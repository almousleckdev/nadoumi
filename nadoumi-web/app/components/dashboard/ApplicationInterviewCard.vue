<script setup lang="ts">
import type { InterviewDto } from '~/types/applicationExtras'
import { BEIJING_ZONE, currentInterview, formatInZone, viewerIsInBeijing, viewerZone } from '~/utils/interviewTime'

/** The interview Nadoumi scheduled, in Beijing time (its own clock) and on the student's own clock. */
const props = defineProps<{ applicationId: number, status: string | null }>()

const { t, locale } = useI18n()
const { interviews } = useApplicationExtras(props.applicationId)

const list = ref<InterviewDto[]>([])
const loaded = ref(false)

onMounted(async () => {
  try {
    list.value = await interviews()
  }
  catch {
    list.value = []
  }
  loaded.value = true
})

const interview = computed(() => currentInterview(list.value))
const waiting = computed(() => loaded.value && !interview.value && props.status === 'INTERVIEW_REQUIRED')
</script>

<template>
  <SectionCard v-if="interview || waiting" :title="t('dashboard.applications.interview.title')">
    <p v-if="waiting" class="text-sm text-slate-600" data-test="interview-waiting">{{ t('dashboard.applications.interview.waiting') }}</p>
    <div v-else-if="interview" class="grid gap-3" data-test="interview">
      <div>
        <p class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('dashboard.applications.interview.beijing') }}</p>
        <p class="text-base font-semibold text-slate-900" data-test="interview-beijing">{{ formatInZone(interview.startsAt, BEIJING_ZONE, locale) }}</p>
      </div>
      <div v-if="!viewerIsInBeijing()">
        <p class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('dashboard.applications.interview.yours', { zone: viewerZone() }) }}</p>
        <p class="text-base font-semibold text-slate-900" data-test="interview-local">{{ formatInZone(interview.startsAt, viewerZone(), locale) }}</p>
      </div>
      <p class="text-sm text-slate-600">
        {{ t('dashboard.applications.interview.length', { minutes: interview.durationMinutes }) }}<template v-if="interview.platform"> · {{ interview.platform }}</template>
      </p>
      <p v-if="interview.instructions" class="whitespace-pre-wrap text-sm text-slate-700">{{ interview.instructions }}</p>
      <div>
        <NButton :to="interview.meetingUrl" target="_blank" rel="noopener" data-test="interview-join">{{ t('dashboard.applications.interview.join') }}</NButton>
      </div>
    </div>
  </SectionCard>
</template>
