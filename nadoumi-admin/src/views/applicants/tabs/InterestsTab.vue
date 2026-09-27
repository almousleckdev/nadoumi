<template>
  <div class="space-y-6">
    <div v-if="loading" class="text-sm text-slate-500">Loading interests...</div>
    <div v-else-if="!data" class="text-sm text-slate-500">No interests provided.</div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div>
        <h4 class="text-sm font-semibold text-slate-900 mb-2">Primary Interest</h4>
        <p class="text-sm text-slate-600">{{ data.primaryKind || 'Not specified' }}</p>
      </div>
      <div>
        <h4 class="text-sm font-semibold text-slate-900 mb-2">Preferred Programs</h4>
        <ul class="list-disc pl-5 text-sm text-slate-600">
          <li v-for="p in data.preferredPrograms" :key="p">{{ p }}</li>
        </ul>
      </div>
      <div>
        <h4 class="text-sm font-semibold text-slate-900 mb-2">Preferred Universities</h4>
        <ul class="list-disc pl-5 text-sm text-slate-600">
          <li v-for="u in data.preferredUniversities" :key="u">{{ u }}</li>
        </ul>
      </div>
      <div>
        <h4 class="text-sm font-semibold text-slate-900 mb-2">Preferred Countries</h4>
        <ul class="list-disc pl-5 text-sm text-slate-600">
          <li v-for="c in data.preferredCountries" :key="c">{{ c }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getInterests, type ApplicantInterest } from '@/api/applicant'

const props = defineProps<{ id: string }>()
const data = ref<ApplicantInterest | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    data.value = await getInterests(props.id)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>
