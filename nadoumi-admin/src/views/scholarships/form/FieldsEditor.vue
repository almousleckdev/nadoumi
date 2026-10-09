<script setup lang="ts">
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { EDUCATION_LEVELS, type EducationLevel } from '@/api/scholarship'
import { useScholarshipForm } from '../scholarshipForm'

const MAX_NAME_CHARS = 120

const { t } = useI18n()
const form = useScholarshipForm()

/** The levels offered, in the order students think of them. */
const offered = computed(() => EDUCATION_LEVELS.filter(level => form.levels.includes(level)))

function namesFor(level: EducationLevel | null): string[] {
  return form.fields.filter(f => f.level === level).map(f => f.name)
}

// Replaces one scope's names (every level, or one level) and keeps everything else as it was.
function setNames(level: EducationLevel | null, names: string[]) {
  const clean = [...new Set(names.map(n => n.trim()).filter(n => n !== '' && n.length <= MAX_NAME_CHARS))]
  form.fields = [
    ...form.fields.filter(f => f.level !== level),
    ...clean.map(name => ({ level, name })),
  ]
}

// A field set for a level that is later unticked no longer applies to anything: drop it.
watch(() => [...form.levels], (levels) => {
  const kept = form.fields.filter(f => f.level === null || levels.includes(f.level))
  if (kept.length !== form.fields.length) form.fields = kept
})
</script>

<template>
  <div class="fields">
    <el-form-item
      :label="t('scholarship.fieldsAll')"
      data-test="fields-all"
    >
      <el-select
        :model-value="namesFor(null)"
        multiple
        filterable
        allow-create
        default-first-option
        :reserve-keyword="false"
        :placeholder="t('scholarship.fieldHint')"
        :no-data-text="t('scholarship.fieldsTypeHint')"
        @update:model-value="(v: string[]) => setNames(null, v)"
      />
      <p class="hint">
        {{ t('scholarship.fieldNote') }}
      </p>
    </el-form-item>

    <el-form-item
      v-for="level in offered"
      :key="level"
      :label="t('scholarship.fieldsOnly', { level: t(`scholarship.level.${level}`) })"
      :data-test="`fields-${level}`"
    >
      <el-select
        :model-value="namesFor(level)"
        multiple
        filterable
        allow-create
        default-first-option
        :reserve-keyword="false"
        :placeholder="t('scholarship.fieldsOnlyHint', { level: t(`scholarship.level.${level}`) })"
        :no-data-text="t('scholarship.fieldsTypeHint')"
        @update:model-value="(v: string[]) => setNames(level, v)"
      />
    </el-form-item>
  </div>
</template>

<style scoped>
.fields :deep(.el-select) {
  width: 100%;
}
.hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--nad-ink-soft);
}
</style>
