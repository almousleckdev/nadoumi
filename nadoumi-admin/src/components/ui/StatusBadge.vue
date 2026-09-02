<template>
  <span
    class="badge"
    :class="`badge--${tone}`"
  >
    <span class="badge__dot" />
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger'

const props = withDefaults(defineProps<{
  /** raw status value, e.g. "ACTIVE", "PENDING_REVIEW"; null/empty renders "Unknown" */
  status?: string | null
  /** optional override map from status -> tone */
  map?: Record<string, Tone>
  /** optional display label; defaults to a sentence-cased status */
  label?: string
}>(), { status: null, map: undefined, label: undefined })

// Shared status vocabulary across every admin module. Extend here, not per-screen.
const DEFAULT_MAP: Record<string, Tone> = {
  ACTIVE: 'success',
  ACCEPTED: 'success',
  VERIFIED: 'success',
  PAID: 'success',
  DRAFT: 'neutral',
  ARCHIVED: 'neutral',
  CLOSED: 'neutral',
  UNLINKED: 'warning',
  PENDING: 'warning',
  PENDING_REVIEW: 'warning',
  IN_REVIEW: 'info',
  SUBMITTED: 'info',
  REJECTED: 'danger',
  EXPIRED: 'danger',
  FAILED: 'danger',
  OVERDUE: 'danger',
}

const tone = computed<Tone>(() =>
  (props.status ? (props.map ?? DEFAULT_MAP)[props.status] : undefined) ?? 'neutral')

const label = computed(() => {
  if (props.label) return props.label
  if (!props.status) return 'Unknown'
  const s = props.status.toLowerCase().replace(/_/g, ' ')
  return s.charAt(0).toUpperCase() + s.slice(1)
})
</script>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 9px 2px 7px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  border: 1px solid transparent;
  white-space: nowrap;
}
.badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: currentColor;
}
.badge--neutral {
  color: #475569;
  background: #f1f5f9;
  border-color: #e2e8f0;
}
.badge--info {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #dbeafe;
}
.badge--success {
  color: #15803d;
  background: #f0fdf4;
  border-color: #dcfce7;
}
.badge--warning {
  color: #b45309;
  background: #fffbeb;
  border-color: #fef3c7;
}
.badge--danger {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fee2e2;
}
</style>
