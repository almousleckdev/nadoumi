<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import { getScholarshipInternal, putScholarshipInternal, type ScholarshipInternal } from '@/api/scholarship'
import { listPrograms } from '@/api/program'
import { useUserStore } from '@/stores/user'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import FormSection from '@/components/ui/FormSection.vue'

const PROGRAM_OPTIONS_SIZE = 200

const props = defineProps<{ scholarshipId: string }>()

const { t } = useI18n()
const userStore = useUserStore()

const canView = computed(() => userStore.hasPerm('nad:scholarship:internal:view'))

const internal = ref<ScholarshipInternal | null>(null)
const internalOpen = ref(false)
const savingInternal = ref(false)
const internalForm = reactive({
  universityId: null as number | null, programId: null as number | null, internalStatus: '',
  operationalNotes: '', confidentialTerms: '', commissionModelJson: '',
})

// programmes of the linked university, for the confidential programme picker
const programOptions = ref<{ id: number, name: string }[]>([])
async function loadProgramOptions(universityId: number | null) {
  if (!universityId) { programOptions.value = []; return }
  try {
    const res = await listPrograms({ universityId, page: 0, size: PROGRAM_OPTIONS_SIZE })
    programOptions.value = res.content.map(p => ({ id: p.id, name: p.name }))
  }
  catch { programOptions.value = [] }
}

function onInternalUniversityChange() {
  internalForm.programId = null
  loadProgramOptions(internalForm.universityId)
}

const linkedProgramLabel = computed(() => {
  if (!internal.value?.programId) return ''
  const hit = programOptions.value.find(p => p.id === internal.value?.programId)
  return hit ? hit.name : `#${internal.value.programId}`
})

async function load() {
  if (!canView.value) return
  internal.value = await getScholarshipInternal(props.scholarshipId).catch(() => null)
  if (internal.value?.programId) await loadProgramOptions(internal.value.universityId ?? null)
}

watch(internalOpen, (open) => {
  if (!open) return
  internalForm.universityId = internal.value?.universityId ?? null
  internalForm.programId = internal.value?.programId ?? null
  internalForm.internalStatus = internal.value?.internalStatus ?? ''
  internalForm.operationalNotes = internal.value?.operationalNotes ?? ''
  internalForm.confidentialTerms = internal.value?.confidentialTerms ?? ''
  internalForm.commissionModelJson = internal.value?.commissionModelJson ?? ''
  loadProgramOptions(internalForm.universityId)
})

async function saveInternal() {
  savingInternal.value = true
  try {
    internal.value = await putScholarshipInternal(props.scholarshipId, {
      universityId: internalForm.universityId,
      programId: internalForm.universityId ? internalForm.programId : null,
      internalStatus: internalForm.internalStatus || null,
      operationalNotes: internalForm.operationalNotes || null,
      confidentialTerms: internalForm.confidentialTerms || null,
      commissionModelJson: internalForm.commissionModelJson || null,
    })
    ElMessage.success(t('common.saved'))
    internalOpen.value = false
  }
  finally {
    savingInternal.value = false
  }
}

onMounted(load)

defineExpose({ reload: load })
</script>

<template>
  <FormSection
    v-if="canView"
    :title="t('scholarship.secInternal')"
    :description="t('scholarship.internalHint')"
  >
    <DescriptionList
      :items="[
        { label: t('scholarship.partnerUniversity'), value: internal?.universityName || (internal?.universityId ? `#${internal.universityId}` : '') },
        { label: t('scholarship.linkedProgram'), value: linkedProgramLabel },
        { label: t('scholarship.internalStatus'), value: internal?.internalStatus || 'DRAFT' },
        { label: t('scholarship.operationalNotes'), value: internal?.operationalNotes || '' },
        { label: t('scholarship.confidentialTerms'), value: internal?.confidentialTerms || '' },
      ]"
    />
    <el-button
      v-if="userStore.hasPerm('nad:scholarship:internal:edit')"
      size="small"
      :icon="Edit"
      style="margin-top: 8px"
      @click="internalOpen = true"
    >
      {{ t('scholarship.editInternal') }}
    </el-button>
  </FormSection>

  <el-dialog
    v-model="internalOpen"
    :title="t('scholarship.editInternal')"
    width="480"
  >
    <el-form label-position="top">
      <el-form-item :label="t('scholarship.partnerUniversityId')">
        <el-input-number
          v-model="internalForm.universityId"
          :min="1"
          controls-position="right"
          @change="onInternalUniversityChange"
        />
      </el-form-item>
      <el-form-item :label="t('scholarship.linkedProgram')">
        <el-select
          v-model="internalForm.programId"
          clearable
          filterable
          :disabled="!internalForm.universityId"
          :placeholder="internalForm.universityId ? t('scholarship.linkedProgramPlaceholder') : t('scholarship.linkedProgramNeedsUniversity')"
          style="width: 100%"
        >
          <el-option
            v-for="p in programOptions"
            :key="p.id"
            :value="p.id"
            :label="p.name"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('scholarship.internalStatus')">
        <el-input
          v-model="internalForm.internalStatus"
          maxlength="24"
        />
      </el-form-item>
      <el-form-item :label="t('scholarship.operationalNotes')">
        <el-input
          v-model="internalForm.operationalNotes"
          type="textarea"
          :rows="3"
        />
      </el-form-item>
      <el-form-item :label="t('scholarship.confidentialTerms')">
        <el-input
          v-model="internalForm.confidentialTerms"
          type="textarea"
          :rows="2"
        />
      </el-form-item>
      <el-form-item :label="t('scholarship.commissionModelJson')">
        <el-input
          v-model="internalForm.commissionModelJson"
          type="textarea"
          :rows="2"
          placeholder="{&quot;rate&quot;:0.15}"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="internalOpen = false">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="primary"
        :loading="savingInternal"
        @click="saveInternal"
      >
        {{ t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped src="./detail.css" />
