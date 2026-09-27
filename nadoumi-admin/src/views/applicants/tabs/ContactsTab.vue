<template>
  <div class="con">
    <div v-if="loading" class="p-4 text-center">Loading...</div>
    <div v-else-if="error" class="p-4 text-red-500 text-center">{{ error }}</div>
    <el-table v-else :data="items" size="small" style="width: 100%" empty-text="No contacts">
      <el-table-column prop="relation" :label="t('applicant.contactRelation')" width="120" />
      <el-table-column prop="name" :label="t('applicant.contactName')" min-width="120" />
      <el-table-column prop="phone" :label="t('applicant.phone')" width="140" />
      <el-table-column prop="email" :label="t('applicant.email')" min-width="180" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { listContacts } from '@/api/applicant'

const props = defineProps<{ id: string }>()
const { t } = useI18n()

const items = ref<any[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const res = await listContacts(props.id)
    items.value = res || []
  } catch (e: any) {
    error.value = e.message || 'Error fetching'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.con { padding: 4px; }
</style>
