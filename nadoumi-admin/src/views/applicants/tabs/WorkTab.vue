<template>
  <div class="space-y-6">
    <div v-if="loading" class="text-sm text-slate-500">Loading work history...</div>
    <div v-else-if="!data || data.length === 0" class="text-sm text-slate-500">No work history provided.</div>
    <div v-else class="space-y-4">
      <div v-for="w in data" :key="w.id" class="p-4 border rounded-md">
        <h4 class="text-sm font-bold text-slate-900">{{ w.position }} at {{ w.employer }}</h4>
        <p class="text-xs text-slate-500 mt-1">
          {{ w.startDate }} - {{ w.current ? 'Present' : w.endDate }}
        </p>
        <p v-if="w.description" class="text-sm text-slate-600 mt-2">{{ w.description }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listWork, type ApplicantWork } from '@/api/applicant'

const props = defineProps<{ id: string }>()
const emit = defineEmits<{ (e: 'count', n: number): void }>()
const data = ref<ApplicantWork[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    data.value = await listWork(props.id)
    emit('count', data.value.length)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>
