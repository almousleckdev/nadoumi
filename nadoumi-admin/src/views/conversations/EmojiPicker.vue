<template>
  <div
    ref="root"
    class="ep"
    role="dialog"
    :aria-label="t('conversations.emoji')"
    data-test="emoji-picker"
  >
    <div
      class="ep__tabs"
      role="tablist"
    >
      <button
        v-for="g in EMOJI_GROUPS"
        :key="g.key"
        type="button"
        role="tab"
        class="ep__tab"
        :class="{ 'ep__tab--on': g.key === groupKey }"
        :aria-selected="g.key === groupKey"
        :aria-label="t(`conversations.emojiGroup.${g.key}`)"
        @click="groupKey = g.key"
      >
        {{ g.icon }}
      </button>
    </div>
    <div
      class="ep__grid"
      role="tabpanel"
    >
      <button
        v-for="(e, i) in group.emojis"
        :key="`${group.key}-${i}`"
        type="button"
        class="ep__emoji"
        @click="emit('pick', e)"
      >
        {{ e }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { EMOJI_GROUPS } from './emoji'

/** A small emoji popover: category tabs and a grid of native characters. Esc or a click outside closes it. */
const emit = defineEmits<{ pick: [emoji: string], close: [] }>()
const { t } = useI18n()
const groupKey = ref<(typeof EMOJI_GROUPS)[number]['key']>('smileys')
const group = computed(() => EMOJI_GROUPS.find(g => g.key === groupKey.value) ?? EMOJI_GROUPS[0]!)
const root = ref<HTMLElement | null>(null)

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
}
function onOutside(e: MouseEvent) {
  if (root.value && !root.value.contains(e.target as Node)) emit('close')
}
onMounted(() => {
  document.addEventListener('keydown', onKey)
  document.addEventListener('mousedown', onOutside)
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey)
  document.removeEventListener('mousedown', onOutside)
})
</script>

<style scoped>
.ep { width: 288px; padding: 8px; border: 1px solid var(--nad-line); border-radius: 12px; background: #fff; box-shadow: var(--nad-shadow-md); }
.ep__tabs { display: flex; gap: 4px; margin-bottom: 4px; padding-bottom: 4px; border-bottom: 1px solid #f1f5f9; }
.ep__tab { flex: 1; height: 32px; border: 0; border-radius: 6px; background: transparent; font-size: 18px; cursor: pointer; }
.ep__tab:hover { background: #f1f5f9; }
.ep__tab--on { background: var(--nad-brand-50); }
.ep__grid { display: grid; grid-template-columns: repeat(8, 1fr); gap: 2px; max-height: 176px; overflow-y: auto; }
.ep__emoji { height: 32px; border: 0; border-radius: 6px; background: transparent; font-size: 20px; cursor: pointer; }
.ep__emoji:hover { background: #f1f5f9; }
.ep__emoji:focus-visible, .ep__tab:focus-visible { outline: 2px solid var(--nad-brand-500); }
</style>
