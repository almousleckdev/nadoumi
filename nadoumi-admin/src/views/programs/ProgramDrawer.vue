<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import {
  createProgram, updateProgram,
  PROGRAM_TYPES, PROGRAM_LANGUAGES, INTAKE_TERMS, TERM_LENGTHS, DEGREE_LEVELS,
  isDegree as isDegreeKind,
  type Program, type ProgramInput, type TermLength, type DegreeLevel,
} from '@/api/program'
import { cnyToUsdRate } from '@/api/fx'
import { usd } from '@/utils/money'
import { listUniversities, listDepartments, type University, type Department } from '@/api/university'
import Drawer from '@/components/ui/Drawer.vue'
import FormSection from '@/components/ui/FormSection.vue'
import ImageUpload from '@/components/ui/ImageUpload.vue'

const props = defineProps<{
  modelValue: boolean
  program: Program | null
  lockedUniversity?: { id: number, name: string } | null
}>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [p: Program] }>()

const { t } = useI18n()
const STATUSES = ['ACTIVE', 'INACTIVE'] as const
const PUBLISH = ['DRAFT', 'PUBLISHED'] as const

const formRef = ref<FormInstance>()
const saving = ref(false)
// create mode holds the image until the program row exists
const imageUp = ref<InstanceType<typeof ImageUpload>>()
const universities = ref<{ id: number, name: string }[]>([])

// live "≈ $" preview for the RMB tuition; saved rows carry the server value
const fxRate = ref(0.1381)
onMounted(async () => { fxRate.value = await cnyToUsdRate() })

function loadUniversities() {
  if (props.lockedUniversity || universities.value.length) return
  listUniversities({ page: 0, size: 200 })
    .then(res => universities.value = res.content.map((u: University) => ({ id: u.id, name: u.name })))
    .catch(() => {})
}

// academic departments of the selected university, for the majors picker
const departments = ref<Department[]>([])
async function loadDepartments(universityId: number | null) {
  if (!universityId) { departments.value = []; return }
  try {
    departments.value = await listDepartments(universityId)
  }
  catch { departments.value = [] }
}

function blankForm() {
  return {
    id: undefined as number | undefined,
    universityId: props.lockedUniversity?.id ?? (null as number | null),
    name: '', nameCn: '',
    programType: 'DEGREE' as ProgramInput['programType'],
    levels: [] as DegreeLevel[],
    field: '',
    termLength: null as TermLength | null,
    teachingLanguage: null as ProgramInput['teachingLanguage'],
    durationMonths: null as number | null,
    tuitionAmount: null as number | null,
    tuitionCurrency: 'CNY',
    summary: '',
    imageMediaId: null as number | null,
    majors: [] as { name: string, nameCn: string | null, departmentId: number | null, level: DegreeLevel | null }[],
    intakes: [] as { term: string, applicationOpen: string | null, applicationClose: string | null }[],
    featured: false, hot: false,
    status: 'ACTIVE' as ProgramInput['status'],
    publishStatus: 'DRAFT' as ProgramInput['publishStatus'],
    remark: '',
  }
}
const form = reactive(blankForm())
const tuitionUsdPreview = computed(() =>
  form.tuitionAmount == null ? '' : `≈ ${usd(form.tuitionAmount * fxRate.value)}`)
const isDegree = computed(() => isDegreeKind(form.programType))

// user picked a different university — its departments no longer apply
async function onUniversityChange() {
  form.majors.forEach(m => { m.departmentId = null })
  await loadDepartments(form.universityId)
}

// dropping a level detaches any major that was assigned to it
watch(() => [...form.levels], (levels) => {
  form.majors.forEach(m => {
    if (m.level && !levels.includes(m.level)) m.level = null
  })
})

const rules = {
  universityId: [{ required: true, message: t('program.required') }],
  name: [{ required: true, trigger: 'blur', message: t('program.required') }],
  programType: [{ required: true, message: t('program.required') }],
  status: [{ required: true, message: t('program.required') }],
  publishStatus: [{ required: true, message: t('program.required') }],
}

watch(() => props.modelValue, (open) => {
  if (!open) return
  loadUniversities()
  Object.assign(form, blankForm())
  const p = props.program
  if (!p) {
    loadDepartments(form.universityId)
    return
  }
  Object.assign(form, {
    id: p.id,
    universityId: p.universityId,
    name: p.name, nameCn: p.nameCn ?? '',
    programType: p.programType,
    levels: [...(p.levels ?? [])],
    field: p.field ?? '',
    termLength: p.termLength ?? null,
    teachingLanguage: p.teachingLanguage ?? null,
    durationMonths: p.durationMonths ?? null,
    tuitionAmount: p.tuitionAmount ?? null,
    tuitionCurrency: p.tuitionCurrency ?? 'CNY',
    summary: p.summary ?? '',
    imageMediaId: p.imageMediaId ?? null,
    majors: p.majors.map(m => ({
      name: m.name, nameCn: m.nameCn ?? null, departmentId: m.departmentId ?? null, level: m.level ?? null,
    })),
    intakes: p.intakes.map(i => ({
      term: i.term, applicationOpen: i.applicationOpen ?? null, applicationClose: i.applicationClose ?? null,
    })),
    featured: p.featured, hot: p.hot,
    status: p.status, publishStatus: p.publishStatus, remark: p.remark ?? '',
  })
  loadDepartments(p.universityId)
}, { immediate: true })

