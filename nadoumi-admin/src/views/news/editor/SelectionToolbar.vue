<template>
  <div
    v-show="visible"
    ref="root"
    class="tb"
    :class="{ 'tb--link': mode === 'link' }"
    :style="{ left: `${left}px`, top: `${top}px` }"
    role="toolbar"
    :aria-label="t('news.editor.toolbar.label')"
    @mousedown="keepSelection"
  >
    <template v-if="mode === 'format'">
      <button
        v-for="b in marks"
        :key="b.key"
        type="button"
        class="tb__btn"
        :class="[b.cls, { 'tb__btn--on': isOn(b.key, b.attrs) }]"
        :aria-pressed="isOn(b.key, b.attrs)"
        :aria-label="b.label"
        :title="b.label"
        @click="b.run()"
      >
        {{ b.glyph }}
      </button>
      <button
        type="button"
        class="tb__btn"
        :class="{ 'tb__btn--on': isOn('link') }"
        :aria-pressed="isOn('link')"
        :aria-label="t('news.editor.toolbar.link')"
        :title="t('news.editor.toolbar.link')"
        @click="openLink"
      >
        <svg
          viewBox="0 0 24 24"
          width="18"
          height="18"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M10 13a5 5 0 0 0 7.07 0l3-3a5 5 0 0 0-7.07-7.07l-1.5 1.5" />
          <path d="M14 11a5 5 0 0 0-7.07 0l-3 3a5 5 0 0 0 7.07 7.07l1.5-1.5" />
        </svg>
      </button>
      <span
        class="tb__sep"
        aria-hidden="true"
      />
      <button
        v-for="b in blocks"
        :key="b.key"
        type="button"
        class="tb__btn"
        :class="[b.cls, { 'tb__btn--on': isOn(b.key, b.attrs) }]"
        :aria-pressed="isOn(b.key, b.attrs)"
        :aria-label="b.label"
        :title="b.label"
        @click="b.run()"
      >
        {{ b.glyph }}
      </button>
    </template>

    <form
      v-else
      class="tb__link"
      @submit.prevent="applyLink"
    >
      <input
        ref="linkInput"
        v-model="linkValue"
        class="tb__input"
        type="text"
        inputmode="url"
        autocomplete="off"
        :placeholder="t('news.editor.toolbar.linkPlaceholder')"
        :aria-label="t('news.editor.toolbar.link')"
        :aria-invalid="linkInvalid"
        @keydown.esc.prevent="closeLink"
      >
      <button
        v-if="isOn('link')"
        type="button"
        class="tb__text"
        @click="removeLink"
      >
        {{ t('news.editor.toolbar.removeLink') }}
      </button>
      <span
        v-if="linkInvalid"
        class="tb__error"
        role="alert"
      >{{ t('news.editor.toolbar.linkInvalid') }}</span>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Editor } from '@tiptap/core'
import { NodeSelection } from '@tiptap/pm/state'
import { normalizeLink } from './dialect'

/** Medium-style floating toolbar shown over a text selection. Positioned inside its `position: relative` parent. */
const props = defineProps<{ editor: Editor }>()

const { t } = useI18n()

interface ToolbarButton {
  key: string
  attrs?: Record<string, unknown>
  glyph: string
  label: string
  cls: string
  run: () => void
}

const visible = ref(false)
const left = ref(0)
const top = ref(0)
const mode = ref<'format' | 'link'>('format')
const linkValue = ref('')
const linkInvalid = ref(false)
// The selection the link field was opened for. The field only lives as long as that selection does.
let linkAnchor: { from: number, to: number } | null = null
const root = ref<HTMLElement>()
const linkInput = ref<HTMLInputElement>()
// bumped on every transaction so active states re-evaluate
const tick = ref(0)

const TOOLBAR_HALF_WIDTH = 150
const GAP_ABOVE_SELECTION = 12

const chain = () => props.editor.chain().focus()

const marks = computed<ToolbarButton[]>(() => [
  { key: 'bold', glyph: 'B', cls: 'tb__btn--bold', label: t('news.editor.toolbar.bold'), run: () => chain().toggleBold().run() },
  { key: 'italic', glyph: 'i', cls: 'tb__btn--italic', label: t('news.editor.toolbar.italic'), run: () => chain().toggleItalic().run() },
])

const blocks = computed<ToolbarButton[]>(() => [
  {
    key: 'heading', attrs: { level: 2 }, glyph: 'T', cls: 'tb__btn--h2', label: t('news.editor.toolbar.heading'),
    run: () => chain().toggleHeading({ level: 2 }).run(),
  },
  {
    key: 'heading', attrs: { level: 3 }, glyph: 'T', cls: 'tb__btn--h3', label: t('news.editor.toolbar.subheading'),
    run: () => chain().toggleHeading({ level: 3 }).run(),
  },
  { key: 'blockquote', glyph: '“', cls: 'tb__btn--quote', label: t('news.editor.toolbar.quote'), run: () => chain().toggleBlockquote().run() },
  { key: 'bulletList', glyph: '•', cls: 'tb__btn--list', label: t('news.editor.toolbar.list'), run: () => chain().toggleBulletList().run() },
])

function isOn(name: string, attrs?: Record<string, unknown>): boolean {
  void tick.value
  return props.editor.isActive(name, attrs)
}

// Buttons must not steal focus from the editor, or the selection (and this toolbar) would vanish.
// The link field is the one place that needs real focus.
function keepSelection(event: MouseEvent) {
  if ((event.target as HTMLElement).tagName !== 'INPUT') event.preventDefault()
}

