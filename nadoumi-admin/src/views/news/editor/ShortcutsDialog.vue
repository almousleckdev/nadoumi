<template>
  <el-dialog
    v-model="open"
    :title="t('news.editor.shortcuts.title')"
    width="480px"
    append-to-body
  >
    <p class="sc__intro">
      {{ t('news.editor.shortcuts.intro') }}
    </p>
    <dl class="sc__list">
      <template
        v-for="row in rows"
        :key="row.keys"
      >
        <dt><kbd>{{ row.keys }}</kbd></dt>
        <dd>{{ row.action }}</dd>
      </template>
    </dl>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const open = defineModel<boolean>({ required: true })
const { t } = useI18n()

const MOD = /Mac|iPhone|iPad/.test(navigator.platform) ? '⌘' : 'Ctrl'

const rows = computed(() => [
  { keys: '## + space', action: t('news.editor.toolbar.heading') },
  { keys: '### + space', action: t('news.editor.toolbar.subheading') },
  { keys: '> + space', action: t('news.editor.toolbar.quote') },
  { keys: '- + space', action: t('news.editor.toolbar.list') },
  { keys: '1. + space', action: t('news.editor.shortcuts.numbered') },
  { keys: '```', action: t('news.editor.insert.code') },
  { keys: '---', action: t('news.editor.insert.divider') },
  { keys: `${MOD} B`, action: t('news.editor.toolbar.bold') },
  { keys: `${MOD} I`, action: t('news.editor.toolbar.italic') },
  { keys: `${MOD} Z`, action: t('news.editor.shortcuts.undo') },
])
</script>

<style scoped>
.sc__intro {
  margin: 0 0 16px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.sc__list {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 10px 20px;
  margin: 0;
}
.sc__list dd {
  margin: 0;
  align-self: center;
}
kbd {
  padding: 2px 8px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: var(--el-fill-color-light);
  font: 12px/18px ui-monospace, SFMono-Regular, Menlo, monospace;
}
</style>
