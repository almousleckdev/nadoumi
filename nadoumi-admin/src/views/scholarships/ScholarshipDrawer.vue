<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import {
  createScholarship, updateScholarship, getScholarship, listScholarshipCategories,
  EDUCATION_LEVELS, FEE_KINDS, DOC_TYPES, INTAKE_TERMS, ROOM_TYPES, NON_DEGREE_DURATIONS,
  COVERAGE_KINDS, APPLICATION_CHANNELS,
  type Scholarship, type ScholarshipInput, type ScholarshipCategoryOption,
  type EducationLevel, type FeeKind, type NationalityScope,
  type RoomType, type NonDegreeDuration, type StipendFrequency,
  type CoverageKind, type ApplicationChannel,
} from '@/api/scholarship'
import { cnyToUsdRate } from '@/api/fx'
import { usd } from '@/utils/money'
import Drawer from '@/components/ui/Drawer.vue'
import FormSection from '@/components/ui/FormSection.vue'
import ImageUpload from '@/components/ui/ImageUpload.vue'

const props = defineProps<{ modelValue: boolean, scholarship: Scholarship | null }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [s: Scholarship] }>()

const { t } = useI18n()
const LANGS = ['ENGLISH', 'CHINESE', 'BOTH'] as const
const FUNDING = ['FULLY', 'PARTIAL', 'SELF'] as const
const SCOPES: NationalityScope[] = ['ANY', 'INCLUDE', 'EXCLUDE']
const FREQ = ['MONTHLY', 'YEARLY', 'ONE_OFF'] as const
const STATUSES = ['ACTIVE', 'INACTIVE'] as const
const PUBLISH = ['DRAFT', 'PUBLISHED'] as const

const formRef = ref<FormInstance>()
const saving = ref(false)
// create mode holds hero/cover until the scholarship row exists
const heroUp = ref<InstanceType<typeof ImageUpload>>()
const coverUp = ref<InstanceType<typeof ImageUpload>>()
const categories = ref<ScholarshipCategoryOption[]>([])
listScholarshipCategories().then(c => categories.value = c).catch(() => {})

// live RMB → USD preview; every amount is entered in RMB
const fxRate = ref(0.1381)
cnyToUsdRate().then(r => fxRate.value = r).catch(() => {})
function usdHint(amount: number | null): string {
  return amount ? `≈ ${usd(amount * fxRate.value)}` : ''
}

function blankEligibility() {
  return {
    ageMin: null as number | null, ageMax: null as number | null,
    nationalityScope: 'ANY' as NationalityScope, acceptedCountries: '',
    inChina: null as boolean | null,
    gpaMin: null as number | null, ieltsMin: null as number | null, toeflMin: null as number | null,
    duolingoMin: null as number | null, hskMin: null as number | null, cscaMin: null as number | null,
    notes: '',
  }
}
function blankForm() {
  return {
    title: '', summary: '', country: '', province: '', city: '', field: '',
    teachingLanguage: null as string | null,
    fundingModel: 'FULLY' as ScholarshipInput['fundingModel'],
    categoryCodes: [] as string[],
    levels: [] as EducationLevel[],
    benefits: '', requirements: '', policy: '', renewalConditions: '',
    applicationFeeAmount: null as number | null, applicationFeeCurrency: 'CNY',
    serviceFeeAmount: null as number | null, serviceFeeCurrency: 'CNY',
    slots: null as number | null,
    deadline: '' as string | null,
    nonDegreeDuration: null as NonDegreeDuration | null,
    studyDurationMonths: null as number | null,
    applicationChannel: null as ApplicationChannel | null,
    agencyNumber: '',
    requiresFinancialProof: false,
    requiresFoundationYear: false,
    coverage: [] as { kind: CoverageKind, detail: string }[],
    fees: [] as { kind: FeeKind, amount: number | null, currency: string, note: string }[],
    eligibility: blankEligibility(),
    levelStipends: [] as { level: EducationLevel, amount: number | null, currency: string, frequency: StipendFrequency, durationMonths: number | null, conditions: string }[],
    accommodations: [] as { roomType: RoomType, amount: number | null, currency: string, note: string }[],
    intakes: [] as { term: string, applicationOpen: string | null, applicationClose: string | null }[],
    documentRequirements: [] as { docType: string, mandatory: boolean, note: string }[],
    featured: false, recommended: false, hot: false,
    id: undefined as number | undefined,
    heroImageUrl: null as string | null, coverImageUrl: null as string | null,
    heroMediaId: null as number | null, coverMediaId: null as number | null,
    status: 'ACTIVE' as ScholarshipInput['status'],
    publishStatus: 'DRAFT' as ScholarshipInput['publishStatus'],
    remark: '',
  }
}
const form = reactive(blankForm())

