<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Delete } from '@element-plus/icons-vue'
import type { University } from '@/api/university'
import FormSection from '@/components/ui/FormSection.vue'
import ImageUpload from '@/components/ui/ImageUpload.vue'
import { uploaderTask, type DeferredTask } from '@/composables/useDeferredImages'
import { MAX_GALLERY, useUniversityForm } from '../universityForm'
import { useUniversityGallery } from '../useUniversityGallery'

defineProps<{ university: University | null }>()

const { t } = useI18n()
const form = useUniversityForm()
const { addFiles, removeRow, heldUploadTasks } = useUniversityGallery(form)

type Uploader = InstanceType<typeof ImageUpload>
const logoUp = ref<Uploader>()
const bannerUp = ref<Uploader>()
const galUps = ref<Uploader[]>([])
const galleryPicker = ref<HTMLInputElement>()

function setGalUp(el: unknown, i: number) {
  if (el) galUps.value[i] = el as Uploader
}

async function onGalleryFiles(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = ''
  await addFiles(files)
}

function uploadTasks(id: number): DeferredTask[] {
  return [
    uploaderTask(logoUp.value, id),
    uploaderTask(bannerUp.value, id),
    ...galUps.value.map(up => uploaderTask(up, id)),
    ...heldUploadTasks(id),
  ]
}

defineExpose({ uploadTasks })
</script>

<template>
  <FormSection :title="t('university.secImages')">
    <div class="imgs">
      <el-form-item :label="t('university.logoImage')">
        <ImageUpload
          ref="logoUp"
          v-model="form.logoMediaId"
          :action="`/api/staff/universities/${form.id}/logo`"
          :resolve-action="(id) => `/api/staff/universities/${id}/logo`"
          :deferred="!form.id"
          :preview-url="university?.logoUrl ?? university?.logoImageUrl"
          aspect="square"
        />
      </el-form-item>
      <el-form-item :label="t('university.coverImage')">
        <ImageUpload
          ref="bannerUp"
          v-model="form.bannerMediaId"
          :action="`/api/staff/universities/${form.id}/banner`"
          :resolve-action="(id) => `/api/staff/universities/${id}/banner`"
          :deferred="!form.id"
          :preview-url="university?.bannerUrl ?? university?.coverImageUrl"
          aspect="wide"
        />
      </el-form-item>
    </div>
  </FormSection>

  <FormSection
    :title="t('university.secGallery')"
    :description="t('university.galleryHint')"
  >
    <div
      v-for="(g, i) in form.gallery"
      :key="i"
      class="repeat gal-row"
    >
      <ImageUpload
        :ref="(el) => setGalUp(el, i)"
        v-model="g.mediaId"
        :action="`/api/staff/universities/${form.id}/gallery`"
        :resolve-action="(id) => `/api/staff/universities/${id}/gallery`"
        :deferred="!form.id"
        :preview-url="g.url ?? g.imageUrl"
        aspect="wide"
      />
      <el-input
        v-model="g.caption"
        :placeholder="t('university.galleryCaption')"
        style="width: 180px"
      />
      <el-button
        :icon="Delete"
        text
        @click="removeRow(i)"
      />
    </div>
    <div
      v-if="form.gallery.length < MAX_GALLERY"
      class="gal-actions"
    >
      <input
        ref="galleryPicker"
        type="file"
        accept="image/*"
        multiple
        hidden
        @change="onGalleryFiles"
      >
      <el-button
        size="small"
        :icon="Plus"
        @click="galleryPicker?.click()"
      >
        {{ t('university.addGalleryImages') }}
      </el-button>
      <el-button
        size="small"
        text
        :icon="Plus"
        @click="form.gallery.push({ imageUrl: null, mediaId: null, url: null, caption: null })"
      >
        {{ t('university.addGalleryImage') }}
      </el-button>
    </div>
    <p
      v-else
      class="gal-max"
    >
      {{ t('university.galleryMax') }}
    </p>
  </FormSection>
</template>

<style scoped src="../universityForm.css" />
