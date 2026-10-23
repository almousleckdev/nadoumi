<script setup lang="ts">
import type { ProgramDetail } from '~/types/catalog'
import type { RequiredDocument } from '~/components/catalog/RequiredDocumentsCard.vue'

const props = defineProps<{ program: ProgramDetail, documents: RequiredDocument[] }>()

const { t } = useI18n()

const languageItems = computed(() => [
  props.program.minHsk != null ? { text: t('program.admission.hsk', { n: props.program.minHsk }) } : null,
  props.program.minIelts != null ? { text: t('program.admission.ielts', { n: props.program.minIelts }) } : null,
  props.program.minToefl != null ? { text: t('program.admission.toefl', { n: props.program.minToefl }) } : null,
].filter((i): i is { text: string } => i !== null))

// programmes saved before the structured list existed still carry a free-text list; it is shown until one is set
const legacyDocuments = computed(() => props.documents.length
  ? []
  : (props.program.applicationDocuments ?? '').split('\n').map(line => line.trim()).filter(Boolean).map(text => ({ text })))

const prose = computed(() => [
  { title: t('program.admission.scholarships'), body: props.program.scholarshipInfo },
  { title: t('program.admission.career'), body: props.program.careerProspects },
].filter(p => p.body))
</script>

<template>
  <DetailCard
    v-if="program.admissionRequirements || languageItems.length"
    :title="t('program.admission.requirements')"
  >
    <p v-if="program.admissionRequirements" class="whitespace-pre-line leading-7 text-slate-700">{{ program.admissionRequirements }}</p>
    <div v-if="languageItems.length" :class="program.admissionRequirements ? 'mt-5' : ''">
      <h3 class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-500">{{ t('program.admission.language') }}</h3>
      <BulletList :items="languageItems" :columns="2" />
    </div>
  </DetailCard>

  <RequiredDocumentsCard :documents="documents" />
  <DetailCard v-if="legacyDocuments.length" :title="t('program.admission.documents')">
    <BulletList :items="legacyDocuments" :columns="2" />
  </DetailCard>

  <DetailCard v-for="p in prose" :key="p.title" :title="p.title">
    <p class="whitespace-pre-line leading-7 text-slate-700">{{ p.body }}</p>
  </DetailCard>
</template>
