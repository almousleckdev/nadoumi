<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Scholarship } from '@/api/scholarship'
import FormSection from '@/components/ui/FormSection.vue'
import ImageUpload from '@/components/ui/ImageUpload.vue'
import { uploaderTask, type DeferredTask } from '@/composables/useDeferredImages'
import { useScholarshipForm } from '../scholarshipForm'

defineProps<{ scholarship: Scholarship | null }>()

const { t } = useI18n()
const form = useScholarshipForm()

const STATUSES = ['ACTIVE', 'INACTIVE'] as const
const PUBLISH = ['DRAFT', 'PUBLISHED'] as const

const heroUp = ref<InstanceType<typeof ImageUpload>>()
const coverUp = ref<InstanceType<typeof ImageUpload>>()

function uploadTasks(id: number | string): DeferredTask[] {
  return [uploaderTask(heroUp.value, id), uploaderTask(coverUp.value, id)]
}

defineExpose({ uploadTasks })
</script>

<template>
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
</template>

<style scoped src="../scholarshipForm.css" />
