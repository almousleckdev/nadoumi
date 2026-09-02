<template>
  <LoadingState
    v-if="loading"
    :rows="4"
  />
  <ErrorState
    v-else-if="error"
    :message="error"
    @retry="emit('retry')"
  />
  <EmptyState
    v-else-if="empty"
    :title="emptyTitle"
    icon="Document"
  />
  <slot v-else />
</template>

<script setup lang="ts">
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

defineProps<{
  loading: boolean
  error: string | null
  empty: boolean
  emptyTitle: string
}>()
const emit = defineEmits<{ retry: [] }>()
</script>
