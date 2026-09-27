<template>
  <div>
    <div class="ov__bar">
      <el-alert
        v-if="piiMasked"
        :title="t('applicant.piiMasked')"
        type="info"
        :closable="false"
        show-icon
        class="ov__notice"
      />
    </div>

    <div class="ov__photo">
      <span class="ov__photo-label">{{ t('applicant.photo') }}</span>
      <ImageUpload
        v-model="photoMediaId"
        :action="`/api/staff/applicants/${props.applicant.id}/photo`"
        :preview-url="photoUrl"
        aspect="square"
        disabled
        :disabled-hint="t('applicant.photoReadOnly')"
        @update:model-value="refreshPhoto"
      />
    </div>

    <DescriptionList :items="items" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getApplicantPhotoUrl, type Applicant } from '@/api/applicant'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import ImageUpload from '@/components/ui/ImageUpload.vue'
import type { DescriptionItem } from '@/components/ui/types'

const props = defineProps<{ applicant: Applicant }>()

const { t } = useI18n()
const MASK = '••••'
const piiMasked = computed(() =>
  props.applicant.dob === MASK || props.applicant.passportNo === MASK)

function titleCase(s: string) {
  return s ? s.charAt(0) + s.slice(1).toLowerCase().replace(/_/g, ' ') : s
}
function fmtDate(v: string | null): string {
  if (!v) return ''
  const d = new Date(v.replace(' ', 'T'))
  return Number.isNaN(d.getTime()) ? v : d.toLocaleDateString()
}

const items = computed<DescriptionItem[]>(() => {
  const a = props.applicant
  return [
    { label: t('applicant.given'), value: a.givenName },
    { label: t('applicant.family'), value: a.familyName },
    { label: t('applicant.dob'), value: a.dob },
    { label: t('applicant.nationality'), value: a.nationality },
    { label: t('applicant.passport'), value: a.passportNo },
    { label: t('applicant.email'), value: a.email },
    { label: t('applicant.phone'), value: a.phone },
    { label: t('applicant.status'), value: titleCase(a.status) },
    { label: t('applicant.registered'), value: fmtDate(a.createdAt) },
  ]
})

const photoUrl = ref<string | null>(null)
const photoMediaId = ref<number | null>(null)

async function refreshPhoto() {
  try {
    photoUrl.value = (await getApplicantPhotoUrl(props.applicant.id)).url
  }
  catch {
    photoUrl.value = null
  }
}
onMounted(refreshPhoto)
</script>

<style scoped>
.ov__bar {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}
.ov__notice {
  flex: 1;
}
.ov__photo {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
}
.ov__photo-label {
  font-size: 12px;
  font-weight: 550;
  color: var(--nad-ink-soft);
}
</style>
