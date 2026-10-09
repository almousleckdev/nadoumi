<template>
  <StatePanel
    :loading="loading"
    :error="error"
    :empty="!data"
    :empty-title="t('applicant.detail.noLocation')"
    @retry="load"
  >
    <DescriptionList :items="items" />
  </StatePanel>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getResidence, type ApplicantResidence } from '@/api/applicant'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import StatePanel from '@/components/ui/StatePanel.vue'
import type { DescriptionItem } from '@/components/ui/types'
import { codeLabel, countryName } from '@/utils/applicantLabels'

const props = defineProps<{ id: string }>()
const { t, locale } = useI18n()
const data = ref<ApplicantResidence | null>(null)
const loading = ref(true)
const error = ref('')

const items = computed<DescriptionItem[]>(() => {
  const d = data.value
  if (!d) return []
  const base: DescriptionItem[] = [
    { label: t('applicant.detail.inChina'), value: d.inChina ? t('common.yes') : t('common.no') },
    { label: t('applicant.detail.country'), value: countryName(d.country, locale.value) },
    { label: t('applicant.detail.city'), value: d.city },
    { label: t('applicant.detail.address'), value: d.address },
  ]
  if (!d.inChina) return base
  return [
    ...base,
    { label: t('applicant.detail.chinaLevel'), value: codeLabel(d.chinaEducationLevel) },
    { label: t('applicant.detail.chinaSchool'), value: d.chinaSchool },
    { label: t('applicant.detail.visaType'), value: codeLabel(d.visaType) },
    { label: t('applicant.detail.visaExpiry'), value: d.visaExpiryDate },
  ]
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await getResidence(props.id)
  }
  catch (e) {
    error.value = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    loading.value = false
  }
}
onMounted(load)
</script>
