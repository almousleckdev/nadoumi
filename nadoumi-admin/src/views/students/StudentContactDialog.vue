<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createConversation } from '@/api/conversation'
import type { SysUserRow } from '@/api/system'

const DEFAULT_SUBJECT = 'Message from Nadoumi Administration'

const props = defineProps<{ student: SysUserRow | null }>()
const open = defineModel<boolean>({ required: true })

const { t } = useI18n()
const router = useRouter()

const sending = ref(false)
const form = reactive({ subject: '', body: '' })

const title = computed(() => t('students.sendMessageModalTitle', {
  name: props.student?.nickName || props.student?.userName,
}))

watch(open, (isOpen) => {
  if (!isOpen) return
  form.subject = DEFAULT_SUBJECT
  form.body = ''
})

async function offerConversation() {
  try {
    await ElMessageBox.confirm(t('students.openConversationPrompt'), t('students.messageSentTitle'), {
      confirmButtonText: t('students.goToConversations'),
      cancelButtonText: t('students.stayHere'),
      type: 'success',
    })
    router.push('/conversations')
  }
  catch {
    /* the user chose to stay on this page */
  }
}

async function submit() {
  if (!props.student || !form.body.trim()) return
  sending.value = true
  try {
    await createConversation({
      studentUserId: props.student.userId,
      subject: form.subject.trim() || DEFAULT_SUBJECT,
      body: form.body.trim(),
    })
    ElMessage.success(t('students.messageSent'))
    open.value = false
    void offerConversation()
  }
  catch (e) {
    ElMessage.error((e as Error)?.message || t('students.sendFailed'))
  }
  finally {
    sending.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="open"
    :title="title"
    width="540px"
    destroy-on-close
  >
    <el-form
      label-position="top"
      class="contact-form"
    >
      <el-form-item :label="t('students.messageSubject')">
        <el-input
          v-model="form.subject"
          :placeholder="t('students.messageSubjectPlaceholder')"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
      <el-form-item
        :label="t('students.messageBody')"
        required
      >
        <el-input
          v-model="form.body"
          type="textarea"
          :rows="5"
          :placeholder="t('students.messageBodyPlaceholder')"
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
        :loading="sending"
        :disabled="!form.body.trim()"
        @click="submit"
      >
        {{ t('students.sendDirectMessage') }}
      </el-button>
    </template>
  </el-dialog>
</template>
