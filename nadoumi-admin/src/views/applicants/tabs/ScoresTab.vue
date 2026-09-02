<template>
  <div>
    <div
      v-if="canEdit"
      class="tab-toolbar"
    >
      <el-button
        size="small"
        :icon="Plus"
        @click="openAdd"
      >
        {{ t('applicant.addScore') }}
      </el-button>
    </div>

    <StatePanel
      :loading="list.loading.value"
      :error="list.error.value"
      :empty="list.items.value.length === 0"
      :empty-title="t('applicant.noScores')"
      @retry="list.reload"
    >
      <el-table :data="list.items.value">
        <el-table-column
          :label="t('applicant.testType')"
          prop="testType"
          width="140"
        />
        <el-table-column
          :label="t('applicant.score')"
          prop="score"
          width="120"
        />
        <el-table-column
          :label="t('applicant.takenOn')"
          width="150"
        >
          <template #default="{ row }">
            {{ fmtDate(row.takenOn) }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('applicant.expiresOn')"
          width="150"
        >
          <template #default="{ row }">
            {{ fmtDate(row.expiresOn) }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="canEdit"
          width="110"
          align="right"
        >
          <template #default="{ row }">
            <el-button
              link
              size="small"
              @click="openEdit(row as TestScore)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              link
              size="small"
              type="danger"
              @click="onDelete(row as TestScore)"
            >
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </StatePanel>

    <Drawer
      v-model="open"
      :title="editing ? t('applicant.editScore') : t('applicant.addScore')"
      :saving="saving"
      @save="save"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <el-form-item
          :label="t('applicant.testType')"
          prop="testType"
        >
          <el-input
            v-model="form.testType"
            placeholder="IELTS / TOEFL / GRE…"
          />
        </el-form-item>
        <el-form-item
          :label="t('applicant.score')"
          prop="score"
        >
          <el-input v-model="form.score" />
        </el-form-item>
        <div class="row2">
          <el-form-item :label="t('applicant.takenOn')">
            <el-date-picker
              v-model="form.takenOn"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item :label="t('applicant.expiresOn')">
            <el-date-picker
              v-model="form.expiresOn"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </div>
      </el-form>
    </Drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listTestScores, addTestScore, updateTestScore, deleteTestScore,
  type TestScore, type TestScoreInput,
} from '@/api/applicant'
import { useResourceList } from '@/composables/useResourceList'
import { useConfirm } from '@/composables/useConfirm'
import StatePanel from '@/components/ui/StatePanel.vue'
import Drawer from '@/components/ui/Drawer.vue'

const props = defineProps<{ id: string, canEdit: boolean }>()
const emit = defineEmits<{ count: [n: number] }>()

const { t } = useI18n()
const { confirm } = useConfirm()
const list = useResourceList<TestScore>(() => listTestScores(props.id))

watch(list.items, v => emit('count', v.length))
onMounted(list.load)

function fmtDate(v: string | null): string {
  return v ? new Date(v).toLocaleDateString() : '—'
}

const open = ref(false)
const saving = ref(false)
const editing = ref<TestScore | null>(null)
const formRef = ref<FormInstance>()
const blank = { testType: '', score: '', takenOn: '', expiresOn: '' }
const form = reactive({ ...blank })
const rules = {
  testType: [{ required: true, trigger: 'blur', message: t('applicant.required') }],
  score: [{ required: true, trigger: 'blur', message: t('applicant.required') }],
}

function openAdd() {
  editing.value = null
  Object.assign(form, blank)
  open.value = true
}
function openEdit(row: TestScore) {
  editing.value = row
  Object.assign(form, {
    testType: row.testType, score: row.score,
    takenOn: row.takenOn ?? '', expiresOn: row.expiresOn ?? '',
  })
  open.value = true
}

function payload(): TestScoreInput {
  return {
    testType: form.testType.trim(),
    score: form.score.trim(),
    takenOn: form.takenOn || null,
    expiresOn: form.expiresOn || null,
  }
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editing.value) await updateTestScore(props.id, editing.value.id, payload())
    else await addTestScore(props.id, payload())
    ElMessage.success(t('common.saved'))
    open.value = false
    await list.reload()
  }
  finally {
    saving.value = false
  }
}

async function onDelete(row: TestScore) {
  const ok = await confirm({
    message: t('applicant.deleteScoreConfirm', { type: row.testType }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  await deleteTestScore(props.id, row.id)
  ElMessage.success(t('common.deleted'))
  await list.reload()
}
</script>

<style scoped>
.tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>
