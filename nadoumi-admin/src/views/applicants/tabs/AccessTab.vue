<template>
  <StatePanel
    :loading="list.loading.value"
    :error="list.error.value"
    :empty="list.items.value.length === 0"
    :empty-title="t('applicant.noAccess')"
    :empty-description="t('applicant.noAccessDesc')"
    empty-icon="Connection"
    @retry="list.reload"
  >
    <el-table :data="list.items.value">
      <el-table-column
        :label="t('applicant.grantee')"
        min-width="200"
      >
        <template #default="{ row }">
          <div class="grantee">
            <span class="grantee__main">
              {{ row.userId ? t('applicant.userRef', { id: row.userId }) : t('applicant.invitePending') }}
            </span>
            <span
              v-if="row.invitedEmail"
              class="grantee__sub"
            >{{ row.invitedEmail }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('applicant.role')"
        width="130"
      >
        <template #default="{ row }">
          {{ titleCase(row.accessRole) }}
          <el-tag
            v-if="row.interim"
            size="small"
            type="warning"
            effect="plain"
            disable-transitions
          >
            {{ t('applicant.interim') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('applicant.status')"
        width="130"
      >
        <template #default="{ row }">
          <StatusBadge :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column
        :label="t('applicant.granted')"
        width="140"
      >
        <template #default="{ row }">
          {{ fmtDate(row.grantedAt) }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('applicant.expires')"
        width="140"
      >
        <template #default="{ row }">
          {{ fmtDate(row.expiresAt) }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('applicant.capabilities')"
        min-width="240"
      >
        <template #default="{ row }">
          <span class="caps">{{ row.effectiveCapabilities.join(', ') || '—' }}</span>
        </template>
      </el-table-column>
    </el-table>
  </StatePanel>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { listAccess, type AccessGrant } from '@/api/applicant'
import { useResourceList } from '@/composables/useResourceList'
import StatePanel from '@/components/ui/StatePanel.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'

const props = defineProps<{ id: string }>()
const emit = defineEmits<{ count: [n: number] }>()

const { t } = useI18n()
const list = useResourceList<AccessGrant>(() => listAccess(props.id))

watch(list.items, v => emit('count', v.length))
onMounted(list.load)

function titleCase(s: string) {
  return s ? s.charAt(0) + s.slice(1).toLowerCase() : s
}
function fmtDate(v: string | null): string {
  return v ? new Date(v).toLocaleDateString() : '—'
}
</script>

<style scoped>
.grantee {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.grantee__main {
  font-weight: 550;
}
.grantee__sub {
  font-size: 12px;
  color: var(--nad-ink-faint);
}
.caps {
  font-size: 12px;
  color: var(--nad-ink-soft);
}
</style>
