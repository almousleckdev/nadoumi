<script setup lang="ts">
import type { AdminContact } from '~/types/messages'
import type { StudentApplicationDto } from '~/types/catalog'

const props = defineProps<{ applicantId: number | undefined }>()
const emit = defineEmits<{ created: [conversationId: number] }>()

const open = defineModel<boolean>({ required: true })

const { t } = useI18n()
const { listAdmins, open: openConversation } = useMessages()
const { list: listApplications } = useApplications()

const admins = ref<AdminContact[]>([])
const studentApplications = ref<StudentApplicationDto[]>([])
const adminUserId = ref('')
const applicationId = ref('')
const subject = ref('')
const messageBody = ref('')
const submitting = ref(false)
const error = ref('')

const adminSelectOptions = computed(() => [
  { value: '', label: t('dashboard.messages.defaultAdvisor') },
  ...admins.value.map((a: AdminContact) => ({
    value: String(a.userId),
    label: `${a.name}${a.email ? ` (${a.email})` : ''}`,
  })),
])

const applicationSelectOptions = computed(() => [
  { value: '', label: '—' },
  ...studentApplications.value.map((app: StudentApplicationDto) => ({
    value: String(app.id),
    label: `#${app.id} - ${app.applicationType || t('dashboard.messages.applicationFallback')} (${app.currentStatus || t('dashboard.messages.draftFallback')})`,
  })),
])

async function loadContacts() {
  try {
    if (!admins.value.length && listAdmins) admins.value = await listAdmins()
    if (!studentApplications.value.length && listApplications) studentApplications.value = await listApplications()
  }
  catch {
    // Best-effort; fall back to the default advisor and manual input
  }
}

watch(open, (isOpen: boolean) => {
  if (!isOpen) return
  error.value = ''
  subject.value = ''
  messageBody.value = ''
  adminUserId.value = ''
  applicationId.value = ''
  void loadContacts()
})

async function submit() {
  if (!subject.value.trim() || !messageBody.value.trim()) return
  submitting.value = true
  error.value = ''
  try {
    const opened = await openConversation({
      applicantId: props.applicantId,
      applicationId: applicationId.value ? Number(applicationId.value) : undefined,
      adminUserId: adminUserId.value ? Number(adminUserId.value) : undefined,
      subject: subject.value.trim(),
      body: messageBody.value.trim(),
    })
    open.value = false
    if (opened && opened.conversationId) emit('created', opened.conversationId)
  }
  catch (err: unknown) {
    error.value = err instanceof Error ? err.message : t('errors.unexpected')
  }
  finally {
    submitting.value = false
  }
}
</script>

<template>
<NModal v-model="open" :title="t('dashboard.messages.contactAdmin')">
  <form class="space-y-4" @submit.prevent="submit">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

    <NField :label="t('dashboard.messages.selectAdvisor')" for="new-advisor">
      <NSelect
        id="new-advisor"
        v-model="adminUserId"
        :options="adminSelectOptions"
      />
    </NField>

    <NField v-if="applicationSelectOptions.length > 1" :label="t('dashboard.messages.applicationId')" for="new-application">
      <NSelect
        id="new-application"
        v-model="applicationId"
        :options="applicationSelectOptions"
      />
    </NField>

    <NField :label="t('dashboard.messages.subject')" for="new-subject" required>
      <NInput
        id="new-subject"
        v-model="subject"
        :placeholder="t('dashboard.messages.subjectPlaceholder')"
        required
      />
    </NField>

    <NField :label="t('dashboard.messages.messageBody')" for="new-message-body" required>
      <NTextarea
        id="new-message-body"
        v-model="messageBody"
        :rows="4"
        :placeholder="t('dashboard.messages.messagePlaceholder')"
        required
      />
    </NField>

    <div class="flex justify-end gap-2 pt-2">
      <NButton type="button" variant="ghost" @click="open = false">
        {{ t('common.cancel') }}
      </NButton>
      <NButton type="submit" :loading="submitting">
        {{ t('dashboard.messages.send') }}
      </NButton>
    </div>
  </form>
</NModal>
</template>
