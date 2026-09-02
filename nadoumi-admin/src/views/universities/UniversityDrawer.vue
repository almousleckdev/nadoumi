<template>
  <Drawer
    :model-value="modelValue"
    :title="university ? t('university.edit') : t('university.new')"
    :saving="saving"
    :size="580"
    @update:model-value="v => emit('update:modelValue', v)"
    @save="save"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <FormSection :title="t('university.secIdentity')">
        <el-form-item
          :label="t('university.name')"
          prop="name"
        >
          <el-input v-model="form.name" />
        </el-form-item>
        <div class="row2">
          <el-form-item :label="t('university.nameCn')">
            <el-input v-model="form.nameCn" />
          </el-form-item>
          <el-form-item :label="t('university.type')">
            <el-select
              v-model="form.type"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="ty in TYPES"
                :key="ty"
                :label="titleCase(ty)"
                :value="ty"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="row2">
          <el-form-item
            :label="t('university.country')"
            prop="country"
          >
            <el-input
              v-model="form.country"
              maxlength="2"
              placeholder="ISO alpha-2 (CN)"
            />
          </el-form-item>
          <el-form-item :label="t('university.foundedYear')">
            <el-input-number
              v-model="form.foundedYear"
              :min="800"
              :max="thisYear"
              :controls="false"
              style="width: 100%"
            />
          </el-form-item>
        </div>
        <div class="row2">
          <el-form-item :label="t('university.city')">
            <el-input v-model="form.city" />
          </el-form-item>
          <el-form-item :label="t('university.province')">
            <el-input v-model="form.province" />
          </el-form-item>
        </div>
      </FormSection>

      <FormSection :title="t('university.secProfile')">
        <div class="row3">
          <el-form-item :label="t('university.totalStudents')">
            <el-input-number
              v-model="form.totalStudents"
              :min="0"
              :controls="false"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item :label="t('university.intlStudents')">
            <el-input-number
              v-model="form.internationalStudents"
              :min="0"
              :controls="false"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item :label="t('university.facultyCount')">
            <el-input-number
              v-model="form.facultyCount"
              :min="0"
              :controls="false"
              style="width: 100%"
            />
          </el-form-item>
        </div>
        <div class="row2">
          <el-form-item :label="t('university.website')">
            <el-input
              v-model="form.website"
              placeholder="https://…"
            />
          </el-form-item>
          <el-form-item :label="t('university.rankingTier')">
            <el-input
              v-model="form.rankingTier"
              placeholder="e.g. Top 100"
            />
          </el-form-item>
        </div>
      </FormSection>

      <FormSection :title="t('university.secContent')">
        <el-form-item :label="t('university.introduction')">
          <el-input
            v-model="form.introduction"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item :label="t('university.history')">
          <el-input
            v-model="form.history"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
        <el-form-item :label="t('university.campusInfo')">
          <el-input
            v-model="form.campusInfo"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
        <div class="row2">
          <el-form-item :label="t('university.accommodationInfo')">
            <el-input
              v-model="form.accommodationInfo"
              type="textarea"
              :rows="2"
            />
          </el-form-item>
          <el-form-item :label="t('university.nearbyInfo')">
            <el-input
              v-model="form.nearbyInfo"
              type="textarea"
              :rows="2"
            />
          </el-form-item>
        </div>
      </FormSection>

      <FormSection
        :title="t('university.secHighlights')"
        :description="t('university.highlightsHint')"
      >
        <div
          v-for="(h, i) in form.highlights"
          :key="i"
          class="repeat"
        >
          <el-select
            v-model="h.kind"
            style="width: 130px"
          >
            <el-option
              v-for="k in KINDS"
              :key="k"
              :label="titleCase(k)"
              :value="k"
            />
          </el-select>
          <el-input
            v-model="h.text"
            :placeholder="t('university.highlightText')"
          />
          <el-button
            :icon="Delete"
            text
            @click="form.highlights.splice(i, 1)"
          />
        </div>
        <el-button
          size="small"
          :icon="Plus"
          @click="form.highlights.push({ kind: 'HIGHLIGHT', text: '' })"
        >
          {{ t('university.addHighlight') }}
        </el-button>
      </FormSection>

      <FormSection
        :title="t('university.secRankings')"
        :description="t('university.rankingsHint')"
      >
        <div
          v-for="(r, i) in form.rankings"
          :key="i"
          class="repeat"
        >
          <el-input
            v-model="r.source"
            placeholder="QS / THE / ARWU"
            style="width: 130px"
          />
          <el-input-number
            v-model="r.rankPosition"
            :min="1"
            :controls="false"
            :placeholder="t('university.rankPosition')"
            style="width: 90px"
          />
          <el-input-number
            v-model="r.rankYear"
            :min="1900"
            :max="thisYear + 1"
            :controls="false"
            :placeholder="t('university.rankYear')"
            style="width: 90px"
          />
          <el-input
            v-model="r.note"
            :placeholder="t('common.actions')"
          />
          <el-button
            :icon="Delete"
            text
            @click="form.rankings.splice(i, 1)"
          />
        </div>
        <el-button
          size="small"
          :icon="Plus"
          @click="form.rankings.push({ source: '', rankPosition: null, rankYear: thisYear, note: null })"
        >
          {{ t('university.addRanking') }}
        </el-button>
      </FormSection>

      <FormSection :title="t('university.secPublication')">
        <div class="row2">
          <el-form-item
            :label="t('university.status')"
            prop="status"
          >
            <el-select
              v-model="form.status"
              style="width: 100%"
            >
              <el-option
                v-for="s in STATUSES"
                :key="s"
                :label="titleCase(s)"
                :value="s"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            :label="t('university.publishStatus')"
            prop="publishStatus"
          >
            <el-select
              v-model="form.publishStatus"
              style="width: 100%"
            >
              <el-option
                v-for="s in PUBLISH"
                :key="s"
                :label="titleCase(s)"
                :value="s"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="flags">
          <el-checkbox v-model="form.recommended">
            {{ t('university.recommended') }}
          </el-checkbox>
          <el-checkbox v-model="form.featured">
            {{ t('university.featured') }}
          </el-checkbox>
        </div>
        <div class="row2">
          <el-form-item :label="t('university.admissionsEmail')">
            <el-input v-model="form.admissionsEmail" />
          </el-form-item>
          <el-form-item :label="t('university.officePhone')">
            <el-input v-model="form.officePhone" />
          </el-form-item>
        </div>
        <el-form-item :label="t('university.remark')">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
      </FormSection>
    </el-form>
  </Drawer>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import {
  createUniversity, updateUniversity,
  type University, type UniversityInput,
} from '@/api/university'
import Drawer from '@/components/ui/Drawer.vue'
import FormSection from '@/components/ui/FormSection.vue'

