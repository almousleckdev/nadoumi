<script setup lang="ts" generic="T extends { id: number }">
/**
 * A list of records the student can add to, edit and remove (education, work, contacts).
 * The parent supplies how a record looks (`summary`) and its editor (`form`); this owns which
 * record is being edited and the remove confirmation, so no section repeats that.
 */
defineProps<{ items: T[], empty: string, addLabel: string }>()
const emit = defineEmits<{ remove: [id: number] }>()
const { t } = useI18n()

const NEW_RECORD = 'new'
const editing = ref<number | typeof NEW_RECORD | null>(null)
const removing = ref<number | null>(null)

const close = () => { editing.value = null }

function confirmRemove() {
  if (removing.value !== null) emit('remove', removing.value)
  removing.value = null
}
</script>

<template>
  <div class="grid gap-4">
    <p v-if="!items.length && editing === null" class="rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500">
      {{ empty }}
    </p>

    <ul v-if="items.length" class="grid gap-3">
      <li v-for="item in items" :key="item.id" class="rounded-lg border border-slate-200 bg-white p-4">
        <div v-if="editing !== item.id" class="flex items-start justify-between gap-4">
          <div class="min-w-0"><slot name="summary" :item="item" /></div>
          <div class="flex shrink-0 gap-3 text-sm">
            <button type="button" class="font-medium text-brand-700 hover:underline" :data-test="`edit-${item.id}`" @click="editing = item.id">{{ t('common.edit') }}</button>
            <button type="button" class="font-medium text-red-600 hover:underline" :data-test="`remove-${item.id}`" @click="removing = item.id">{{ t('common.remove') }}</button>
          </div>
        </div>
        <slot v-else name="form" :item="item" :close="close" />
      </li>
    </ul>

    <div v-if="editing === NEW_RECORD" class="rounded-lg border border-slate-200 bg-slate-50 p-4">
      <slot name="form" :item="null" :close="close" />
    </div>
    <div v-else-if="editing === null">
      <NButton variant="secondary" size="sm" data-test="add" @click="editing = NEW_RECORD">{{ addLabel }}</NButton>
    </div>

    <NModal :model-value="removing !== null" :title="t('common.delete')" @update:model-value="removing = null">
      <p class="text-sm">{{ t('dashboard.removeConfirm') }}</p>
      <template #footer>
        <NButton variant="secondary" size="sm" @click="removing = null">{{ t('common.cancel') }}</NButton>
        <NButton size="sm" data-test="confirm-remove" @click="confirmRemove">{{ t('common.delete') }}</NButton>
      </template>
    </NModal>
  </div>
</template>
