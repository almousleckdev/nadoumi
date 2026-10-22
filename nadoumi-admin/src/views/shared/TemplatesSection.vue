<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Delete, Upload } from '@element-plus/icons-vue'
import {
  addTemplate, listTemplates, removeTemplate, TEMPLATE_ACCEPT, TEMPLATE_MAX_MB,
  type DocumentTemplate, type TemplateScope,
} from '@/api/documentTemplate'
import FormSection from '@/components/ui/FormSection.vue'

/**
 * The form templates of one programme or scholarship (for example the application form to fill in and send back).
 * It saves on its own, straight away, so it is available once the record exists.
 */
const props = defineProps<{ scope: TemplateScope, refId: number | null }>()

const { t } = useI18n()
const templates = ref<DocumentTemplate[]>([])
const title = ref('')
const busy = ref(false)

async function load() {
  templates.value = props.refId == null ? [] : await listTemplates(props.scope, props.refId).catch(() => [])
}

async function onFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || props.refId == null) return
  if (!title.value.trim()) {
    ElMessage.warning(t('template.titleRequired'))
    return
  }
  if (file.size > TEMPLATE_MAX_MB * 1024 * 1024) {
    ElMessage.warning(t('template.tooLarge', { mb: TEMPLATE_MAX_MB }))
    return
  }
  busy.value = true
  try {
    await addTemplate(props.scope, props.refId, title.value.trim(), file)
    title.value = ''
    ElMessage.success(t('common.saved'))
    await load()
  }
  finally {
    busy.value = false
  }
}

async function remove(template: DocumentTemplate) {
  await removeTemplate(template.id)
  await load()
}

watch(() => props.refId, load, { immediate: true })
defineExpose({ load })
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
    <template v-else>
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
        <label class="pick">
          <el-button
            :icon="Upload"
            :loading="busy"
            tag="span"
          >
            {{ t('template.upload') }}
          </el-button>
          <input
            type="file"
            :accept="TEMPLATE_ACCEPT"
            class="sr-only"
            :disabled="busy"
            @change="onFile"
          >
        </label>
      </div>
    </template>
  </FormSection>
</template>

<style scoped>
.list { list-style: none; margin: 0 0 12px; padding: 0; }
.row { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.name { display: grid; min-width: 0; }
.file { color: var(--el-text-color-secondary); font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.add { display: flex; gap: 8px; align-items: center; }
.title { flex: 1; }
.pick { cursor: pointer; }
.hint { color: var(--el-text-color-secondary); font-size: 13px; margin: 0; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
</style>
