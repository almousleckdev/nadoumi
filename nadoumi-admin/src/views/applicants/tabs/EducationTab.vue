<template>
  <div class="edu">
    <div v-if="loading" class="p-4 text-center">Loading...</div>
    <div v-else-if="error" class="p-4 text-red-500 text-center">{{ error }}</div>
    <div v-else>
      <div v-if="items.length === 0" class="edu__empty">
        {{ t('applicant.noEducation') }}
      </div>
      <div v-for="edu in items" :key="edu.id" class="edu__card">
        <div class="edu__main">
          <div class="edu__degree">{{ edu.degree }}</div>
          <div class="edu__school">{{ edu.school }}</div>
          <div class="edu__major">{{ edu.major }}</div>
          <div class="edu__dates">
            {{ edu.startDate }} — {{ edu.endDate ?? t('applicant.present') }}
          </div>
          <div class="edu__gpa" v-if="edu.gpa">
            GPA: {{ edu.gpa }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { listEducation } from '@/api/applicant'

const props = defineProps<{ id: string }>()
const { t } = useI18n()

const items = ref<any[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const res = await listEducation(props.id)
    items.value = res || []
  } catch (e: any) {
    error.value = e.message || 'Error fetching'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.edu { padding: 4px; }
.edu__empty {
  color: var(--nad-ink-soft);
  font-size: 13px;
  text-align: center;
  padding: 24px;
}
.edu__card {
  border: 1px solid var(--nad-border);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.edu__main { flex: 1; }
.edu__degree { font-weight: 600; font-size: 15px; margin-bottom: 4px; }
.edu__school { font-size: 14px; margin-bottom: 2px; }
.edu__major { font-size: 13px; color: var(--nad-ink-soft); margin-bottom: 6px; }
.edu__dates, .edu__gpa { font-size: 13px; color: var(--nad-ink-soft); }
</style>
