<template>
  <LoadingState
    v-if="loading"
    :rows="rows"
  />
  <ErrorState
    v-else-if="error"
    :message="error"
    @retry="emit('retry')"
  />
  <EmptyState
    v-else-if="empty"
    :title="emptyTitle"
    :description="emptyDescription"
    :icon="emptyIcon"
  >
    <template
      v-if="$slots.action"
      #action
    >
      <slot name="action" />
    </template>
  </EmptyState>
  <slot v-else />
</template>

<script setup lang="ts">
import LoadingState from './LoadingState.vue'
import ErrorState from './ErrorState.vue'
import EmptyState from './EmptyState.vue'

withDefaults(defineProps<{
  loading: boolean
  error: string | null
  empty: boolean
  emptyTitle: string
  emptyDescription?: string
  emptyIcon?: string
  rows?: number
}>(), { emptyDescription: undefined, emptyIcon: 'Document', rows: 4 })

const emit = defineEmits<{ retry: [] }>()
</script>
