<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Plus, Delete } from '@element-plus/icons-vue'
import FormSection from '@/components/ui/FormSection.vue'
import { titleCase } from '@/utils/text'
import { useUniversityForm } from '../universityForm'

const { t } = useI18n()
const form = useUniversityForm()

const thisYear = new Date().getFullYear()
const TYPES = ['PUBLIC', 'PRIVATE'] as const
</script>

<template>
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

  <FormSection
    :title="t('university.secDepartments')"
    :description="t('university.departmentsHint')"
  >
    <div
      v-for="(d, i) in form.departments"
      :key="i"
      class="repeat"
    >
      <el-input
        v-model="d.name"
        :placeholder="t('department.name')"
        maxlength="120"
      />
      <el-input
        v-model="d.nameCn"
        :placeholder="t('department.nameCn')"
        maxlength="120"
      />
      <el-button
        :icon="Delete"
        text
        @click="form.departments.splice(i, 1)"
      />
    </div>
    <el-button
      size="small"
      :icon="Plus"
      @click="form.departments.push({ id: undefined, name: '', nameCn: '' })"
    >
      {{ t('university.addDepartment') }}
    </el-button>
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
</template>

<style scoped src="../universityForm.css" />
