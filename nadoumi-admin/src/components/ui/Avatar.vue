<template>
  <span
    class="avatar"
    :style="{ width: `${size}px`, height: `${size}px`, fontSize: `${Math.round(size * 0.4)}px` }"
    :title="name || undefined"
    aria-hidden="true"
  >
    <img
      v-if="src"
      :src="src"
      :alt="name"
      class="avatar__img"
    >
    <template v-else>{{ initials }}</template>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  name?: string
  src?: string
  size?: number
}>(), { name: '', src: undefined, size: 32 })

const initials = computed(() => {
  const parts = props.name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '?'
  if (parts.length === 1) return parts[0]!.charAt(0).toUpperCase()
  return (parts[0]!.charAt(0) + parts[parts.length - 1]!.charAt(0)).toUpperCase()
})
</script>

<style scoped>
.avatar {
  display: inline-grid;
  place-items: center;
  border-radius: 999px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--nad-brand-500), var(--nad-brand-600));
  overflow: hidden;
  flex-shrink: 0;
  user-select: none;
}
.avatar__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
