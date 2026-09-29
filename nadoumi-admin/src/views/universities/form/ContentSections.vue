<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Plus, Delete } from '@element-plus/icons-vue'
import FormSection from '@/components/ui/FormSection.vue'
import { titleCase } from '@/utils/text'
import { useUniversityForm } from '../universityForm'

const { t } = useI18n()
const form = useUniversityForm()

const thisYear = new Date().getFullYear()
const KINDS = ['HIGHLIGHT', 'ADVANTAGE'] as const
</script>

<template>
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
          :placeholder="t('university.accommodationHint')"
        />
      </el-form-item>
      <el-form-item :label="t('university.nearbyInfo')">
        <el-input
          v-model="form.nearbyInfo"
          type="textarea"
          :rows="2"
          :placeholder="t('university.nearbyHint')"
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
</template>

<style scoped src="../universityForm.css" />
