<script setup lang="ts">
const props = withDefaults(defineProps<{
  tone?: 'info' | 'success' | 'warning' | 'danger'
  title?: string
  dismissible?: boolean
}>(), { tone: 'info', title: undefined })
defineEmits<{ dismiss: [] }>()

const role = computed(() => (props.tone === 'warning' || props.tone === 'danger' ? 'alert' : 'status'))
const tones = {
  info: 'bg-slate-50 border-slate-200 text-slate-800',
  success: 'bg-green-50 border-green-200 text-green-800',
  warning: 'bg-amber-50 border-amber-200 text-amber-900',
  danger: 'bg-red-50 border-red-200 text-red-800',
}
</script>

<template>
  <div :role="role" class="flex gap-3 rounded-md border px-4 py-3 text-sm" :class="tones[tone]">
    <div class="flex-1">
      <p v-if="title" class="font-semibold">{{ title }}</p>
      <div><slot /></div>
    </div>
    <button v-if="dismissible" type="button" aria-label="Dismiss" class="shrink-0 opacity-70 hover:opacity-100" @click="$emit('dismiss')">×</button>
  </div>
</template>