const rules = {
  title: [{ required: true, trigger: 'blur', message: t('scholarship.required') }],
  country: [
    { required: true, trigger: 'blur', message: t('scholarship.required') },
    { pattern: /^[A-Za-z]{2}$/, trigger: 'blur', message: t('scholarship.countryFormat') },
  ],
  fundingModel: [{ required: true, message: t('scholarship.required') }],
  status: [{ required: true, message: t('scholarship.required') }],
  publishStatus: [{ required: true, message: t('scholarship.required') }],
}

watch(() => props.modelValue, async (open) => {
  if (!open) return
  Object.assign(form, blankForm())
  let s = props.scholarship
  if (!s) return
  // the list row carries no child collections (fees / stipends / accommodation /
  // coverage / documents / eligibility); fetch the full aggregate so Edit shows
  // and preserves everything.
  try {
    const full = await getScholarship(s.view.id)
    if (full?.view) s = full
  }
  catch { /* fall back to the row we already have */ }
  const v = s.view
  Object.assign(form, {
    title: v.title, summary: v.summary ?? '', country: v.country,
    province: v.province ?? '', city: v.city ?? '', field: v.field ?? '',
    teachingLanguage: v.teachingLanguage ?? null, fundingModel: v.fundingModel,
    categoryCodes: [...v.categories], levels: [...v.levels] as EducationLevel[],
    benefits: v.benefits ?? '', requirements: v.requirements ?? '', policy: v.policy ?? '',
    renewalConditions: v.renewalConditions ?? '',
    nonDegreeDuration: v.nonDegreeDuration ?? null,
    studyDurationMonths: v.studyDurationMonths ?? null,
    applicationChannel: v.applicationChannel ?? null,
    agencyNumber: v.agencyNumber ?? '',
    requiresFinancialProof: v.requiresFinancialProof,
    requiresFoundationYear: v.requiresFoundationYear,
    coverage: v.coverage.map(c => ({ kind: c.kind as CoverageKind, detail: c.detail ?? '' })),
    // amounts are re-populated from the RMB figure (see the Media/Fees note) in CNY
    applicationFeeAmount: v.applicationFee?.amountRmb ?? null, applicationFeeCurrency: 'CNY',
    serviceFeeAmount: v.serviceFee?.amountRmb ?? null, serviceFeeCurrency: 'CNY',
    slots: v.slots ?? null, deadline: v.deadline ?? '',
    fees: v.fees.map(f => ({ kind: f.kind as FeeKind, amount: f.amountRmb, currency: 'CNY', note: f.note ?? '' })),
    eligibility: { ...blankEligibility(), ...(v.eligibility ?? {}), acceptedCountries: v.eligibility?.acceptedCountries ?? '', notes: v.eligibility?.notes ?? '', nationalityScope: (v.eligibility?.nationalityScope as NationalityScope) ?? 'ANY' },
    levelStipends: v.stipends.map(st => ({
      level: st.level as EducationLevel, amount: st.amountRmb, currency: 'CNY',
      frequency: st.frequency, durationMonths: st.durationMonths ?? null, conditions: st.conditions ?? '',
    })),
    accommodations: v.accommodation.map(a => ({
      roomType: a.roomType as RoomType, amount: a.amountRmb ?? null, currency: 'CNY', note: a.note ?? '',
    })),
    intakes: v.intakes.map(i => ({ term: i.term, applicationOpen: i.applicationOpen ?? null, applicationClose: i.applicationClose ?? null })),
    documentRequirements: v.documentRequirements.map(d => ({ docType: d.docType, mandatory: d.mandatory, note: d.note ?? '' })),
    featured: v.featured, recommended: v.recommended, hot: v.hot,
    id: v.id,
    heroImageUrl: v.heroImageUrl ?? null, coverImageUrl: v.coverImageUrl ?? null,
    heroMediaId: v.heroMediaId ?? null, coverMediaId: v.coverMediaId ?? null,
    status: s.status, publishStatus: s.publishStatus, remark: s.remark ?? '',
  })
}, { immediate: true })

