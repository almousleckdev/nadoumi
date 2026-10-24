<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { listAssignees, type StaffUser } from '@/api/application'

const open = defineModel<boolean>({ required: true })
const props = defineProps<{ saving: boolean, current: number | null }>()
const emit = defineEmits<{ submit: [userId: number] }>()

const { t } = useI18n()
const staff = ref<StaffUser[]>([])
const loading = ref(false)
const picked = ref<number | null>(null)

watch(open, async (isOpen) => {
  if (!isOpen) return
  picked.value = props.current
  loading.value = true
  try {
    staff.value = await listAssignees()
  }
  finally {
    loading.value = false
  }
})
</script>

<template>
  <el-dialog
    v-model="open"
    :title="t('applications.assignTitle')"
    width="440px"
    append-to-body
  >
    <p class="as-help">
      {{ t('applications.assignHelp') }}
    </p>
    <el-select
      v-model="picked"
      v-loading="loading"
      filterable
      :placeholder="t('applications.pickStaff')"
      style="width: 100%"
      data-test="assign-select"
    >
      <el-option
        v-for="s in staff"
        :key="s.userId"
        :label="s.name"
        :value="s.userId"
      >
        <span>{{ s.name }}</span>
        <span class="as-user">{{ s.userName }}</span>
      </el-option>
    </el-select>
    <template #footer>
      <el-button @click="open = false">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="picked === null"
        data-test="assign-submit"
        @click="picked !== null && emit('submit', picked)"
      >
        {{ t('applications.assign') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.as-help { margin: 0 0 12px; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
.as-user { float: right; color: var(--nad-ink-faint, #94a3b8); font-size: 12px; }
</style>