function n(v: unknown): number | null {
  return v === '' || v === null || v === undefined ? null : Number(v)
}
function s(v: string): string | null {
  return v.trim() === '' ? null : v.trim()
}

function payload(): ProgramInput {
  return {
    universityId: form.universityId as number,
    name: form.name.trim(),
    nameCn: s(form.nameCn),
    programType: form.programType,
    levels: isDegree.value ? [...form.levels] : [],
    field: s(form.field),
    termLength: isDegree.value ? null : (form.termLength || null),
    teachingLanguage: form.teachingLanguage || null,
    durationMonths: n(form.durationMonths),
    tuitionAmount: n(form.tuitionAmount),
    tuitionCurrency: form.tuitionAmount != null ? 'CNY' : null,
    summary: s(form.summary),
    imageMediaId: form.imageMediaId,
    featured: form.featured, hot: form.hot,
    status: form.status, publishStatus: form.publishStatus, remark: s(form.remark),
    majors: isDegree.value
      ? form.majors.filter(m => m.name.trim()).map(m => ({
          name: m.name.trim(), nameCn: s(m.nameCn ?? ''), departmentId: m.departmentId ?? null,
          level: m.level ?? null,
        }))
      : [],
    intakes: form.intakes.filter(i => i.term).map(i => ({
      term: i.term, applicationOpen: i.applicationOpen || null, applicationClose: i.applicationClose || null,
    })),
  }
}

async function save() {
  await formRef.value?.validate()
  if (isDegree.value && !form.levels.length) {
    ElMessage.warning(t('program.levelsRequired'))
    return
  }
  saving.value = true
  try {
    const saved = props.program
      ? await updateProgram(props.program.id, payload())
      : await createProgram(payload())
    if (!props.program) await imageUp.value?.flush(saved.id)
    ElMessage.success(t('common.saved'))
    emit('update:modelValue', false)
    emit('saved', saved)
  }
  finally {
    saving.value = false
  }
}

defineExpose({ form, save, rules })
</script>

