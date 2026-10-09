<template>
  <NodeViewWrapper
    as="figure"
    class="nimg"
    :class="[`nimg--${size}`, { 'nimg--selected': selected }]"
    data-drag-handle
  >
    <div
      v-if="selected && editor.isEditable"
      class="nimg__bar"
      contenteditable="false"
    >
      <button
        v-for="option in sizes"
        :key="option.value"
        type="button"
        class="nimg__btn"
        :class="{ 'nimg__btn--on': size === option.value }"
        :aria-pressed="size === option.value"
        :aria-label="option.label"
        :title="option.label"
        @click="setSize(option.value)"
      >
        <svg
          viewBox="0 0 24 24"
          width="22"
          height="22"
          fill="currentColor"
          aria-hidden="true"
        >
          <rect
            :x="option.inset"
            y="6"
            :width="24 - option.inset * 2"
            height="9"
            rx="1"
          />
          <rect
            x="4"
            y="17.5"
            width="16"
            height="1.8"
            rx="0.9"
          />
        </svg>
      </button>
      <span
        class="nimg__sep"
        aria-hidden="true"
      />
      <button
        type="button"
        class="nimg__btn nimg__btn--text"
        @click="altOpen = true"
      >
        {{ t('news.editor.image.altText') }}
      </button>
    </div>

    <img
      class="nimg__img"
      :src="node.attrs.src"
      :alt="node.attrs.alt ?? ''"
      draggable="false"
    >

    <input
      class="nimg__caption"
      type="text"
      maxlength="300"
      :value="node.attrs.title ?? ''"
      :placeholder="t('news.editor.image.caption')"
      :readonly="!editor.isEditable"
      :aria-label="t('news.editor.image.captionLabel')"
      @input="onCaption"
      @keydown.enter.prevent="moveOn"
    >

    <AltTextDialog
      v-if="altOpen"
      :src="node.attrs.src"
      :alt="node.attrs.alt ?? ''"
      @save="saveAlt"
      @cancel="altOpen = false"
    />
  </NodeViewWrapper>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NodeViewWrapper, type NodeViewProps } from '@tiptap/vue-3'
import type { ImageSize } from './dialect'
import AltTextDialog from './AltTextDialog.vue'

const props = defineProps<NodeViewProps>()
const { t } = useI18n()

const altOpen = ref(false)
const size = computed(() => props.node.attrs.size as ImageSize)

// `inset` is how far the schematic picture is indented from the 24px icon edge: less indent, wider image.
const sizes = computed(() => [
  { value: 'inline' as const, label: t('news.editor.image.inline'), inset: 6 },
  { value: 'wide' as const, label: t('news.editor.image.wide'), inset: 3 },
  { value: 'full' as const, label: t('news.editor.image.full'), inset: 0 },
])

function setSize(value: ImageSize) {
  props.updateAttributes({ size: value })
}

function onCaption(event: Event) {
  props.updateAttributes({ title: (event.target as HTMLInputElement).value || null })
}

function saveAlt(alt: string) {
  props.updateAttributes({ alt })
  altOpen.value = false
}

/** Enter in the caption continues writing in the paragraph below the picture. */
function moveOn() {
  const position = props.getPos()
  if (typeof position !== 'number') return
  props.editor.chain().focus().setTextSelection(position + props.node.nodeSize).run()
}
</script>

<style scoped>
.nimg {
  position: relative;
  margin: 41px auto 0;
  padding: 0;
}
.nimg--wide {
  width: var(--ne-wide-width, min(1032px, calc(100vw - 32px)));
  inset-inline-start: 50%;
  transform: translateX(-50%);
}
.nimg--full {
  width: var(--ne-full-width, 100vw);
  inset-inline-start: 50%;
  transform: translateX(-50%);
}
.nimg__img {
  display: block;
  width: 100%;
  height: auto;
  box-sizing: border-box;
  border: 3px solid transparent;
  cursor: default;
}
.nimg--selected .nimg__img {
  border-color: var(--ne-accent);
}
.nimg__caption {
  display: block;
  width: 100%;
  margin-top: 10px;
  padding: 0 16px;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--ne-muted);
  font: 400 14px/20px var(--ne-sans);
  text-align: center;
}
.nimg__caption::placeholder {
  color: var(--ne-placeholder);
}
.nimg__bar {
  position: absolute;
  inset-block-start: -56px;
  inset-inline-start: 50%;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 2px;
  height: 44px;
  padding: 0 10px;
  border-radius: 4px;
  background: #242424;
  color: #fff;
  transform: translateX(-50%);
}
.nimg__bar::after {
  content: '';
  position: absolute;
  inset-inline-start: 50%;
  inset-block-start: 100%;
  margin-inline-start: -6px;
  border: 6px solid transparent;
  border-block-start-color: #242424;
}
.nimg__btn {
  display: grid;
  place-items: center;
  min-width: 36px;
  height: 34px;
  padding: 0 6px;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: #fff;
  cursor: pointer;
}
.nimg__btn:hover,
.nimg__btn:focus-visible {
  background: rgb(255 255 255 / 12%);
}
.nimg__btn--on {
  color: var(--ne-accent-on-dark);
}
.nimg__btn--text {
  padding: 0 10px;
  font: 400 14px/20px var(--ne-sans);
  white-space: nowrap;
}
.nimg__sep {
  width: 1px;
  height: 24px;
  margin: 0 6px;
  background: rgb(255 255 255 / 25%);
}
</style>
