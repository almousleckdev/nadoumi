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

    <h3 class="ov__heading">
      {{ t('applicant.detail.sectionIdentity') }}
    </h3>
    <div class="ov__identity">
      <div class="ov__card">
        <span class="ov__photo-label">{{ t('applicant.photo') }}</span>
        <ImageUpload
          v-model="photoMediaId"
          :action="`/api/staff/applicants/${props.applicant.publicId}/photo`"
          :preview-url="photoUrl"
          aspect="square"
          :disabled="!canEdit"
          :disabled-hint="t('applicant.photoReadOnly')"
          @update:model-value="refreshPhoto"
        />
      </div>

      <div
        class="ov__card"
        data-test="passport-card"
      >
        <div class="ov__card-head">
          <span class="ov__photo-label">{{ t('applicant.detail.passportTitle') }}</span>
          <template v-if="passport?.passportNo">
            <el-tag
              :type="passport.validForAdmission ? 'success' : 'warning'"
              size="small"
              effect="light"
            >
              {{ passport.validForAdmission ? t('applicant.detail.passportValid') : t('applicant.detail.passportInvalid') }}
            </el-tag>
            <el-tag
              :type="passport.matchesProfile ? 'success' : 'danger'"
              size="small"
              effect="light"
            >
              {{ passport.matchesProfile ? t('applicant.detail.matchesProfile') : t('applicant.detail.mismatch') }}
            </el-tag>
          </template>
        </div>

        <a
          v-if="scanUrl"
          :href="scanUrl"
          target="_blank"
          rel="noopener"
          class="ov__scan"
          data-test="passport-scan"
        >
          <img
            :src="scanUrl"
            :alt="t('applicant.detail.passportScan')"
            @error="scanIsFile = true"
          >
          <span v-if="scanIsFile">{{ t('applicant.detail.openFile') }}</span>
        </a>
        <p
          v-else
          class="ov__none"
          data-test="passport-no-scan"
        >
          {{ t('applicant.detail.noScan') }}
        </p>

        <DescriptionList
          v-if="passport?.passportNo"
          :items="passportItems"
        />
        <p
          v-else
          class="ov__none"
        >
          {{ t('applicant.detail.noPassport') }}
        </p>
        <ul
          v-if="passport?.mismatches?.length"
          class="ov__mismatches"
        >
          <li
            v-for="m in passport.mismatches"
            :key="m.field"
          >
            {{ t('applicant.detail.mismatchRow', { field: codeLabel(m.field), passport: m.passportValue ?? '', profile: m.profileValue ?? '' }) }}
          </li>
        </ul>
      </div>
    </div>

    <h3 class="ov__heading">
      {{ t('applicant.detail.sectionPersonal') }}
    </h3>
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
import { codeLabel, countryName, languageName } from '@/utils/applicantLabels'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import {
  getApplicantPhotoUrl, getPassportScanUrl, getPassportStatus, updateApplicant,
  type Applicant, type ApplicantProfileInput, type PassportStatus,
} from '@/api/applicant'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import Drawer from '@/components/ui/Drawer.vue'
import ImageUpload from '@/components/ui/ImageUpload.vue'
import type { DescriptionItem } from '@/components/ui/types'
import { formatDate } from '@/utils/date'
import { titleCase } from '@/utils/text'

const props = defineProps<{ applicant: Applicant, canEdit: boolean }>()
const emit = defineEmits<{ updated: [] }>()

const { t, locale } = useI18n()
const MASK = '••••'
// The backend serializes a hidden value as the literal MASK string; a genuinely
// empty field comes back as null. So MASK ⇒ "there is a value you may not see".
const piiMasked = computed(() =>
  props.applicant.dob === MASK || props.applicant.passportNo === MASK)

