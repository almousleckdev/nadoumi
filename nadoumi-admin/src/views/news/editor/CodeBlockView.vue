<template>
  <NodeViewWrapper class="ncode">
    <div
      class="ncode__head"
      contenteditable="false"
    >
      <select
        class="ncode__lang"
        :value="node.attrs.language ?? ''"
        :disabled="!editor.isEditable"
        :aria-label="t('news.editor.code.language')"
        @change="onLanguage"
      >
        <option value="">
          {{ t('news.editor.code.none') }}
        </option>
        <option
          v-for="language in CODE_LANGUAGES"
          :key="language.value"
          :value="language.value"
        >
          {{ language.label }}
        </option>
      </select>
    </div>
    <pre class="ncode__pre"><NodeViewContent as="code" /></pre>
  </NodeViewWrapper>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { NodeViewContent, NodeViewWrapper, type NodeViewProps } from '@tiptap/vue-3'
import { CODE_LANGUAGES } from './codeLanguages'

const props = defineProps<NodeViewProps>()
const { t } = useI18n()

function onLanguage(event: Event) {
  props.updateAttributes({ language: (event.target as HTMLSelectElement).value || null })
}
</script>

<style scoped>
.ncode {
  margin-top: 29px;
  padding: 10px 16px 14px;
  border: 1px solid #242424;
  border-radius: 4px;
  background: #fafafa;
}
.ncode__lang {
  max-width: 100%;
  padding: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: #242424;
  font: 400 12px/18px var(--ne-mono);
  cursor: pointer;
}
.ncode__lang:focus-visible {
  outline: 2px solid var(--ne-accent);
  outline-offset: 2px;
}
.ncode__pre {
  margin: 8px 0 0;
  overflow-x: auto;
  color: #242424;
  font: 400 15px/24px var(--ne-mono);
  tab-size: 2;
}
.ncode__pre :deep(code) {
  font: inherit;
  white-space: pre;
}
</style>