const PARTIAL_EXCLUDED = ['CSC', 'CGS', 'TYPE_A', 'TYPE_B', 'TYPE_C', 'TYPE_D']
const allowedCategories = computed(() => {
  if (form.fundingModel === 'SELF') return []
  if (form.fundingModel === 'PARTIAL') return categories.value.filter(c => !PARTIAL_EXCLUDED.includes(c.code))
  return categories.value
})
watch(() => form.fundingModel, () => {
  const allowed = new Set(allowedCategories.value.map(c => c.code))
  form.categoryCodes = form.categoryCodes.filter(c => allowed.has(c))
})

const scopeHint = computed(() => {
  if (form.eligibility.nationalityScope === 'INCLUDE') return t('scholarship.scopeIncludeHint')
  if (form.eligibility.nationalityScope === 'EXCLUDE') return t('scholarship.scopeExcludeHint')
  return t('scholarship.scopeAnyHint')
})

function n(v: unknown): number | null {
  return v === '' || v === null || v === undefined ? null : Number(v)
}
function s(v: string): string | null {
  return v.trim() === '' ? null : v.trim()
}

function payload(): ScholarshipInput {
  const elig = form.eligibility
  const anyElig = elig.ageMin != null || elig.ageMax != null || elig.gpaMin != null || elig.ieltsMin != null
    || elig.toeflMin != null || elig.duolingoMin != null || elig.hskMin != null || elig.cscaMin != null
    || elig.nationalityScope !== 'ANY' || s(elig.notes) != null
  return {
    title: form.title.trim(),
    summary: s(form.summary),
    country: form.country.trim().toUpperCase(),
    province: s(form.province),
    city: s(form.city),
    field: s(form.field),
    teachingLanguage: (form.teachingLanguage as ScholarshipInput['teachingLanguage']) || null,
    fundingModel: form.fundingModel,
    hasStipend: form.levelStipends.some(st => st.amount != null),
    nonDegreeDuration: form.levels.includes('NON_DEGREE') ? form.nonDegreeDuration : null,
    studyDurationMonths: n(form.studyDurationMonths),
    applicationChannel: form.applicationChannel || null,
    agencyNumber: form.applicationChannel === 'CSC_AGENCY' ? s(form.agencyNumber) : null,
    requiresFinancialProof: form.requiresFinancialProof,
    requiresFoundationYear: form.requiresFoundationYear,
    deadline: s(form.deadline ?? ''),
    benefits: s(form.benefits),
    requirements: s(form.requirements),
    policy: s(form.policy),
    renewalConditions: s(form.renewalConditions),
    applicationFeeAmount: n(form.applicationFeeAmount),
    applicationFeeCurrency: form.applicationFeeAmount != null ? 'CNY' : null,
    serviceFeeAmount: n(form.serviceFeeAmount),
    serviceFeeCurrency: form.serviceFeeAmount != null ? 'CNY' : null,
    slots: n(form.slots),
    featured: form.featured, recommended: form.recommended, hot: form.hot,
    heroImageUrl: form.heroImageUrl, coverImageUrl: form.coverImageUrl,
    heroMediaId: form.heroMediaId, coverMediaId: form.coverMediaId,
    status: form.status, publishStatus: form.publishStatus, remark: s(form.remark),
    levels: [...form.levels],
    categoryCodes: [...form.categoryCodes],
    intakes: form.intakes.filter(i => i.term).map(i => ({
      term: i.term, applicationOpen: i.applicationOpen || null, applicationClose: i.applicationClose || null,
    })),
    eligibility: anyElig
      ? {
          ageMin: n(elig.ageMin), ageMax: n(elig.ageMax),
          nationalityScope: elig.nationalityScope,
          acceptedCountries: elig.nationalityScope === 'ANY' ? null : s(elig.acceptedCountries),
          inChina: elig.inChina,
          gpaMin: n(elig.gpaMin), ieltsMin: n(elig.ieltsMin), toeflMin: n(elig.toeflMin),
          duolingoMin: n(elig.duolingoMin), hskMin: n(elig.hskMin), cscaMin: n(elig.cscaMin),
          notes: s(elig.notes),
        }
      : null,
    fees: form.fees.filter(f => f.amount != null && f.kind).map(f => ({
      kind: f.kind, amount: Number(f.amount), currency: 'CNY', note: s(f.note),
    })),
    levelStipends: form.levelStipends.filter(st => st.amount != null && st.level).map(st => ({
      level: st.level, amount: Number(st.amount), currency: 'CNY',
      frequency: st.frequency, durationMonths: n(st.durationMonths), conditions: s(st.conditions),
    })),
    accommodations: form.accommodations.filter(a => a.roomType).map(a => ({
      roomType: a.roomType, amount: n(a.amount), currency: 'CNY', note: s(a.note),
    })),
    coverage: form.coverage.filter(c => c.kind).map(c => ({ kind: c.kind, detail: s(c.detail) })),
    documentRequirements: form.documentRequirements.filter(d => d.docType.trim()).map(d => ({
      docType: d.docType.trim().toUpperCase(), mandatory: d.mandatory, note: s(d.note),
    })),
  }
}

