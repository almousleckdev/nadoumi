<template>
  <el-form-item
    :label="label"
    :prop="prop"
  >
    <el-input-number
      v-model="amount"
      :min="0"
      :precision="2"
      :step="100"
      controls-position="right"
      style="width: 100%"
    />
    <span class="money__usd">{{ usdPreview }}</span>
    <p
      v-if="note"
      class="money__note"
    >
      {{ note }}
    </p>
  </el-form-item>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { cnyToUsdRate } from '@/api/fx'
import { usd } from '@/utils/money'

/**
 * An RMB amount input with the live USD equivalent beside it, converted at the editable
 * `nadoumi.fx.cny_usd` rate. Every screen that takes money in RMB uses this one field.
 */
defineProps<{ label: string, prop: string, note?: string }>()
const amount = defineModel<number>({ required: true })

const fxRate = ref<number | null>(null)
const usdPreview = computed(() =>
  amount.value && fxRate.value ? `≈ ${usd(amount.value * fxRate.value)}` : '')

onMounted(async () => { fxRate.value = await cnyToUsdRate() })
</script>

<style scoped>
.money__usd { display: block; margin-top: 4px; font-size: 12px; color: var(--nad-ink-soft, #64748b); font-variant-numeric: tabular-nums; }
.money__note { margin: 4px 0 0; font-size: 12px; color: var(--nad-ink-faint, #9ca3af); }
</style>
