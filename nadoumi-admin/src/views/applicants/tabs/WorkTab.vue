<template>
  <StatePanel
    :loading="loading"
    :error="error"
    :empty="data.length === 0"
    :empty-title="t('applicant.detail.noWork')"
    @retry="load"
  >
    <ul class="jobs">
      <li
        v-for="w in data"
        :key="w.id"
        class="job"
        data-test="job"
      >
        <div class="job__head">
          <h4 class="job__title">
            {{ w.jobTitle }} · {{ w.employer }}
          </h4>
          <el-tag
            v-if="w.employmentType"
            size="small"
            effect="light"
          >
            {{ codeLabel(w.employmentType) }}
          </el-tag>
        </div>
        <p class="job__meta">
          {{ [place(w), period(w)].filter(Boolean).join(' · ') }}
        </p>
        <p
          v-if="w.description"
          class="job__text"
        >
          {{ w.description }}
        </p>
        <p
          v-if="w.workVisaType"
          class="job__meta"
        >
          {{ t('applicant.detail.workVisa', { type: codeLabel(w.workVisaType), expiry: w.workVisaExpiry ?? '' }) }}
        </p>
      </li>
    </ul>
  </StatePanel>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listWork, type ApplicantWork } from '@/api/applicant'
import StatePanel from '@/components/ui/StatePanel.vue'
import { codeLabel, countryName } from '@/utils/applicantLabels'

const props = defineProps<{ id: string }>()
const emit = defineEmits<{ (e: 'count', n: number): void }>()
const { t, locale } = useI18n()
const data = ref<ApplicantWork[]>([])
const loading = ref(true)
const error = ref('')

const place = (w: ApplicantWork) => [w.city, countryName(w.country, locale.value)].filter(Boolean).join(', ')
const period = (w: ApplicantWork) => `${w.startDate} - ${w.current ? t('applicant.present') : (w.endDate ?? '')}`

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await listWork(props.id)
    emit('count', data.value.length)
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
.jobs { display: grid; gap: 12px; margin: 0; padding: 0; list-style: none; }
.job { padding: 16px; border: 1px solid var(--nad-line); border-radius: 12px; background: #fff; }
.job__head { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px; }
.job__title { margin: 0; font-size: 15px; font-weight: 650; color: var(--nad-ink); }
.job__meta { margin: 4px 0 0; font-size: 13px; color: var(--nad-ink-soft); }
.job__text { margin: 8px 0 0; font-size: 14px; color: var(--nad-ink); white-space: pre-wrap; }
</style>
