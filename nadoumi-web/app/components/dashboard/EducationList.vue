<script setup lang="ts">
import type { EducationDto } from '~/types/catalog'
import type { EducationBody } from '~/composables/useApplicant'

defineProps<{ items: EducationDto[]; busy: boolean }>()
const emit = defineEmits<{
  add: [body: EducationBody]
  update: [id: number, body: EducationBody]
  remove: [id: number]
}>()
const { t } = useI18n()

const adding = ref(false)
const editingId = ref<number | null>(null)
const removeId = ref<number | null>(null)
const blank = (): EducationBody => ({ institution: '', level: '', field: '', gpa: undefined, gpaScale: undefined, startDate: '', endDate: '' })
const draft = reactive<EducationBody>(blank())
const draftError = ref('')

function startAdd() { Object.assign(draft, blank()); adding.value = true; editingId.value = null; draftError.value = '' }
function startEdit(row: EducationDto) {
  Object.assign(draft, { institution: row.institution, level: row.level ?? '', field: row.field ?? '', gpa: row.gpa ?? undefined, gpaScale: row.gpaScale ?? undefined, startDate: row.startDate ?? '', endDate: row.endDate ?? '' })
  editingId.value = row.id; adding.value = false; draftError.value = ''
}
function saveNew() {
  if (!draft.institution) { draftError.value = t('validation.required'); return }
  emit('add', { ...draft }); adding.value = false
}
function saveEdit() {
  if (!draft.institution) { draftError.value = t('validation.required'); return }
  if (editingId.value != null) emit('update', editingId.value, { ...draft })
  editingId.value = null
}
</script>

<template>
  <div class="grid gap-4">
    <p v-if="!items.length && !adding" class="text-sm text-slate-500">{{ t('dashboard.eduEmpty') }}</p>

    <table v-if="items.length" class="w-full text-sm">
      <thead class="text-left text-slate-500">
        <tr><th class="py-2">{{ t('dashboard.institution') }}</th><th>{{ t('dashboard.level') }}</th><th>{{ t('dashboard.field') }}</th><th>{{ t('dashboard.gpa') }}</th><th /></tr>
      </thead>
      <tbody>
        <template v-for="row in items" :key="row.id">
          <tr v-if="editingId !== row.id" class="border-t border-slate-100">
            <td class="py-2">{{ row.institution }}</td><td>{{ row.level }}</td><td>{{ row.field }}</td><td>{{ row.gpa }}</td>
            <td class="text-right">
              <button class="text-brand-700 hover:underline" @click="startEdit(row)">{{ t('common.edit') }}</button>
              <button :data-test="`remove-${row.id}`" class="ms-3 text-red-600 hover:underline" @click="removeId = row.id">{{ t('common.remove') }}</button>
            </td>
          </tr>
          <tr v-else class="border-t border-slate-100">
            <td colspan="5" class="py-3">
              <div class="grid gap-3 sm:grid-cols-2">
                <NAlert v-if="draftError" tone="danger" class="sm:col-span-2">{{ draftError }}</NAlert>
                <NField :label="t('dashboard.institution')" for="edu-institution-e" required><NInput id="edu-institution-e" v-model="draft.institution" /></NField>
                <NField :label="t('dashboard.level')" for="edu-level-e"><NInput id="edu-level-e" v-model="draft.level" /></NField>
                <NField :label="t('dashboard.field')" for="edu-field-e"><NInput id="edu-field-e" v-model="draft.field" /></NField>
                <NField :label="t('dashboard.startDate')" for="edu-start-e"><NInput id="edu-start-e" v-model="draft.startDate" type="date" /></NField>
                <NField :label="t('dashboard.endDate')" for="edu-end-e"><NInput id="edu-end-e" v-model="draft.endDate" type="date" /></NField>
              </div>
              <div class="mt-3 flex gap-2">
                <NButton size="sm" :loading="busy" @click="saveEdit">{{ t('common.save') }}</NButton>
                <NButton size="sm" variant="secondary" @click="editingId = null">{{ t('common.cancel') }}</NButton>
              </div>
            </td>
          </tr>
        </template>
      </tbody>
    </table>

    <div v-if="adding" class="rounded-md border border-slate-200 p-4">
      <div class="grid gap-3 sm:grid-cols-2">
        <NAlert v-if="draftError" tone="danger" class="sm:col-span-2">{{ draftError }}</NAlert>
        <NField :label="t('dashboard.institution')" for="edu-institution" required><NInput id="edu-institution" v-model="draft.institution" /></NField>
        <NField :label="t('dashboard.level')" for="edu-level"><NInput id="edu-level" v-model="draft.level" /></NField>
        <NField :label="t('dashboard.field')" for="edu-field"><NInput id="edu-field" v-model="draft.field" /></NField>
        <NField :label="t('dashboard.startDate')" for="edu-start"><NInput id="edu-start" v-model="draft.startDate" type="date" /></NField>
        <NField :label="t('dashboard.endDate')" for="edu-end"><NInput id="edu-end" v-model="draft.endDate" type="date" /></NField>
      </div>
      <div class="mt-3 flex gap-2">
        <NButton data-test="save-new" size="sm" :loading="busy" @click="saveNew">{{ t('common.add') }}</NButton>
        <NButton size="sm" variant="secondary" @click="adding = false">{{ t('common.cancel') }}</NButton>
      </div>
    </div>

    <div v-else>
      <NButton data-test="add" variant="secondary" size="sm" @click="startAdd">{{ t('common.add') }}</NButton>
    </div>

    <NModal :model-value="removeId !== null" :title="t('common.delete')" @update:model-value="removeId = null">
      <p class="text-sm">{{ t('dashboard.removeConfirm') }}</p>
      <template #footer>
        <NButton variant="secondary" size="sm" @click="removeId = null">{{ t('common.cancel') }}</NButton>
        <NButton data-test="confirm-remove" size="sm" @click="() => { if (removeId !== null) emit('remove', removeId); removeId = null }">{{ t('common.delete') }}</NButton>
      </template>
    </NModal>
  </div>
</template>
