import os

base = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/applicants/tabs"

ov = """<template>
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
const emit = defineEmits<{ updated: [] }>()

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
"""
with open(os.path.join(base, "OverviewTab.vue"), "w") as f:
    f.write(ov)


ed = """<template>
  <div class="edu">
    <AsyncList ref="list" :fetch="() => fetchEducation(props.id)">
      <template #default="{ items }">
        <div v-if="items.length === 0" class="edu__empty">
          {{ t('applicant.noEducation') }}
        </div>
        <div v-for="edu in items" :key="edu.id" class="edu__card">
          <div class="edu__main">
            <div class="edu__degree">{{ edu.degree }}</div>
            <div class="edu__school">{{ edu.school }}</div>
            <div class="edu__major">{{ edu.major }}</div>
            <div class="edu__dates">
              {{ edu.startDate }} — {{ edu.endDate ?? t('applicant.present') }}
            </div>
            <div class="edu__gpa" v-if="edu.gpa">
              GPA: {{ edu.gpa }}
            </div>
          </div>
        </div>
      </template>
    </AsyncList>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchEducation, type Education } from '@/api/applicant'
import AsyncList from '@/components/ui/AsyncList.vue'

const props = defineProps<{ id: string }>()
const { t } = useI18n()
const list = ref<InstanceType<typeof AsyncList>>()
</script>

<style scoped>
.edu { padding: 4px; }
.edu__empty {
  color: var(--nad-ink-soft);
  font-size: 13px;
  text-align: center;
  padding: 24px;
}
.edu__card {
  border: 1px solid var(--nad-border);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.edu__main { flex: 1; }
.edu__degree { font-weight: 600; font-size: 15px; margin-bottom: 4px; }
.edu__school { font-size: 14px; margin-bottom: 2px; }
.edu__major { font-size: 13px; color: var(--nad-ink-soft); margin-bottom: 6px; }
.edu__dates, .edu__gpa { font-size: 13px; color: var(--nad-ink-soft); }
</style>
"""
with open(os.path.join(base, "EducationTab.vue"), "w") as f:
    f.write(ed)


co = """<template>
  <div class="con">
    <AsyncList ref="list" :fetch="() => fetchContacts(props.id)">
      <template #default="{ items }">
        <el-table :data="items" size="small" style="width: 100%" empty-text="No contacts">
          <el-table-column prop="relation" :label="t('applicant.contactRelation')" width="120" />
          <el-table-column prop="name" :label="t('applicant.contactName')" min-width="120" />
          <el-table-column prop="phone" :label="t('applicant.phone')" width="140" />
          <el-table-column prop="email" :label="t('applicant.email')" min-width="180" />
        </el-table>
      </template>
    </AsyncList>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchContacts, type Contact } from '@/api/applicant'
import AsyncList from '@/components/ui/AsyncList.vue'

const props = defineProps<{ id: string }>()
const { t } = useI18n()
const list = ref<InstanceType<typeof AsyncList>>()
</script>

<style scoped>
.con { padding: 4px; }
</style>
"""
with open(os.path.join(base, "ContactsTab.vue"), "w") as f:
    f.write(co)
