<template>
  <div class="con">
    <AsyncList ref="list" :fetch="() => listContacts(props.id)">
      <template #default="{ items }">
        <el-table :data="items" size="small" style="width: 100%" empty-text="No contacts">
          <el-table-column prop="relation" :label="t('applicant.contactRelation')" width="120" />
          <el-table-column prop="name" :label="t('applicant.contactName')" min-width="120" />
          <el-table-column prop="phone" :label="t('applicant.phone')" width="140" />
          <el-table-column prop="email" :label="t('applicant.email')" min-width="180" />
        </el-table>
      </template>
    </AsyncList>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listContacts } from '@/api/applicant'
import AsyncList from '@/components/ui/AsyncList.vue'

const props = defineProps<{ id: string }>()
const { t } = useI18n()
const list = ref<InstanceType<typeof AsyncList>>()
</script>

<style scoped>
.con { padding: 4px; }
</style>
