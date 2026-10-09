<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { EDUCATION_LEVELS, type ScholarshipView } from '@/api/scholarship'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import FormSection from '@/components/ui/FormSection.vue'
import { assetUrl } from '@/utils/asset'

const props = defineProps<{ view: ScholarshipView }>()

const { t } = useI18n()

// "Engineering" when every level shares the fields, or "All levels: Engineering · Master's: Medicine" when they differ.
const fieldsSummary = computed(() => {
  const list = props.view.fields ?? []
  if (!list.length) return props.view.field || ''
  const shared = list.filter(f => !f.level).map(f => f.name)
  const byLevel = EDUCATION_LEVELS
    .map(level => ({ level, names: list.filter(f => f.level === level).map(f => f.name) }))
    .filter(entry => entry.names.length > 0)
  if (!byLevel.length) return shared.join(', ')
  const parts = byLevel.map(e => `${t(`scholarship.level.${e.level}`)}: ${e.names.join(', ')}`)
  return (shared.length ? [`${t('scholarship.fieldsAllShort')}: ${shared.join(', ')}`, ...parts] : parts).join(' · ')
})

const heroSrc = computed(() => assetUrl(props.view.heroUrl ?? props.view.heroImageUrl))
const coverSrc = computed(() => assetUrl(props.view.coverUrl ?? props.view.coverImageUrl))

function nationalityText(e: NonNullable<ScholarshipView['eligibility']>) {
  if (e.nationalityScope === 'INCLUDE') return `${t('scholarship.scope.INCLUDE')}: ${e.acceptedCountries || ''}`
  if (e.nationalityScope === 'EXCLUDE') return `${t('scholarship.scope.EXCLUDE')}: ${e.acceptedCountries || ''}`
  return t('scholarship.scope.ANY')
}

const eligibilityItems = computed(() => {
  const e = props.view.eligibility
  if (!e) return []
  const items: { label: string, value: string | number }[] = []
  if (e.ageMin != null || e.ageMax != null) items.push({ label: t('scholarship.age'), value: `${e.ageMin ?? ''} to ${e.ageMax ?? ''}`.trim() })
  items.push({ label: t('scholarship.nationality'), value: nationalityText(e) })
  if (e.inChina != null) items.push({ label: t('scholarship.inChina'), value: e.inChina ? t('common.yes') : t('common.no') })
  if (e.gpaMin != null) items.push({ label: 'GPA', value: `≥ ${e.gpaMin}` })
  if (e.ieltsMin != null) items.push({ label: 'IELTS', value: `≥ ${e.ieltsMin}` })
  if (e.toeflMin != null) items.push({ label: 'TOEFL', value: `≥ ${e.toeflMin}` })
  if (e.hskMin != null) items.push({ label: 'HSK', value: `≥ ${e.hskMin}` })
  return items
})
</script>

<template>
  <FormSection
    v-if="heroSrc || coverSrc"
    :title="t('scholarship.secMedia')"
  >
    <div class="imgs">
      <figure v-if="heroSrc">
        <img
          :src="heroSrc"
          alt=""
        >
        <figcaption>{{ t('scholarship.heroImage') }}</figcaption>
      </figure>
      <figure v-if="coverSrc">
        <img
          :src="coverSrc"
          alt=""
        >
        <figcaption>{{ t('scholarship.coverImage') }}</figcaption>
      </figure>
    </div>
  </FormSection>

  <FormSection :title="t('scholarship.secIdentity')">
    <DescriptionList
      :items="[
        { label: t('scholarship.referenceCode'), value: view.referenceCode || t('scholarship.refPending') },
        { label: t('scholarship.slug'), value: view.slug },
        { label: t('scholarship.field'), value: fieldsSummary },
        { label: t('scholarship.teachingLanguage'), value: view.teachingLanguage ? t(`scholarship.lang.${view.teachingLanguage}`) : '' },
        { label: t('scholarship.deadline'), value: view.deadline || t('scholarship.rolling') },
        { label: t('scholarship.levels'), value: view.levels.map(l => t(`scholarship.level.${l}`)).join(', ') || '' },
        { label: t('scholarship.nonDegreeDuration'), value: view.nonDegreeDuration ? t(`scholarship.nonDegree.${view.nonDegreeDuration}`) : '' },
        { label: t('scholarship.studyDurationMonths'), value: view.studyDurationMonths ?? '' },
        { label: t('scholarship.applicationChannel'), value: view.applicationChannel ? t(`scholarship.channel.${view.applicationChannel}`) : '' },
        { label: t('scholarship.agencyNumber'), value: view.agencyNumber || '' },
        { label: t('scholarship.requiresFinancialProof'), value: view.requiresFinancialProof ? t('common.yes') : t('common.no') },
        { label: t('scholarship.requiresFoundationYear'), value: view.requiresFoundationYear ? t('common.yes') : t('common.no') },
        { label: t('scholarship.categories'), value: view.categories.join(', ') || '' },
        { label: t('scholarship.slots'), value: view.slots ?? '' },
      ]"
    />
    <p
      v-if="view.summary"
      class="prose"
    >
      {{ view.summary }}
    </p>
  </FormSection>

  <FormSection
    v-if="view.eligibility"
    :title="t('scholarship.secEligibility')"
  >
    <DescriptionList :items="eligibilityItems" />
    <p
      v-if="view.eligibility.notes"
      class="prose"
    >
      {{ view.eligibility.notes }}
    </p>
  </FormSection>
</template>

<style scoped src="./detail.css" />
