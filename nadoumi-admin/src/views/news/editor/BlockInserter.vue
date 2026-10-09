<template>
  <div
    v-show="visible"
    ref="root"
    class="ins"
    :style="{ top: `${top}px` }"
    @mousedown="keepSelection"
  >
    <button
      type="button"
      class="ins__toggle"
      :class="{ 'ins__toggle--open': open }"
      :aria-expanded="open"
      :aria-label="open ? t('news.editor.insert.close') : t('news.editor.insert.open')"
      @click="toggle"
    >
      <svg
        viewBox="0 0 32 32"
        width="32"
        height="32"
        fill="none"
        stroke="currentColor"
        stroke-width="1.2"
        aria-hidden="true"
      >
        <circle
          cx="16"
          cy="16"
          r="15"
        />
        <path d="M9 16h14M16 9v14" />
      </svg>
    </button>

    <div
      v-if="open && mode === 'menu'"
      class="ins__items"
    >
      <button
        v-for="(item, i) in items"
        :key="item.key"
        type="button"
        class="ins__item"
        :style="{ transitionDelay: `${i * 30}ms` }"
        :aria-label="item.label"
        :title="item.label"
        @click="item.run()"
      >
        <svg
          viewBox="0 0 24 24"
          width="18"
          height="18"
          fill="none"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path
            v-for="d in item.icon"
            :key="d"
            :d="d"
          />
        </svg>
      </button>
    </div>

    <div
      v-if="open && mode === 'video'"
      class="ins__video"
    >
      <input
        ref="videoInput"
        v-model="videoUrl"
        class="ins__field"
        type="url"
        inputmode="url"
        autocomplete="off"
        :placeholder="t('news.editor.insert.videoPlaceholder')"
        :aria-label="t('news.editor.insert.video')"
        :aria-invalid="videoInvalid"
        @keydown.enter.prevent="insertVideo"
        @keydown.esc.prevent="close(true)"
      >
      <p
        v-if="videoInvalid"
        class="ins__error"
        role="alert"
      >
        {{ t('news.editor.insert.videoInvalid') }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Editor } from '@tiptap/core'
import { embedFromUrl } from './dialect'

/**
 * The "+" in the left gutter beside an empty line. Image upload is delegated to the page (it needs a
 * saved draft); video, code and divider are plain editor commands.
 */
const props = defineProps<{ editor: Editor }>()
const emit = defineEmits<{ image: [] }>()

const { t } = useI18n()

const visible = ref(false)
const top = ref(0)
const open = ref(false)
const mode = ref<'menu' | 'video'>('menu')
const videoUrl = ref('')
const videoInvalid = ref(false)
const root = ref<HTMLElement>()
const videoInput = ref<HTMLInputElement>()

const TOGGLE_RADIUS = 16

// Icons are plain path data (24x24 grid), rendered as <path> elements.
const ICON_IMAGE = [
  'M5 4h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z',
  'M9 8.4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2z',
  'm21 16-5-5-9 9',
]
const ICON_VIDEO = ['M5 4h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z', 'm10 9 5 3-5 3z']
const ICON_CODE = ['m8 8-5 4 5 4', 'm16 8 5 4-5 4', 'm13.5 5-3 14']
const ICON_DIVIDER = ['M4 12h2', 'M11 12h2', 'M18 12h2']

const items = computed(() => [
  { key: 'image', icon: ICON_IMAGE, label: t('news.editor.insert.image'), run: () => { close(false); emit('image') } },
  { key: 'video', icon: ICON_VIDEO, label: t('news.editor.insert.video'), run: openVideo },
  { key: 'code', icon: ICON_CODE, label: t('news.editor.insert.code'), run: () => run(c => c.setCodeBlock()) },
  { key: 'divider', icon: ICON_DIVIDER, label: t('news.editor.insert.divider'), run: () => run(c => c.setHorizontalRule()) },
])

function run(command: (chain: ReturnType<Editor['chain']>) => ReturnType<Editor['chain']>) {
  command(props.editor.chain().focus()).run()
  close(false)
}

