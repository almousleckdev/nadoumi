<script setup lang="ts">
import { computed } from 'vue'
import type { ReceiptStatus } from '~/types/chat'

/** One tick = sent, two = delivered, two in the brand colour = read. A clock while sending, an alert if it failed. */
const props = defineProps<{ status: ReceiptStatus }>()
const { t } = useI18n()
const label = computed(() => t(`dashboard.messages.receipt.${props.status}`))
</script>

<template>
  <span
    class="inline-flex items-center"
    :class="status === 'read' ? 'text-brand-600' : status === 'failed' ? 'text-red-600' : 'text-slate-400'"
    role="img"
    :aria-label="label"
    :title="label"
    :data-status="status"
  >
    <svg v-if="status === 'sending'" viewBox="0 0 16 16" class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
      <circle cx="8" cy="8" r="6" /><path d="M8 4.5V8l2.2 1.4" stroke-linecap="round" />
    </svg>
    <svg v-else-if="status === 'failed'" viewBox="0 0 16 16" class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
      <circle cx="8" cy="8" r="6" /><path d="M8 5v3.5M8 11h.01" stroke-linecap="round" />
    </svg>
    <svg v-else-if="status === 'sent'" viewBox="0 0 16 16" class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
      <path d="m3.5 8.5 3 3 6-7" stroke-linecap="round" stroke-linejoin="round" />
    </svg>
    <svg v-else viewBox="0 0 20 16" class="h-3.5 w-4" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
      <path d="m1.5 8.5 3 3 6-7M8.5 11.5l.5.5 7-8" stroke-linecap="round" stroke-linejoin="round" />
    </svg>
  </span>
</template>