<template>
  <Drawer
    :model-value="modelValue"
    :title="program ? t('program.edit') : t('program.new')"
    :saving="saving"
    :size="600"
    @update:model-value="v => emit('update:modelValue', v)"
    @save="save"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <FormSection :title="t('program.secIdentity')">
        <el-form-item
          :label="t('program.university')"
          prop="universityId"
        >
          <el-input
            v-if="lockedUniversity"
            :model-value="lockedUniversity.name"
            disabled
          />
          <el-select
            v-else
            v-model="form.universityId"
            filterable
            :placeholder="t('program.universityPlaceholder')"
            style="width: 100%"
            @change="onUniversityChange"
          >
            <el-option
              v-for="u in universities"
              :key="u.id"
              :label="u.name"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <div class="row">
          <el-form-item
            :label="t('program.name')"
            prop="name"
          >
            <el-input
              v-model="form.name"
              maxlength="200"
            />
          </el-form-item>
          <el-form-item :label="t('program.nameCn')">
            <el-input
              v-model="form.nameCn"
              maxlength="200"
            />
          </el-form-item>
        </div>
        <el-form-item :label="t('program.summary')">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="2"
            maxlength="4000"
          />
        </el-form-item>
      </FormSection>

      <FormSection :title="t('program.secClassification')">
        <div class="row">
          <el-form-item
            :label="t('program.type')"
            prop="programType"
          >
            <el-select
              v-model="form.programType"
              style="width: 100%"
            >
              <el-option
                v-for="pt in PROGRAM_TYPES"
                :key="pt"
                :value="pt"
                :label="t(`program.typeMap.${pt}`)"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('program.language')">
            <el-select
              v-model="form.teachingLanguage"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="l in PROGRAM_LANGUAGES"
                :key="l"
                :value="l"
                :label="t(`program.langMap.${l}`)"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item
          v-if="isDegree"
          :label="t('program.levels')"
        >
          <el-checkbox-group v-model="form.levels">
            <el-checkbox
              v-for="lv in DEGREE_LEVELS"
              :key="lv"
              :value="lv"
              :label="t(`program.levelMap.${lv}`)"
            />
          </el-checkbox-group>
          <p class="fx-note">
            {{ t('program.levelsHint') }}
          </p>
        </el-form-item>
        <div class="row">
          <el-form-item :label="t('program.durationMonths')">
            <el-input-number
              v-model="form.durationMonths"
              :min="1"
              :max="120"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item
            v-if="!isDegree"
            :label="t('program.termLength')"
          >
            <el-select
              v-model="form.termLength"
              clearable
              style="width: 100%"
              :placeholder="t('program.termLengthPlaceholder')"
            >
              <el-option
                v-for="tl in TERM_LENGTHS"
                :key="tl"
                :value="tl"
                :label="t(`program.termLengthMap.${tl}`)"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="row">
          <el-form-item
            :label="t('program.tuitionAmountRmb')"
            class="w-40"
          >
            <el-input-number
              v-model="form.tuitionAmount"
              :min="0"
              :precision="2"
              :step="1000"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item :label="t('program.tuitionUsd')">
            <span class="tuition-usd">{{ tuitionUsdPreview }}</span>
          </el-form-item>
        </div>
        <p class="fx-note">
          {{ t('program.tuitionFxNote') }}
        </p>
      </FormSection>

      <FormSection
        v-if="isDegree"
        :title="t('program.secMajors')"
        :description="t('program.majorsHint')"
      >
        <p
          v-if="!departments.length"
          class="fx-note"
        >
          {{ form.universityId ? t('program.noDepartments') : t('program.pickUniversityFirst') }}
        </p>
        <div
          v-for="(m, i) in form.majors"
          :key="i"
          class="line line--major"
        >
          <el-select
            v-model="m.level"
            :placeholder="t('program.majorLevel')"
            clearable
            class="w-32"
          >
            <el-option
              v-for="lv in form.levels"
              :key="lv"
              :value="lv"
              :label="t(`program.levelMap.${lv}`)"
            />
          </el-select>
          <el-select
            v-model="m.departmentId"
            :placeholder="t('program.majorDepartment')"
            clearable
            filterable
            class="w-44"
          >
            <el-option
              v-for="d in departments"
              :key="d.id"
              :value="d.id"
              :label="d.name"
            />
          </el-select>
          <el-input
            v-model="m.name"
            :placeholder="t('program.majorName')"
            maxlength="200"
          />
          <el-input
            v-model="m.nameCn"
            :placeholder="t('program.majorNameCn')"
            maxlength="200"
          />
          <el-button
            link
            type="danger"
            :icon="Delete"
            @click="form.majors.splice(i, 1)"
          />
        </div>
        <el-button
          :icon="Plus"
          @click="form.majors.push({ name: '', nameCn: null, departmentId: null, level: form.levels[0] ?? null })"
        >
          {{ t('program.addMajor') }}
        </el-button>
      </FormSection>

      <FormSection
        :title="t('program.secIntakes')"
        :description="t('program.intakesHint')"
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
              :label="t(`program.intake.${term}`)"
            />
          </el-select>
          <el-date-picker
            v-model="it.applicationOpen"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="t('program.opens')"
          />
          <el-date-picker
            v-model="it.applicationClose"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="t('program.closes')"
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
          {{ t('program.addIntake') }}
        </el-button>
      </FormSection>

      <FormSection :title="t('program.secImage')">
        <el-form-item :label="t('program.image')">
          <ImageUpload
            ref="imageUp"
            v-model="form.imageMediaId"
            :action="`/api/staff/programs/${form.id}/image`"
            :resolve-action="(id) => `/api/staff/programs/${id}/image`"
            :deferred="!form.id"
            :preview-url="program?.imageUrl"
            aspect="wide"
          />
        </el-form-item>
      </FormSection>

      <FormSection :title="t('program.secPublication')">
        <div class="row">
          <el-form-item :label="t('program.featured')">
            <el-switch v-model="form.featured" />
          </el-form-item>
          <el-form-item :label="t('program.hot')">
            <el-switch v-model="form.hot" />
          </el-form-item>
        </div>
        <div class="row">
          <el-form-item
            :label="t('program.status')"
            prop="status"
          >
            <el-select
              v-model="form.status"
              style="width: 100%"
            >
              <el-option
                v-for="st in STATUSES"
                :key="st"
                :value="st"
                :label="t(`program.statusMap.${st}`)"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            :label="t('program.publishStatus')"
            prop="publishStatus"
          >
            <el-select
              v-model="form.publishStatus"
              style="width: 100%"
            >
              <el-option
                v-for="p in PUBLISH"
                :key="p"
                :value="p"
                :label="t(`program.publishMap.${p}`)"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item :label="t('program.remark')">
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
.row > .w-40 { flex: 0 0 10rem; min-width: 10rem; }
.line { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.line > .w-44 { flex: 0 0 11rem; }
.line > .w-32 { flex: 0 0 8rem; }
.tuition-usd { color: var(--nad-ink-soft, #64748b); font-variant-numeric: tabular-nums; }
.fx-note { margin: 4px 0 0; font-size: 12px; color: var(--nad-ink-faint, #9ca3af); }
</style>
