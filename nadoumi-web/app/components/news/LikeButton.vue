<script setup lang="ts">
withDefaults(defineProps<{
  liked: boolean
  count: number
  label: string
  size?: 'sm' | 'md'
  /** False when the count is shown elsewhere (the article bar makes it a separate, clickable control). */
  showCount?: boolean
}>(), { size: 'md', showCount: true })
defineEmits<{ toggle: [] }>()

const { n } = useI18n()
</script>

<template>
  <button
    type="button"
    :aria-pressed="liked"
    :aria-label="label"
    :title="label"
    class="inline-flex items-center gap-1.5 rounded-full transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
    :class="[
      size === 'sm' ? 'px-1.5 py-1 text-sm' : 'px-2 py-1.5 text-base',
      liked ? 'text-rose-600' : 'text-slate-500 hover:text-rose-600',
    ]"
    @click="$emit('toggle')"
  >
    <svg
      viewBox="0 0 24 24"
      :class="size === 'sm' ? 'h-4 w-4' : 'h-5 w-5'"
      :fill="liked ? 'currentColor' : 'none'"
      stroke="currentColor"
      stroke-width="1.8"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <path d="M12 20.5s-7.5-4.6-9.2-9.4C1.7 7.8 3.6 5 6.5 5c1.9 0 3.5 1 4.5 2.6C12 6 13.6 5 15.5 5c2.9 0 4.8 2.8 3.7 6.1C19.5 15.9 12 20.5 12 20.5z" />
    </svg>
    <span v-if="showCount && count > 0" class="tabular-nums">{{ n(count) }}</span>
  </button>
</template>
