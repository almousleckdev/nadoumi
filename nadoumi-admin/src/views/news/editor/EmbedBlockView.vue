<template>
  <NodeViewWrapper
    class="nemb"
    :class="{ 'nemb--selected': selected }"
    data-drag-handle
  >
    <div
      v-if="embed"
      class="nemb__frame"
      contenteditable="false"
    >
      <iframe
        class="nemb__iframe"
        :class="{ 'nemb__iframe--live': selected }"
        :src="embed.src"
        :title="t('news.editor.embed.title')"
        loading="lazy"
        referrerpolicy="strict-origin-when-cross-origin"
        sandbox="allow-scripts allow-same-origin allow-presentation allow-popups"
        allow="fullscreen; picture-in-picture"
        allowfullscreen
      />
    </div>
    <p
      v-else
      class="nemb__fallback"
      contenteditable="false"
    >
      {{ node.attrs.url }}
    </p>
  </NodeViewWrapper>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NodeViewWrapper, type NodeViewProps } from '@tiptap/vue-3'
import { embedFromUrl } from './dialect'

const props = defineProps<NodeViewProps>()
const { t } = useI18n()

// The iframe source is rebuilt from the allow-listed url every time, never read from the document.
const embed = computed(() => embedFromUrl(String(props.node.attrs.url ?? '')))
</script>

<style scoped>
.nemb {
  margin: 41px auto 0;
}
.nemb--selected {
  outline: 3px solid var(--ne-accent);
}
.nemb__frame {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #000;
}
.nemb__iframe {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: 0;
  /* the first click selects the block; only a selected video receives clicks */
  pointer-events: none;
}
.nemb__iframe--live {
  pointer-events: auto;
}
.nemb__fallback {
  margin: 0;
  padding: 12px;
  color: var(--ne-muted);
  font: 400 14px/20px var(--ne-sans);
  word-break: break-all;
}
</style>
