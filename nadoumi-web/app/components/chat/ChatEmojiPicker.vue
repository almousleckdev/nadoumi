<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { EMOJI_GROUPS } from '~/constants/emoji'

/** A small emoji popover: category tabs and a grid of native characters. Esc or a pick closes it. */
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

<template>
  <div
    ref="root"
    class="w-72 rounded-xl border border-slate-200 bg-white p-2 shadow-lg"
    role="dialog"
    :aria-label="t('dashboard.messages.emoji')"
    data-test="emoji-picker"
  >
    <div class="mb-1 flex gap-1 border-b border-slate-100 pb-1" role="tablist">
      <button
        v-for="g in EMOJI_GROUPS"
        :key="g.key"
        type="button"
        role="tab"
        :aria-selected="g.key === groupKey"
        :aria-label="t(`dashboard.messages.emojiGroup.${g.key}`)"
        class="grid h-8 flex-1 place-items-center rounded-md text-lg hover:bg-slate-100"
        :class="g.key === groupKey ? 'bg-brand-50' : ''"
        @click="groupKey = g.key"
      >
        {{ g.icon }}
      </button>
    </div>
    <div class="grid max-h-44 grid-cols-8 gap-0.5 overflow-y-auto" role="tabpanel">
      <button
        v-for="(e, i) in group.emojis"
        :key="`${group.key}-${i}`"
        type="button"
        class="grid h-8 w-8 place-items-center rounded-md text-xl hover:bg-slate-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-500"
        @click="emit('pick', e)"
      >
        {{ e }}
      </button>
    </div>
  </div>
</template>
