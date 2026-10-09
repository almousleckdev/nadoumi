<template>
  <StatePanel
    :loading="loading"
    :error="error"
    :empty="!data"
    :empty-title="t('applicant.detail.noInterests')"
    @retry="load"
  >
    <template v-if="data">
      <DescriptionList :items="items" />
      <div class="tags">
        <div>
          <h4 class="tags__title">
            {{ t('applicant.detail.fieldsOfStudy') }}
          </h4>
          <el-tag
            v-for="f in data.fields"
            :key="f"
            class="tags__tag"
            effect="light"
          >
            {{ f }}
          </el-tag>
        </div>
        <div>
          <h4 class="tags__title">
            {{ t('applicant.detail.cities') }}
          </h4>
          <el-tag
            v-for="c in data.cities"
            :key="c"
            class="tags__tag"
            type="warning"
            effect="light"
          >
            {{ c }}
          </el-tag>
        </div>
      </div>
    </template>
  </StatePanel>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getInterests, type ApplicantInterest } from '@/api/applicant'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import StatePanel from '@/components/ui/StatePanel.vue'
import type { DescriptionItem } from '@/components/ui/types'
import { codeLabel } from '@/utils/applicantLabels'

const props = defineProps<{ id: string }>()
const { t } = useI18n()
const data = ref<ApplicantInterest | null>(null)
const loading = ref(true)
const error = ref('')

const teaching = (code: string | null) => (code ? t(`applicant.detail.teaching.${code}`) : '')
const items = computed<DescriptionItem[]>(() => {
  const d = data.value
  if (!d) return []
  return [
    { label: t('applicant.detail.desiredLevel'), value: codeLabel(d.desiredLevel) },
    { label: t('applicant.detail.scholarshipInterest'), value: codeLabel(d.scholarshipInterest) },
    { label: t('applicant.detail.teachingLanguage'), value: teaching(d.teachingLanguage) },
    { label: t('applicant.detail.intakeYear'), value: d.intakeYear ? String(d.intakeYear) : '' },
    { label: t('applicant.detail.intakeTerm'), value: codeLabel(d.intakeTerm) },
    { label: t('applicant.detail.notes'), value: d.notes },
  ]
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await getInterests(props.id)
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

<style scoped>
.tags { display: grid; gap: 16px; margin-top: 16px; }
.tags__title { margin: 0 0 8px; font-size: 12px; font-weight: 600; color: var(--nad-ink-soft); text-transform: uppercase; letter-spacing: 0.04em; }
.tags__tag { margin: 0 8px 8px 0; }
</style>
