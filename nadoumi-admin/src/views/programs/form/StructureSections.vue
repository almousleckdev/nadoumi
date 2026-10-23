<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Delete } from '@element-plus/icons-vue'
import { INTAKE_TERMS, isDegree as isDegreeKind } from '@/api/program'
import type { Department } from '@/api/university'
import FormSection from '@/components/ui/FormSection.vue'
import AddMajorsDialog, { type NewMajorRow } from './AddMajorsDialog.vue'
import MajorPicker from './MajorPicker.vue'
import { useProgramForm } from '../programForm'

defineProps<{ departments: Department[] }>()

const { t } = useI18n()
const form = useProgramForm()

const isDegree = computed(() => isDegreeKind(form.programType))
const addSeveralOpen = ref(false)

/** Adds the dialog's rows, skipping a major already listed at the same level. */
function addMajors(rows: NewMajorRow[]) {
  const same = (a: NewMajorRow, b: NewMajorRow) =>
    a.level === b.level && (a.majorId != null ? a.majorId === b.majorId : a.name.toLowerCase() === b.name.toLowerCase())
  for (const row of rows) {
    if (!form.majors.some(existing => same(existing, row))) form.majors.push(row)
  }
}
</script>

<template>
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
      <MajorPicker
        :row="m"
        class="line__major"
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
      @click="addSeveralOpen = true"
    >
      {{ t('program.addSeveral') }}
    </el-button>
    <el-button
      :icon="Plus"
      @click="form.majors.push({ majorId: null, name: '', nameCn: null, departmentId: null, level: form.levels[0] ?? null })"
    >
      {{ t('program.addMajor') }}
    </el-button>
    <AddMajorsDialog
      v-model="addSeveralOpen"
      :program-levels="form.levels"
      :departments="departments"
      @add="addMajors"
    />
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
</template>

<style scoped src="../programForm.css" />
