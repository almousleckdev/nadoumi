<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Delete } from '@element-plus/icons-vue'
import { INTAKE_TERMS, isDegree as isDegreeKind } from '@/api/program'
import type { Department } from '@/api/university'
import FormSection from '@/components/ui/FormSection.vue'
import { useProgramForm } from '../programForm'

defineProps<{ departments: Department[] }>()

const { t } = useI18n()
const form = useProgramForm()

const isDegree = computed(() => isDegreeKind(form.programType))
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
</template>

<style scoped src="../programForm.css" />