/** Everything the student typed on the personal step, plus where they stand. */
const items = computed<DescriptionItem[]>(() => {
  const a = props.applicant
  const place = (code: string | null) => countryName(code, locale.value)
  return [
    { label: t('applicant.given'), value: a.givenName },
    { label: t('applicant.family'), value: a.familyName },
    { label: t('applicant.dob'), value: a.dob },
    { label: t('applicant.detail.gender'), value: codeLabel(a.gender) },
    { label: t('applicant.nationality'), value: place(a.nationality) },
    { label: t('applicant.detail.countryOfOrigin'), value: place(a.countryOfOrigin) },
    { label: t('applicant.detail.countryOfResidence'), value: place(a.countryOfResidence) },
    { label: t('applicant.detail.nativeLanguage'), value: languageName(a.nativeLanguage, locale.value) },
    { label: t('applicant.passport'), value: a.passportNo },
    { label: t('applicant.email'), value: a.email ? `${a.email}${a.emailVerified ? ` (${t('applicant.detail.verified')})` : ''}` : '' },
    { label: t('applicant.phone'), value: a.phone },
    { label: t('applicant.detail.wechat'), value: a.wechatId },
    { label: t('applicant.detail.whatsapp'), value: a.whatsapp },
    { label: t('applicant.detail.onboarding'), value: a.onboardingComplete ? t('applicant.detail.onboardingDone') : t('applicant.detail.onboardingPending') },
    { label: t('applicant.status'), value: titleCase(a.status) },
    { label: t('applicant.registered'), value: formatDate(a.createdAt) },
  ]
})

const passport = ref<PassportStatus | null>(null)
const scanUrl = ref<string | null>(null)
const scanIsFile = ref(false)
const passportItems = computed<DescriptionItem[]>(() => {
  const p = passport.value
  if (!p) return []
  return [
    { label: t('applicant.passport'), value: p.passportNo },
    { label: t('applicant.detail.passportGiven'), value: p.givenName },
    { label: t('applicant.detail.passportFamily'), value: p.familyName },
    { label: t('applicant.dob'), value: p.dob },
    { label: t('applicant.detail.issueDate'), value: p.issueDate },
    { label: t('applicant.detail.expiryDate'), value: p.expiryDate },
  ]
})

async function loadPassport() {
  try {
    passport.value = await getPassportStatus(props.applicant.publicId)
    scanUrl.value = passport.value.scanUploaded ? (await getPassportScanUrl(props.applicant.publicId)).url : null
  }
  catch {
    scanUrl.value = null
  }
}
onMounted(loadPassport)

// The photo is a protected asset: display it through a short-lived signed URL
// fetched on load (and re-fetched after an upload). `photoMediaId` only drives
// the upload widget — it is not persisted through this tab.
const photoUrl = ref<string | null>(null)
const photoMediaId = ref<number | null>(null)

async function refreshPhoto() {
  try {
    photoUrl.value = (await getApplicantPhotoUrl(props.applicant.publicId)).url
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
    await updateApplicant(props.applicant.publicId, body)
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
.ov__heading { margin: 20px 0 10px; font-size: 14px; font-weight: 650; color: var(--nad-ink); }
.ov__identity { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 16px; }
.ov__card { display: flex; flex-direction: column; gap: 10px; padding: 16px; border: 1px solid var(--nad-line); border-radius: 12px; background: #fff; }
.ov__card-head { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.ov__scan { display: block; overflow: hidden; border: 1px solid var(--nad-line); border-radius: 8px; background: #f8fafc; text-align: center; color: var(--nad-brand-700); font-size: 13px; }
.ov__scan img { display: block; width: 100%; max-height: 240px; object-fit: contain; }
.ov__none { margin: 0; padding: 20px 0; text-align: center; font-size: 13px; color: var(--nad-ink-soft); }
.ov__mismatches { margin: 0; padding-inline-start: 18px; font-size: 13px; color: #b91c1c; }
.ov__photo-label {
  font-size: 12px;
  font-weight: 550;
  color: var(--nad-ink-soft);
}
</style>