const props = defineProps<{ modelValue: boolean, university: University | null }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [u: University] }>()

const { t } = useI18n()
const thisYear = new Date().getFullYear()
const TYPES = ['PUBLIC', 'PRIVATE'] as const
const STATUSES = ['ACTIVE', 'INACTIVE'] as const
const PUBLISH = ['DRAFT', 'PUBLISHED'] as const
const KINDS = ['HIGHLIGHT', 'ADVANTAGE'] as const

function titleCase(s: string) {
  return s.charAt(0) + s.slice(1).toLowerCase()
}

const formRef = ref<FormInstance>()
const saving = ref(false)

function blankForm() {
  return {
    name: '', nameCn: '', country: '', type: null as string | null,
    city: '', province: '', foundedYear: null as number | null,
    totalStudents: null as number | null, internationalStudents: null as number | null,
    facultyCount: null as number | null, website: '', rankingTier: '',
    introduction: '', history: '', campusInfo: '', accommodationInfo: '', nearbyInfo: '',
    admissionsEmail: '', officePhone: '',
    recommended: false, featured: false,
    status: 'ACTIVE' as UniversityInput['status'],
    publishStatus: 'DRAFT' as UniversityInput['publishStatus'],
    remark: '',
    highlights: [] as University['highlights'],
    rankings: [] as University['rankings'],
  }
}
const form = reactive(blankForm())

