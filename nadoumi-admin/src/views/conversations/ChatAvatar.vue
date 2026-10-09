<template>
  <span class="ca">
    <Avatar
      :name="name ?? ''"
      :src="src ?? undefined"
      :size="size"
    />
    <span
      v-if="online !== null"
      class="ca__dot"
      :class="online ? 'ca__dot--on' : ''"
      data-test="presence-dot"
    />
  </span>
</template>

<script setup lang="ts">
import Avatar from '@/components/ui/Avatar.vue'

/** A person's photo (or initials) with an optional presence dot. `online: null` hides the dot. */
withDefaults(defineProps<{
  name?: string | null
  src?: string | null
  size?: number
  online?: boolean | null
}>(), { name: null, src: null, size: 40, online: null })
</script>

<style scoped>
.ca { position: relative; display: inline-flex; flex-shrink: 0; }
.ca__dot {
  position: absolute;
  inset-block-end: 0;
  inset-inline-end: 0;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #cbd5e1;
  box-shadow: 0 0 0 2px #fff;
}
.ca__dot--on { background: #10b981; }
</style>
