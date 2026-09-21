<script setup lang="ts">
import { statusKey, statusTone } from '~/utils/applicationView'

defineProps<{ status: string | null }>()
const { t } = useI18n()
</script>

<template>
  <NBadge :tone="statusTone(status)">{{ t(`dashboard.applications.status.${statusKey(status)}`) }}</NBadge>
</template>