async function save() {
  await formRef.value?.validate()
  if (!form.levels.length) {
    ElMessage.warning(t('scholarship.needLevel'))
    return
  }
  saving.value = true
  try {
    const saved = props.scholarship
      ? await updateScholarship(props.scholarship.view.id, payload())
      : await createScholarship(payload())
    if (!props.scholarship) {
      await Promise.all([heroUp.value?.flush(saved.view.id), coverUp.value?.flush(saved.view.id)])
    }
    ElMessage.success(t('common.saved'))
    emit('update:modelValue', false)
    emit('saved', saved)
  }
  finally {
    saving.value = false
  }
}
</script>

<template>
  <Drawer
    :model-value="modelValue"
    :title="scholarship ? t('scholarship.edit') : t('scholarship.new')"
    :saving="saving"
    :size="640"
    @update:model-value="v => emit('update:modelValue', v)"
    @save="save"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <FormSection :title="t('scholarship.secIdentity')">
        <el-form-item
          :label="t('scholarship.title')"
          prop="title"
        >
          <el-input
            v-model="form.title"
            maxlength="200"
          />
        </el-form-item>
        <el-form-item :label="t('scholarship.summary')">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="2"
            maxlength="2000"
          />
        </el-form-item>
        <div class="row">
          <el-form-item
            :label="t('scholarship.country')"
            prop="country"
            class="w-28"
          >
            <el-input
              v-model="form.country"
              maxlength="2"
              style="text-transform:uppercase"
            />
          </el-form-item>
          <el-form-item :label="t('scholarship.province')">
            <el-input
              v-model="form.province"
              maxlength="120"
            />
          </el-form-item>
          <el-form-item :label="t('scholarship.city')">
            <el-input
              v-model="form.city"
              maxlength="120"
            />
          </el-form-item>
        </div>
        <el-form-item :label="t('scholarship.field')">
          <el-input
            v-model="form.field"
            maxlength="120"
            :placeholder="t('scholarship.fieldHint')"
          />
          <p class="hint">
            {{ t('scholarship.fieldNote') }}
          </p>
        </el-form-item>
      </FormSection>

      <FormSection :title="t('scholarship.secClassification')">
        <div class="row">
          <el-form-item
            :label="t('scholarship.fundingModel')"
            prop="fundingModel"
          >
            <el-select v-model="form.fundingModel">
              <el-option
                v-for="f in FUNDING"
                :key="f"
                :value="f"
                :label="t(`scholarship.funding.${f}`)"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('scholarship.teachingLanguage')">
            <el-select
              v-model="form.teachingLanguage"
              clearable
            >
              <el-option
                v-for="l in LANGS"
                :key="l"
                :value="l"
                :label="t(`scholarship.lang.${l}`)"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item
          v-if="form.fundingModel !== 'SELF'"
          :label="t('scholarship.categories')"
        >
          <el-select
            v-model="form.categoryCodes"
            multiple
            filterable
            :placeholder="t('scholarship.categoriesHint')"
          >
            <el-option
              v-for="c in allowedCategories"
              :key="c.code"
              :value="c.code"
              :label="c.name"
            />
          </el-select>
          <p
            v-if="form.fundingModel === 'PARTIAL'"
            class="hint"
          >
            {{ t('scholarship.categoriesPartialHint') }}
          </p>
        </el-form-item>
        <el-form-item
          :label="t('scholarship.levels')"
          :required="true"
        >
          <el-checkbox-group v-model="form.levels">
            <el-checkbox
              v-for="lv in EDUCATION_LEVELS"
              :key="lv"
              :value="lv"
              :label="t(`scholarship.level.${lv}`)"
            />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item
          v-if="form.levels.includes('NON_DEGREE')"
          :label="t('scholarship.nonDegreeDuration')"
        >
          <el-select
            v-model="form.nonDegreeDuration"
            clearable
            style="width: 220px"
          >
            <el-option
              v-for="d in NON_DEGREE_DURATIONS"
              :key="d"
              :value="d"
              :label="t(`scholarship.nonDegree.${d}`)"
            />
          </el-select>
        </el-form-item>
      </FormSection>

      <FormSection
        :title="t('scholarship.secIntakes')"
        :description="t('scholarship.intakesHint')"
      >
        <div
          v-for="(it, i) in form.intakes"
          :key="i"
          class="line"
        >
          <el-select
            v-model="it.term"
            class="w-44"
          >
            <el-option
              v-for="term in INTAKE_TERMS"
              :key="term"
              :value="term"
              :label="t(`scholarship.intake.${term}`)"
            />
          </el-select>
          <el-date-picker
            v-model="it.applicationOpen"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="t('scholarship.opens')"
          />
          <el-date-picker
            v-model="it.applicationClose"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="t('scholarship.closes')"
          />
          <el-button
            link
            type="danger"
            :icon="Delete"
            @click="form.intakes.splice(i, 1)"
          />
        </div>
        <el-button
          :icon="Plus"
          @click="form.intakes.push({ term: 'AUTUMN_SEPTEMBER', applicationOpen: null, applicationClose: null })"
        >
          {{ t('scholarship.addIntake') }}
        </el-button>
      </FormSection>

      <FormSection :title="t('scholarship.secEligibility')">
        <div class="row">
          <el-form-item
            :label="t('scholarship.ageMin')"
            class="w-28"
          >
            <el-input-number
              v-model="form.eligibility.ageMin"
              :min="0"
              :max="120"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item
            :label="t('scholarship.ageMax')"
            class="w-28"
          >
            <el-input-number
              v-model="form.eligibility.ageMax"
              :min="0"
              :max="120"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item :label="t('scholarship.inChina')">
            <el-select
              v-model="form.eligibility.inChina"
              clearable
              :placeholder="t('scholarship.inChinaEither')"
            >
              <el-option
                :value="true"
                :label="t('scholarship.inChinaYes')"
              />
              <el-option
                :value="false"
                :label="t('scholarship.inChinaNo')"
              />
            </el-select>
          </el-form-item>
        </div>
        <p class="hint">
          {{ t('scholarship.inChinaHint') }}
        </p>
        <el-form-item :label="t('scholarship.nationality')">
          <el-radio-group v-model="form.eligibility.nationalityScope">
            <el-radio
              v-for="sc in SCOPES"
              :key="sc"
              :value="sc"
            >
              {{ t(`scholarship.scope.${sc}`) }}
            </el-radio>
          </el-radio-group>
          <p class="hint">
            {{ scopeHint }}
          </p>
        </el-form-item>
        <el-form-item
          v-if="form.eligibility.nationalityScope !== 'ANY'"
          :label="t('scholarship.countryList')"
        >
          <el-input
            v-model="form.eligibility.acceptedCountries"
            :placeholder="t('scholarship.countryListHint')"
          />
        </el-form-item>
        <div class="row">
          <el-form-item
            label="GPA min"
            class="w-28"
          >
            <el-input-number
              v-model="form.eligibility.gpaMin"
              :min="0"
              :max="5"
              :step="0.1"
              :precision="2"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item
            label="IELTS min"
            class="w-28"
          >
            <el-input-number
              v-model="form.eligibility.ieltsMin"
              :min="0"
              :max="9"
              :step="0.5"
              :precision="1"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item
            label="TOEFL min"
            class="w-28"
          >
            <el-input-number
              v-model="form.eligibility.toeflMin"
              :min="0"
              :max="120"
              controls-position="right"
            />
          </el-form-item>
        </div>
        <div class="row">
          <el-form-item
            label="Duolingo min"
            class="w-28"
          >
            <el-input-number
              v-model="form.eligibility.duolingoMin"
              :min="0"
              :max="160"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item
            label="HSK min"
            class="w-28"
          >
            <el-input-number
              v-model="form.eligibility.hskMin"
              :min="0"
              :max="9"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item
            label="CSCA min"
            class="w-28"
          >
            <el-input-number
              v-model="form.eligibility.cscaMin"
              :min="0"
              controls-position="right"
            />
          </el-form-item>
        </div>
        <el-form-item :label="t('scholarship.eligNotes')">
          <el-input
            v-model="form.eligibility.notes"
            type="textarea"
            :rows="2"
            maxlength="2000"
          />
        </el-form-item>
      </FormSection>

      <FormSection
        :title="t('scholarship.secFees')"
        :description="t('scholarship.feesHint')"
      >
        <div class="row">
          <el-form-item :label="t('scholarship.applicationFeeRmb')">
            <el-input-number
              v-model="form.applicationFeeAmount"
              :min="0"
              :precision="2"
              :step="100"
              controls-position="right"
              style="width: 100%"
            />
            <span class="usd">{{ usdHint(form.applicationFeeAmount) }}</span>
          </el-form-item>
          <el-form-item :label="t('scholarship.serviceFeeRmb')">
            <el-input-number
              v-model="form.serviceFeeAmount"
              :min="0"
              :precision="2"
              :step="100"
              controls-position="right"
              style="width: 100%"
            />
            <span class="usd">{{ usdHint(form.serviceFeeAmount) }}</span>
          </el-form-item>
        </div>
        <p class="hint">
          {{ t('scholarship.feeLinesHint') }}
        </p>
        <div
          v-for="(f, i) in form.fees"
          :key="i"
          class="line"
        >
          <el-select
            v-model="f.kind"
            class="w-56"
            filterable
          >
            <el-option
              v-for="k in FEE_KINDS"
              :key="k"
              :value="k"
              :label="t(`scholarship.fee.${k}`)"
            />
          </el-select>
          <el-input-number
            v-model="f.amount"
            :min="0"
            :precision="2"
            :step="1000"
            controls-position="right"
          />
          <span class="usd">{{ usdHint(f.amount) }}</span>
          <el-input
            v-model="f.note"
            :placeholder="t('scholarship.note')"
          />
          <el-button
            link
            type="danger"
            :icon="Delete"
            @click="form.fees.splice(i, 1)"
          />
        </div>
        <el-button
          :icon="Plus"
          @click="form.fees.push({ kind: 'TUITION_AFTER', amount: null, currency: 'CNY', note: '' })"
        >
          {{ t('scholarship.addFee') }}
        </el-button>
      </FormSection>

      <FormSection
        :title="t('scholarship.secStipend')"
        :description="t('scholarship.stipendPerLevelHint')"
      >
        <div
          v-for="(st, i) in form.levelStipends"
          :key="i"
          class="line"
        >
          <el-select
            v-model="st.level"
            class="w-44"
          >
            <el-option
              v-for="lv in form.levels.length ? form.levels : EDUCATION_LEVELS"
              :key="lv"
              :value="lv"
              :label="t(`scholarship.level.${lv}`)"
            />
          </el-select>
          <el-input-number
            v-model="st.amount"
            :min="0"
            :precision="2"
            :step="1000"
            controls-position="right"
            :placeholder="t('scholarship.amountRmb')"
          />
          <span class="usd">{{ usdHint(st.amount) }}</span>
          <el-select
            v-model="st.frequency"
            class="w-44"
          >
            <el-option
              v-for="fr in FREQ"
              :key="fr"
              :value="fr"
              :label="t(`scholarship.freq.${fr}`)"
            />
          </el-select>
          <el-input-number
            v-model="st.durationMonths"
            :min="0"
            controls-position="right"
            :placeholder="t('scholarship.durationMonths')"
            class="w-28"
          />
          <el-input
            v-model="st.conditions"
            :placeholder="t('scholarship.stipendConditions')"
          />
          <el-button
            link
            type="danger"
            :icon="Delete"
            @click="form.levelStipends.splice(i, 1)"
          />
        </div>
        <el-button
          :icon="Plus"
          @click="form.levelStipends.push({ level: (form.levels[0] ?? 'MASTER'), amount: null, currency: 'CNY', frequency: 'MONTHLY', durationMonths: null, conditions: '' })"
        >
          {{ t('scholarship.addStipend') }}
        </el-button>
      </FormSection>

      <FormSection
        :title="t('scholarship.secAccommodation')"
        :description="t('scholarship.accommodationHint')"
      >
        <div
          v-for="(a, i) in form.accommodations"
          :key="i"
          class="line"
        >
          <el-select
            v-model="a.roomType"
            class="w-44"
          >
            <el-option
              v-for="rt in ROOM_TYPES"
              :key="rt"
              :value="rt"
              :label="t(`scholarship.room.${rt}`)"
            />
          </el-select>
          <el-input-number
            v-model="a.amount"
            :min="0"
            :precision="2"
            :step="500"
            controls-position="right"
            :placeholder="t('scholarship.amountRmb')"
          />
          <span class="usd">{{ usdHint(a.amount) }}</span>
          <el-input
            v-model="a.note"
            :placeholder="t('scholarship.accommodationNote')"
            maxlength="200"
          />
          <el-button
            link
            type="danger"
            :icon="Delete"
            @click="form.accommodations.splice(i, 1)"
          />
        </div>
        <el-button
          :icon="Plus"
          @click="form.accommodations.push({ roomType: 'SINGLE', amount: null, currency: 'CNY', note: '' })"
        >
          {{ t('scholarship.addAccommodation') }}
        </el-button>
      </FormSection>

      <FormSection
        :title="t('scholarship.secCoverage')"
        :description="t('scholarship.coverageHint')"
      >
        <div
          v-for="(c, i) in form.coverage"
          :key="i"
          class="line"
        >
          <el-select
            v-model="c.kind"
            class="w-52"
          >
            <el-option
              v-for="ck in COVERAGE_KINDS"
              :key="ck"
              :value="ck"
              :label="t(`scholarship.coverageKind.${ck}`)"
            />
          </el-select>
          <el-input
            v-model="c.detail"
            :placeholder="t('scholarship.coverageDetail')"
            maxlength="300"
          />
          <el-button
            link
            type="danger"
            :icon="Delete"
            @click="form.coverage.splice(i, 1)"
          />
        </div>
        <el-button
          :icon="Plus"
          @click="form.coverage.push({ kind: 'TUITION', detail: '' })"
        >
          {{ t('scholarship.addCoverage') }}
        </el-button>
      </FormSection>

      <FormSection :title="t('scholarship.secApplication')">
        <div class="row">
          <el-form-item :label="t('scholarship.studyDurationMonths')">
            <el-input-number
              v-model="form.studyDurationMonths"
              :min="1"
              :max="120"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item :label="t('scholarship.applicationChannel')">
            <el-select
              v-model="form.applicationChannel"
              clearable
            >
              <el-option
                v-for="ch in APPLICATION_CHANNELS"
                :key="ch"
                :value="ch"
                :label="t(`scholarship.channel.${ch}`)"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            v-if="form.applicationChannel === 'CSC_AGENCY'"
            :label="t('scholarship.agencyNumber')"
          >
            <el-input
              v-model="form.agencyNumber"
              maxlength="64"
            />
          </el-form-item>
        </div>
        <el-form-item>
          <el-checkbox v-model="form.requiresFinancialProof">
            {{ t('scholarship.requiresFinancialProof') }}
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.requiresFoundationYear">
            {{ t('scholarship.requiresFoundationYear') }}
          </el-checkbox>
        </el-form-item>
      </FormSection>

      <FormSection
        :title="t('scholarship.secDocuments')"
        :description="t('scholarship.documentsHint')"
      >
        <div
          v-for="(d, i) in form.documentRequirements"
          :key="i"
          class="line"
        >
          <el-select
            v-model="d.docType"
            class="w-52"
            filterable
            allow-create
            default-first-option
          >
            <el-option
              v-for="dt in DOC_TYPES"
              :key="dt"
              :value="dt"
              :label="t(`scholarship.doc.${dt}`, dt)"
            />
          </el-select>
          <el-checkbox v-model="d.mandatory">
            {{ t('scholarship.mandatory') }}
          </el-checkbox>
          <el-input
            v-model="d.note"
            :placeholder="t('scholarship.note')"
          />
          <el-button
            link
            type="danger"
            :icon="Delete"
            @click="form.documentRequirements.splice(i, 1)"
          />
        </div>
        <el-button
          :icon="Plus"
          @click="form.documentRequirements.push({ docType: 'PASSPORT', mandatory: true, note: '' })"
        >
          {{ t('scholarship.addDocument') }}
        </el-button>
      </FormSection>

      <FormSection :title="t('scholarship.secContent')">
        <el-form-item :label="t('scholarship.benefits')">
          <el-input
            v-model="form.benefits"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item :label="t('scholarship.requirements')">
          <el-input
            v-model="form.requirements"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item :label="t('scholarship.policy')">
          <el-input
            v-model="form.policy"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
        <el-form-item :label="t('scholarship.renewalConditions')">
          <el-input
            v-model="form.renewalConditions"
            type="textarea"
            :rows="2"
            maxlength="2000"
            :placeholder="t('scholarship.renewalConditionsHint')"
          />
        </el-form-item>
      </FormSection>

      <FormSection :title="t('scholarship.secMedia')">
        <div class="imgs">
          <el-form-item :label="t('scholarship.heroImage')">
            <ImageUpload
              ref="heroUp"
              v-model="form.heroMediaId"
              :action="`/api/staff/scholarships/${form.id}/hero`"
              :resolve-action="(id) => `/api/staff/scholarships/${id}/hero`"
              :deferred="!form.id"
              :preview-url="scholarship?.view.heroUrl ?? scholarship?.view.heroImageUrl"
              aspect="wide"
            />
          </el-form-item>
          <el-form-item :label="t('scholarship.coverImage')">
            <ImageUpload
              ref="coverUp"
              v-model="form.coverMediaId"
              :action="`/api/staff/scholarships/${form.id}/cover`"
              :resolve-action="(id) => `/api/staff/scholarships/${id}/cover`"
              :deferred="!form.id"
              :preview-url="scholarship?.view.coverUrl ?? scholarship?.view.coverImageUrl"
              aspect="wide"
            />
          </el-form-item>
        </div>
      </FormSection>

      <FormSection :title="t('scholarship.secPublication')">
        <div class="row">
          <el-form-item :label="t('scholarship.deadline')">
            <el-date-picker
              v-model="form.deadline"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item
            :label="t('scholarship.slots')"
            class="w-28"
          >
            <el-input-number
              v-model="form.slots"
              :min="0"
              controls-position="right"
            />
          </el-form-item>
        </div>
        <el-form-item>
          <el-checkbox v-model="form.featured">
            {{ t('scholarship.featured') }}
          </el-checkbox>
          <el-checkbox v-model="form.recommended">
            {{ t('scholarship.recommended') }}
          </el-checkbox>
          <el-checkbox v-model="form.hot">
            {{ t('scholarship.hot') }}
          </el-checkbox>
        </el-form-item>
        <div class="row">
          <el-form-item
            :label="t('scholarship.status')"
            prop="status"
          >
            <el-select v-model="form.status">
              <el-option
                v-for="st in STATUSES"
                :key="st"
                :value="st"
                :label="st"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            :label="t('scholarship.publishStatus')"
            prop="publishStatus"
          >
            <el-select v-model="form.publishStatus">
              <el-option
                v-for="p in PUBLISH"
                :key="p"
                :value="p"
                :label="p"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item :label="t('scholarship.remark')">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            maxlength="500"
          />
        </el-form-item>
      </FormSection>
    </el-form>
  </Drawer>
</template>

<style scoped>
.row { display: flex; gap: 12px; flex-wrap: wrap; }
.row > * { flex: 1; min-width: 140px; }
.row > .w-28 { flex: 0 0 7rem; min-width: 7rem; }
.row > .w-24 { flex: 0 0 6rem; min-width: 6rem; }
.imgs { display: flex; gap: 24px; flex-wrap: wrap; }
.line { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 10px; }
.usd { font-size: 12px; color: var(--nad-ink-soft); font-variant-numeric: tabular-nums; min-width: 4rem; }
.line > .w-20 { flex: 0 0 5rem; }
.line > .w-44 { flex: 0 0 11rem; }
.line > .w-52 { flex: 0 0 13rem; }
.line > .w-56 { flex: 0 0 14rem; }
.hint { margin: 2px 0 8px; font-size: 12px; color: var(--nad-ink-soft); }
</style>
