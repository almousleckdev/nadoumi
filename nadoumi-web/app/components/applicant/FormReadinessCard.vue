<script setup lang="ts">
import type { ReadinessCode } from '~/types/family'

/** What the Nadoumi application form still lacks for this student, with a link to fill it in. Guidance only. */
const props = defineProps<{ applicantId: number }>()
const { t } = useI18n()
const localePath = useLocalePath()
const api = useApplicantFamily()

const missing = ref<ReadinessCode[]>([])
onMounted(async () => {
  missing.value = await api.readiness(props.applicantId).then(r => r.missing).catch(() => [])
})
</script>

<template>
  <SectionCard v-if="missing.length" :title="t('family.readiness.title')" data-test="readiness">
    <p class="text-sm text-slate-600">{{ t('family.readiness.body') }}</p>
    <ul class="mt-3 grid list-disc gap-1 ps-5 text-sm text-slate-700">
      <li v-for="code in missing" :key="code">{{ t(`family.readiness.${code}`) }}</li>
    </ul>
    <div class="mt-4">
      <NButton :to="localePath('/dashboard/family')" variant="secondary" size="sm">{{ t('family.readiness.fill') }}</NButton>
    </div>
  </SectionCard>
</template>
