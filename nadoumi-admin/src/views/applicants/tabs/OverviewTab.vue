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
      <el-button
        v-if="canEdit"
        size="small"
        :icon="Edit"
        class="ov__edit"
        @click="openEdit"
      >
        {{ t('applicant.editProfile') }}
      </el-button>
    </div>

    <div class="ov__photo">
      <span class="ov__photo-label">{{ t('applicant.photo') }}</span>
      <ImageUpload
        v-model="photoMediaId"
        :action="`/api/staff/applicants/${props.applicant.id}/photo`"
        :preview-url="photoUrl"
        aspect="square"
        :disabled="!canEdit"
        :disabled-hint="t('applicant.photoReadOnly')"
        @update:model-value="refreshPhoto"
      />
    </div>

    <DescriptionList :items="items" />

    <Drawer
      v-model="open"
      :title="t('applicant.editProfile')"
      :saving="saving"
      @save="save"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <div class="row2">
          <el-form-item
            :label="t('applicant.given')"
            prop="givenName"
          >
            <el-input v-model="form.givenName" />
          </el-form-item>
          <el-form-item
            :label="t('applicant.family')"
            prop="familyName"
          >
            <el-input v-model="form.familyName" />
          </el-form-item>
        </div>
        <div class="row2">
          <el-form-item :label="t('applicant.dob')">
            <el-date-picker
              v-model="form.dob"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              :disabled="piiMasked"
            />
          </el-form-item>
          <el-form-item :label="t('applicant.nationality')">
            <el-input
              v-model="form.nationality"
              maxlength="2"
              placeholder="ISO alpha-2"
            />
          </el-form-item>
        </div>
        <el-form-item :label="t('applicant.passport')">
          <el-input
            v-model="form.passportNo"
            :disabled="piiMasked"
            :placeholder="piiMasked ? t('applicant.piiLocked') : ''"
          />
        </el-form-item>
        <div class="row2">
          <el-form-item :label="t('applicant.email')">
            <el-input v-model="form.email" />
          </el-form-item>
          <el-form-item :label="t('applicant.phone')">
            <el-input v-model="form.phone" />
          </el-form-item>
        </div>
      </el-form>
    </Drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import {
  getApplicantPhotoUrl, updateApplicant,
  type Applicant, type ApplicantProfileInput,
} from '@/api/applicant'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import Drawer from '@/components/ui/Drawer.vue'
import ImageUpload from '@/components/ui/ImageUpload.vue'
import type { DescriptionItem } from '@/components/ui/types'

const props = defineProps<{ applicant: Applicant, canEdit: boolean }>()
const emit = defineEmits<{ updated: [] }>()

const { t } = useI18n()
const MASK = '••••'
// The backend serializes a hidden value as the literal MASK string; a genuinely
// empty field comes back as null. So MASK ⇒ "there is a value you may not see".
const piiMasked = computed(() =>
  props.applicant.dob === MASK || props.applicant.passportNo === MASK)

function titleCase(s: string) {
  return s ? s.charAt(0) + s.slice(1).toLowerCase().replace(/_/g, ' ') : s
}
function fmtDate(v: string | null): string {
  if (!v) return '—'
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

// The photo is a protected asset: display it through a short-lived signed URL
// fetched on load (and re-fetched after an upload). `photoMediaId` only drives
// the upload widget — it is not persisted through this tab.
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

const open = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  givenName: '', familyName: '', dob: '', nationality: '', passportNo: '', email: '', phone: '',
})
const rules = {
  givenName: [{ required: true, trigger: 'blur', message: t('applicant.required') }],
  familyName: [{ required: true, trigger: 'blur', message: t('applicant.required') }],
}

function openEdit() {
  const a = props.applicant
  Object.assign(form, {
    givenName: a.givenName, familyName: a.familyName,
    dob: a.dob === MASK ? '' : (a.dob ?? ''),
    nationality: a.nationality ?? '',
    passportNo: a.passportNo === MASK ? '' : (a.passportNo ?? ''),
    email: a.email ?? '', phone: a.phone ?? '',
  })
  open.value = true
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const body: ApplicantProfileInput = {
      givenName: form.givenName.trim(),
      familyName: form.familyName.trim(),
      nationality: form.nationality || null,
      email: form.email || null,
      phone: form.phone || null,
    }
    // only send PII fields the caller can actually see/change
    if (!piiMasked.value) {
      body.dob = form.dob || null
      body.passportNo = form.passportNo || null
    }
    await updateApplicant(props.applicant.id, body)
    ElMessage.success(t('common.saved'))
    open.value = false
    emit('updated')
  }
  finally {
    saving.value = false
  }
}
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
.ov__edit {
  flex-shrink: 0;
  margin-left: auto;
}
.row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
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
