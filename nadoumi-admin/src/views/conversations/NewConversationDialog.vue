<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { createConversation } from '@/api/conversation'
import { listUsers, type SysUserRow } from '@/api/system'

const STUDENT_USER_TYPE = '10'
const STUDENT_PAGE_SIZE = 100
const DEFAULT_SUBJECT = 'Conversation with Nadoumi Administration'

const open = defineModel<boolean>({ required: true })
const emit = defineEmits<{ created: [conversationId: number] }>()

const { t } = useI18n()

const students = ref<SysUserRow[]>([])
const loadingStudents = ref(false)
const creating = ref(false)
const form = reactive({
  studentUserId: undefined as number | undefined,
  applicationId: undefined as number | undefined,
  subject: '',
  body: '',
})

const canCreate = computed(() => Boolean(form.studentUserId) && form.body.trim().length > 0)

async function loadStudents() {
  if (students.value.length) return
  loadingStudents.value = true
  try {
    const res = await listUsers({ userType: STUDENT_USER_TYPE, pageSize: STUDENT_PAGE_SIZE })
    students.value = res.rows || []
  }
  catch {
    students.value = []
  }
  finally {
    loadingStudents.value = false
  }
}

watch(open, (isOpen) => {
  if (!isOpen) return
  form.studentUserId = undefined
  form.applicationId = undefined
  form.subject = ''
  form.body = ''
  void loadStudents()
})

async function submit() {
  if (!canCreate.value) return
  creating.value = true
  try {
    const res = await createConversation({
      studentUserId: form.studentUserId!,
      applicationId: form.applicationId || undefined,
      subject: form.subject.trim() || DEFAULT_SUBJECT,
      body: form.body.trim(),
    })
    ElMessage.success(t('conversations.dialog.created'))
    open.value = false
    if (res.conversationId) emit('created', res.conversationId)
  }
  catch (e) {
    ElMessage.error((e as Error)?.message || t('conversations.dialog.createFailed'))
  }
  finally {
    creating.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="open"
    :title="t('conversations.dialog.title')"
    width="540px"
    destroy-on-close
  >
    <el-form label-position="top">
      <el-form-item
        :label="t('conversations.dialog.student')"
        required
      >
        <el-select
          v-model="form.studentUserId"
          :placeholder="t('conversations.dialog.studentPlaceholder')"
          filterable
          :loading="loadingStudents"
          style="width: 100%"
        >
          <el-option
            v-for="s in students"
            :key="s.userId"
            :label="`${s.nickName || s.userName} (@${s.userName})${s.email ? ' · ' + s.email : ''}`"
            :value="s.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('conversations.dialog.applicationId')">
        <el-input-number
          v-model="form.applicationId"
          :min="1"
          :placeholder="t('conversations.dialog.applicationIdPlaceholder')"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('conversations.dialog.subject')">
        <el-input
          v-model="form.subject"
          :placeholder="t('conversations.dialog.subjectPlaceholder')"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
      <el-form-item
        :label="t('conversations.dialog.message')"
        required
      >
        <el-input
          v-model="form.body"
          type="textarea"
          :rows="5"
          :placeholder="t('conversations.dialog.messagePlaceholder')"
          maxlength="4000"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="open = false">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="primary"
        :loading="creating"
        :disabled="!canCreate"
        @click="submit"
      >
        {{ t('conversations.dialog.start') }}
      </el-button>
    </template>
  </el-dialog>
</template>