function keepSelection(event: MouseEvent) {
  if ((event.target as HTMLElement).tagName !== 'INPUT') event.preventDefault()
}

function toggle() {
  if (open.value) close(true)
  else open.value = true
}

function close(refocus: boolean) {
  open.value = false
  mode.value = 'menu'
  videoUrl.value = ''
  videoInvalid.value = false
  if (refocus) props.editor.chain().focus().run()
}

function openVideo() {
  mode.value = 'video'
  void nextTick(() => videoInput.value?.focus())
}

function insertVideo() {
  const embed = embedFromUrl(videoUrl.value)
  if (!embed) {
    videoInvalid.value = true
    return
  }
  props.editor.chain().focus()
    .insertContent([{ type: 'embed', attrs: { url: embed.url } }, { type: 'paragraph' }])
    .run()
  close(false)
}

function update() {
  const { selection } = props.editor.state
  const { $from } = selection
  const onEmptyLine = selection.empty
    && $from.depth === 1
    && $from.parent.type.name === 'paragraph'
    && $from.parent.content.size === 0
  const keep = open.value || props.editor.view.hasFocus()
  if (!onEmptyLine || !props.editor.isEditable || !keep) {
    if (visible.value && !open.value) visible.value = false
    if (!onEmptyLine && open.value) close(false)
    return
  }
  const host = root.value?.parentElement
  if (!host) return
  const line = props.editor.view.coordsAtPos($from.pos)
  top.value = (line.top + line.bottom) / 2 - host.getBoundingClientRect().top - TOGGLE_RADIUS
  visible.value = true
}

function onBlur({ event }: { event: FocusEvent }) {
  if (open.value) return
  if (root.value && event.relatedTarget instanceof Node && root.value.contains(event.relatedTarget)) return
  visible.value = false
}

watch(open, (isOpen) => { if (!isOpen) update() })

onMounted(() => {
  props.editor.on('transaction', update)
  props.editor.on('focus', update)
  props.editor.on('blur', onBlur)
})
onBeforeUnmount(() => {
  props.editor.off('transaction', update)
  props.editor.off('focus', update)
  props.editor.off('blur', onBlur)
})
</script>

<style scoped>
.ins {
  position: absolute;
  inset-inline-start: -75px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 32px;
}
.ins__toggle,
.ins__item {
  display: grid;
  place-items: center;
  flex: none;
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 999px;
  background: var(--ne-paper);
  cursor: pointer;
}
.ins__toggle {
  border: 0;
  color: var(--ne-ink);
  opacity: 0.75;
  transition: transform 180ms ease, opacity 150ms ease;
}
.ins__toggle:hover,
.ins__toggle:focus-visible {
  opacity: 1;
}
.ins__toggle--open {
  transform: rotate(45deg);
  opacity: 1;
}
.ins__items {
  display: flex;
  gap: 12px;
}
.ins__item {
  border: 1px solid var(--ne-accent);
  color: var(--ne-accent);
  animation: ins-pop 180ms ease both;
}
.ins__item:hover,
.ins__item:focus-visible {
  background: var(--ne-accent);
  color: #fff;
}
.ins__video {
  position: absolute;
  inset-inline-start: 75px;
  inset-block-start: -2px;
  width: min(680px, calc(100vw - 120px));
}
.ins__field {
  width: 100%;
  padding: 0;
  border: 0;
  outline: 0;
  background: var(--ne-paper);
  color: var(--ne-ink);
  font: var(--ne-body-font);
}
.ins__field::placeholder {
  color: var(--ne-placeholder);
}
.ins__error {
  margin: 4px 0 0;
  color: #c94a4a;
  font: 400 13px/18px var(--ne-sans);
}

@keyframes ins-pop {
  from {
    opacity: 0;
    transform: translateX(-10px) scale(0.85);
  }
}
@media (prefers-reduced-motion: reduce) {
  .ins__item {
    animation: none;
  }
  .ins__toggle {
    transition: none;
  }
}
</style>
