<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Delete, Upload } from '@element-plus/icons-vue'
import {
  addTemplate, listTemplates, removeTemplate, TEMPLATE_ACCEPT, TEMPLATE_MAX_MB, withKnownType,
  type DocumentTemplate, type TemplateScope,
} from '@/api/documentTemplate'
import FormSection from '@/components/ui/FormSection.vue'

/**
 * The form templates of one programme or scholarship (for example the application form to fill in and send back).
 * Once the record exists a file is saved straight away. On a new record the files are held here and uploaded by the
 * drawer through `flush(id)` right after the record is created.
 */
const props = defineProps<{ scope: TemplateScope, refId: number | null }>()

const { t } = useI18n()
const templates = ref<DocumentTemplate[]>([])
const filePicker = ref<HTMLInputElement>()
const staged = ref<{ title: string, file: File }[]>([])
const title = ref('')
const busy = ref(false)
const canPick = computed(() => !busy.value && title.value.trim().length > 0)

async function load() {
  templates.value = props.refId == null ? [] : await listTemplates(props.scope, props.refId).catch(() => [])
}

async function onFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !title.value.trim()) return
  if (file.size > TEMPLATE_MAX_MB * 1024 * 1024) {
    ElMessage.warning(t('template.tooLarge', { mb: TEMPLATE_MAX_MB }))
    return
  }
  if (props.refId == null) {
    staged.value = [...staged.value, { title: title.value.trim(), file: withKnownType(file) }]
    title.value = ''
    return
  }
  busy.value = true
  try {
    await addTemplate(props.scope, props.refId, title.value.trim(), withKnownType(file))
    title.value = ''
    ElMessage.success(t('common.saved'))
    await load()
  }
  finally {
    busy.value = false
  }
}

function unstage(index: number) {
  staged.value = staged.value.filter((_, i) => i !== index)
}

/** Uploads the files held for a new record; true when every one was saved. */
async function flush(refId: number): Promise<boolean> {
  const pending = staged.value
  let allSaved = true
  for (const item of pending) {
    try {
      await addTemplate(props.scope, refId, item.title, item.file)
      staged.value = staged.value.filter(s => s !== item)
    }
    catch {
      allSaved = false
    }
  }
  return allSaved
}

async function remove(template: DocumentTemplate) {
  await removeTemplate(template.id)
  await load()
}

watch(() => props.refId, load, { immediate: true })
defineExpose({ load, flush })
</script>

<template>
  <FormSection
    :title="t('template.title')"
    :description="t('template.hint')"
  >
    <p
      v-if="refId == null"
      class="hint"
    >
      {{ t('template.saveFirst') }}
    </p>
    <div>
      <ul
        v-if="staged.length"
        class="list"
      >
        <li
          v-for="(item, index) in staged"
          :key="`${item.title}-${index}`"
          class="row"
        >
          <span class="name">
            <strong>{{ item.title }}</strong>
            <span class="file">{{ item.file.name }}</span>
          </span>
          <el-button
            link
            type="danger"
            :icon="Delete"
            :aria-label="t('common.delete')"
            @click="unstage(index)"
          />
        </li>
      </ul>
      <ul
        v-if="templates.length"
        class="list"
      >
        <li
          v-for="tpl in templates"
          :key="tpl.id"
          class="row"
        >
          <span class="name">
            <strong>{{ tpl.title }}</strong>
            <span class="file">{{ tpl.fileName }}</span>
          </span>
          <el-button
            link
            type="danger"
            :icon="Delete"
            :aria-label="t('common.delete')"
            @click="remove(tpl)"
          />
        </li>
      </ul>
      <div class="add">
        <el-input
          v-model="title"
          maxlength="120"
          :placeholder="t('template.titlePlaceholder')"
          class="title"
        />
        <!-- a hidden input opened from the button: an off-screen visible input makes the browser scroll the drawer when it takes focus -->
        <input
          ref="filePicker"
          type="file"
          :accept="TEMPLATE_ACCEPT"
          hidden
          @change="onFile"
        >
        <el-button
          :icon="Upload"
          :loading="busy"
          :disabled="!canPick"
          :title="canPick ? undefined : t('template.titleRequired')"
          @click="filePicker?.click()"
        >
          {{ t('template.upload') }}
        </el-button>
      </div>
    </div>
  </FormSection>
</template>

<style scoped>
.list { list-style: none; margin: 0 0 12px; padding: 0; }
.row { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.name { display: grid; min-width: 0; }
.file { color: var(--el-text-color-secondary); font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.add { display: flex; gap: 8px; align-items: center; }
.title { flex: 1; }
.hint { color: var(--el-text-color-secondary); font-size: 13px; margin: 0; }
</style>
