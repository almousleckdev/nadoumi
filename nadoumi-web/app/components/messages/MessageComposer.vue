<script setup lang="ts">
import { MESSAGE_MAX_LENGTH } from '~/types/messages'

/** Textarea + send button. The parent owns the draft (v-model) and does the sending. */
const props = defineProps<{ id: string, busy?: boolean, disabled?: boolean }>()
const draft = defineModel<string>({ required: true })
const emit = defineEmits<{ submit: [] }>()
const { t } = useI18n()

const canSend = computed(() => !props.busy && !props.disabled && draft.value.trim().length > 0)

function submit() {
  if (canSend.value) emit('submit')
}
</script>

<template>
  <form class="grid gap-2" @submit.prevent="submit">
    <NTextarea
      :id="id"
      v-model="draft"
      :rows="3"
      :maxlength="MESSAGE_MAX_LENGTH"
      :disabled="disabled"
      :placeholder="t('dashboard.messages.composer')"
      @keydown.ctrl.enter.prevent="submit"
      @keydown.meta.enter.prevent="submit"
    />
    <div class="flex justify-end">
      <NButton type="submit" size="sm" :loading="busy" :disabled="!canSend">
        {{ t('dashboard.messages.send') }}
      </NButton>
    </div>
  </form>
</template>
