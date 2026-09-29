<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Program } from '@/api/program'
import FormSection from '@/components/ui/FormSection.vue'
import ImageUpload from '@/components/ui/ImageUpload.vue'
import { uploaderTask, type DeferredTask } from '@/composables/useDeferredImages'
import { useProgramForm } from '../programForm'

defineProps<{ program: Program | null }>()

const { t } = useI18n()
const form = useProgramForm()

const STATUSES = ['ACTIVE', 'INACTIVE'] as const
const PUBLISH = ['DRAFT', 'PUBLISHED'] as const

const imageUp = ref<InstanceType<typeof ImageUpload>>()

function uploadTasks(id: number | string): DeferredTask[] {
  return [uploaderTask(imageUp.value, id)]
}

defineExpose({ uploadTasks })
</script>

<template>
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
</template>

<style scoped src="../programForm.css" />
