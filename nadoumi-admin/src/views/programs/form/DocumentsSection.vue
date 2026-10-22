<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Plus, Delete } from '@element-plus/icons-vue'
import { DOC_TYPES } from '@/api/scholarship'
import FormSection from '@/components/ui/FormSection.vue'
import { useProgramForm } from '../programForm'

const { t } = useI18n()
const form = useProgramForm()
</script>

<template>
  <FormSection
    :title="t('program.documents.title')"
    :description="t('program.documents.hint')"
  >
    <div
      v-for="(d, i) in form.documentRequirements"
      :key="i"
      class="doc-line"
    >
      <el-select
        v-model="d.docType"
        class="w-52"
        filterable
      >
        <el-option
          v-for="dt in DOC_TYPES"
          :key="dt"
          :value="dt"
          :label="t(`scholarship.doc.${dt}`, dt)"
        />
      </el-select>
      <el-checkbox v-model="d.mandatory">
        {{ t('program.documents.mandatory') }}
      </el-checkbox>
      <el-input
        v-model="d.notes"
        maxlength="255"
        :placeholder="t('program.documents.note')"
      />
      <el-button
        link
        type="danger"
        :icon="Delete"
        :aria-label="t('common.delete')"
        @click="form.documentRequirements.splice(i, 1)"
      />
    </div>
    <el-button
      :icon="Plus"
      @click="form.documentRequirements.push({ docType: 'PASSPORT', mandatory: true, notes: '' })"
    >
      {{ t('program.documents.add') }}
    </el-button>
  </FormSection>
</template>

<style scoped>
.doc-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
