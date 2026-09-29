<template>
  <div class="space-y-6">
    <div
      v-if="loading"
      class="text-sm text-slate-500"
    >
      Loading location...
    </div>
    <div
      v-else-if="!data"
      class="text-sm text-slate-500"
    >
      No location provided.
    </div>
    <div
      v-else
      class="grid grid-cols-1 md:grid-cols-2 gap-4"
    >
      <div>
        <h4 class="text-sm font-semibold text-slate-900 mb-2">
          Country of Residence
        </h4>
        <p class="text-sm text-slate-600">
          {{ data.countryOfResidence }}
        </p>
      </div>
      <div>
        <h4 class="text-sm font-semibold text-slate-900 mb-2">
          City & State
        </h4>
        <p class="text-sm text-slate-600">
          {{ data.city }}, {{ data.stateProvince }}
        </p>
      </div>
      <div>
        <h4 class="text-sm font-semibold text-slate-900 mb-2">
          Address
        </h4>
        <p class="text-sm text-slate-600">
          {{ data.addressLine1 }}<br v-if="data.addressLine2">{{ data.addressLine2 }}
        </p>
      </div>
      <div>
        <h4 class="text-sm font-semibold text-slate-900 mb-2">
          Postal Code
        </h4>
        <p class="text-sm text-slate-600">
          {{ data.postalCode }}
        </p>
      </div>
      <div>
        <h4 class="text-sm font-semibold text-slate-900 mb-2">
          Current Since
        </h4>
        <p class="text-sm text-slate-600">
          {{ data.currentSince }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getResidence, type ApplicantResidence } from '@/api/applicant'

const props = defineProps<{ id: string }>()
const data = ref<ApplicantResidence | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    data.value = await getResidence(props.id)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>