function selectionBox(): { x: number, top: number } | null {
  const range = window.getSelection()?.rangeCount ? window.getSelection()!.getRangeAt(0) : null
  const rect = range?.getBoundingClientRect()
  if (rect && (rect.width > 0 || rect.height > 0)) return { x: rect.left + rect.width / 2, top: rect.top }
  const { from, to } = props.editor.state.selection
  const a = props.editor.view.coordsAtPos(from)
  const b = props.editor.view.coordsAtPos(to)
  return { x: (a.left + b.right) / 2, top: Math.min(a.top, b.top) }
}

function update() {
  const { selection } = props.editor.state
  const typing = mode.value === 'format'
  const showable = !selection.empty
    && !(selection instanceof NodeSelection)
    && props.editor.isEditable
    && !props.editor.isActive('codeBlock')
    && (props.editor.view.hasFocus() || !typing)
  if (!showable) {
    visible.value = false
    if (typing) linkInvalid.value = false
    return
  }
  const host = root.value?.parentElement
  const box = selectionBox()
  if (!host || !box) return
  const hostRect = host.getBoundingClientRect()
  left.value = Math.min(Math.max(box.x - hostRect.left, TOOLBAR_HALF_WIDTH), hostRect.width - TOOLBAR_HALF_WIDTH)
  top.value = box.top - hostRect.top - GAP_ABOVE_SELECTION
  visible.value = true
}

function onTransaction() {
  tick.value++
  if (mode.value === 'link' && !isSameSelection(linkAnchor)) leaveLinkMode()
  update()
}

function isSameSelection(anchor: { from: number, to: number } | null): boolean {
  const { from, to } = props.editor.state.selection
  return anchor !== null && anchor.from === from && anchor.to === to
}

/** Back to the formatting buttons, without touching the editor's focus or selection. */
function leaveLinkMode() {
  mode.value = 'format'
  linkAnchor = null
  linkInvalid.value = false
}

function onBlur({ event }: { event: FocusEvent }) {
  // moving focus into the link field keeps the toolbar; leaving for anywhere else closes it
  if (root.value && event.relatedTarget instanceof Node && root.value.contains(event.relatedTarget)) return
  leaveLinkMode()
  visible.value = false
}

function openLink() {
  linkValue.value = (props.editor.getAttributes('link').href as string | undefined) ?? ''
  linkInvalid.value = false
  const { from, to } = props.editor.state.selection
  linkAnchor = { from, to }
  mode.value = 'link'
  void nextTick(() => linkInput.value?.focus())
}

function closeLink() {
  leaveLinkMode()
  props.editor.chain().focus().run()
}

function applyLink() {
  const href = normalizeLink(linkValue.value)
  if (!href) {
    linkInvalid.value = true
    return
  }
  leaveLinkMode()
  props.editor.chain().focus().extendMarkRange('link').setLink({ href }).run()
}

function removeLink() {
  leaveLinkMode()
  props.editor.chain().focus().extendMarkRange('link').unsetLink().run()
}

onMounted(() => {
  props.editor.on('transaction', onTransaction)
  props.editor.on('blur', onBlur)
})
onBeforeUnmount(() => {
  props.editor.off('transaction', onTransaction)
  props.editor.off('blur', onBlur)
})
</script>

<style scoped>
.tb {
  position: absolute;
  z-index: 15;
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 0 8px;
  height: 44px;
  border-radius: 4px;
  background: #242424;
  color: #fff;
  transform: translate(-50%, -100%);
  box-shadow: 0 2px 8px rgb(0 0 0 / 24%);
  font-family: var(--ne-sans);
}
.tb::after {
  content: '';
  position: absolute;
  inset-inline-start: 50%;
  inset-block-start: 100%;
  margin-inline-start: -6px;
  border: 6px solid transparent;
  border-block-start-color: #242424;
}
.tb__btn {
  display: grid;
  place-items: center;
  min-width: 34px;
  height: 34px;
  padding: 0 6px;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: #fff;
  font: 400 18px/1 var(--ne-serif);
  cursor: pointer;
}
.tb__btn:hover,
.tb__btn:focus-visible {
  background: rgb(255 255 255 / 12%);
}
.tb__btn--on {
  color: var(--ne-accent-on-dark);
}
.tb__btn--bold {
  font-weight: 800;
}
.tb__btn--italic {
  font-style: italic;
}
.tb__btn--h2 {
  font-size: 22px;
  font-weight: 700;
}
.tb__btn--h3 {
  font-size: 16px;
  font-weight: 700;
}
.tb__btn--quote {
  font-size: 26px;
  line-height: 1;
}
.tb__btn--list {
  font-size: 26px;
  font-weight: 700;
}
.tb__sep {
  width: 1px;
  height: 24px;
  margin: 0 6px;
  background: rgb(255 255 255 / 25%);
}
.tb--link {
  padding: 0 12px;
}
.tb__link {
  display: flex;
  align-items: center;
  gap: 12px;
}
.tb__input {
  width: 260px;
  border: 0;
  outline: 0;
  background: transparent;
  color: #fff;
  font: 400 14px/20px var(--ne-sans);
}
.tb__input::placeholder {
  color: rgb(255 255 255 / 55%);
}
.tb__text {
  border: 0;
  background: transparent;
  color: var(--ne-accent-on-dark);
  font: 400 13px/1 var(--ne-sans);
  cursor: pointer;
  white-space: nowrap;
}
.tb__error {
  position: absolute;
  inset-block-start: calc(100% + 10px);
  inset-inline-start: 0;
  padding: 4px 8px;
  border-radius: 3px;
  background: #c94a4a;
  color: #fff;
  font: 400 12px/16px var(--ne-sans);
  white-space: nowrap;
}
</style>