const rules = {
  name: [{ required: true, trigger: 'blur', message: t('university.required') }],
  country: [
    { required: true, trigger: 'blur', message: t('university.required') },
    { pattern: /^[A-Za-z]{2}$/, trigger: 'blur', message: t('university.countryFormat') },
  ],
  status: [{ required: true, message: t('university.required') }],
  publishStatus: [{ required: true, message: t('university.required') }],
}

watch(() => props.modelValue, (open) => {
  if (!open) return
  Object.assign(form, blankForm())
  const u = props.university
  if (!u) return
  Object.assign(form, {
    name: u.name, nameCn: u.nameCn ?? '', country: u.country, type: u.type,
    city: u.city ?? '', province: u.province ?? '', foundedYear: u.foundedYear,
    totalStudents: u.totalStudents, internationalStudents: u.internationalStudents,
    facultyCount: u.facultyCount, website: u.website ?? '', rankingTier: u.rankingTier ?? '',
    introduction: u.introduction ?? '', history: u.history ?? '', campusInfo: u.campusInfo ?? '',
    accommodationInfo: u.accommodationInfo ?? '', nearbyInfo: u.nearbyInfo ?? '',
    admissionsEmail: u.admissionsEmail ?? '', officePhone: u.officePhone ?? '',
    recommended: u.recommended, featured: u.featured,
    status: u.status, publishStatus: u.publishStatus, remark: u.remark ?? '',
    highlights: u.highlights.map(h => ({ ...h })),
    rankings: u.rankings.map(r => ({ ...r })),
  })
}, { immediate: true })

function orNum(v: unknown): number | null {
  return v === '' || v === null || v === undefined ? null : Number(v)
}
function orNull(v: string): string | null {
  return v.trim() === '' ? null : v.trim()
}

function payload(): UniversityInput {
  return {
    name: form.name.trim(),
    nameCn: orNull(form.nameCn),
    country: form.country.trim().toUpperCase(),
    type: (form.type as UniversityInput['type']) || null,
    city: orNull(form.city),
    province: orNull(form.province),
    foundedYear: orNum(form.foundedYear),
    totalStudents: orNum(form.totalStudents),
    internationalStudents: orNum(form.internationalStudents),
    facultyCount: orNum(form.facultyCount),
    website: orNull(form.website),
    rankingTier: orNull(form.rankingTier),
    introduction: orNull(form.introduction),
    history: orNull(form.history),
    campusInfo: orNull(form.campusInfo),
    accommodationInfo: orNull(form.accommodationInfo),
    nearbyInfo: orNull(form.nearbyInfo),
    admissionsEmail: orNull(form.admissionsEmail),
    officePhone: orNull(form.officePhone),
    recommended: form.recommended,
    featured: form.featured,
    status: form.status,
    publishStatus: form.publishStatus,
    remark: orNull(form.remark),
    highlights: form.highlights
      .filter(h => h.text.trim())
      .map(h => ({ kind: h.kind, text: h.text.trim() })),
    rankings: form.rankings
      .filter(r => r.source.trim() && r.rankPosition)
      .map(r => ({ source: r.source.trim(), rankPosition: Number(r.rankPosition), rankYear: orNum(r.rankYear), note: orNull(r.note ?? '') })),
  }
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const saved = props.university
      ? await updateUniversity(props.university.id, payload())
      : await createUniversity(payload())
    ElMessage.success(t('common.saved'))
    emit('update:modelValue', false)
    emit('saved', saved)
  }
  finally {
    saving.value = false
  }
}
</script>

<style scoped>
.row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.row3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
}
.repeat {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.repeat > .el-input {
  flex: 1;
}
.flags {
  display: flex;
  gap: 20px;
  margin: 4px 0 12px;
}
</style>
