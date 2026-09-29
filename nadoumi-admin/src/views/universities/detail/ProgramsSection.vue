<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listPrograms, deleteProgram, type Program } from '@/api/program'
import type { University } from '@/api/university'
import { useUserStore } from '@/stores/user'
import { useConfirm } from '@/composables/useConfirm'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import ProgramDrawer from '@/views/programs/ProgramDrawer.vue'

const PROGRAM_PAGE_SIZE = 100

const props = defineProps<{ university: University }>()

const { t } = useI18n()
const userStore = useUserStore()
const { confirm } = useConfirm()

const programs = ref<Program[]>([])
const drawerOpen = ref(false)
const editing = ref<Program | null>(null)

async function load() {
  try {
    const res = await listPrograms({ universityId: props.university.id, page: 0, size: PROGRAM_PAGE_SIZE })
    programs.value = res.content
  }
  catch {
    programs.value = []
  }
}

function openCreate() {
  editing.value = null
  drawerOpen.value = true
}

function openEdit(p: Program) {
  editing.value = p
  drawerOpen.value = true
}

async function onDelete(p: Program) {
  const ok = await confirm({
    title: t('program.deleteTitle'),
    message: t('program.deleteConfirm', { name: p.name }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  await deleteProgram(p.id)
  ElMessage.success(t('common.deleted'))
  load()
}

onMounted(load)
watch(() => props.university.id, load)

defineExpose({ reload: load })
</script>

<template>
  <div>
    <div class="nad-card sec">
      <div class="sec__head">
        <h3 class="sec__title">
          {{ t('program.sectionTitle') }}
        </h3>
        <el-button
          v-if="userStore.hasPerm('nad:program:create')"
          size="small"
          :icon="Plus"
          @click="openCreate"
        >
          {{ t('program.addHere') }}
        </el-button>
      </div>
      <p class="sec__hint">
        {{ t('program.sectionHint') }}
      </p>
      <el-table
        v-if="programs.length"
        :data="programs"
      >
        <el-table-column
          :label="t('program.name')"
          prop="name"
          min-width="200"
        />
        <el-table-column
          :label="t('program.type')"
          min-width="160"
        >
          <template #default="{ row }">
            {{ row.programType === 'DEGREE' && row.levels?.length
              ? row.levels.map((l: string) => t(`program.levelMap.${l}`)).join(', ')
              : t(`program.typeMap.${row.programType}`) }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('program.language')"
          width="120"
        >
          <template #default="{ row }">
            {{ row.teachingLanguage ? t(`program.langMap.${row.teachingLanguage}`) : '' }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('program.publishStatus')"
          width="120"
        >
          <template #default="{ row }">
            <StatusBadge
              :status="row.publishStatus"
              :map="{ PUBLISHED: 'success', DRAFT: 'neutral' }"
            />
          </template>
        </el-table-column>
        <el-table-column
          :label="t('common.actions')"
          width="130"
          align="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="userStore.hasPerm('nad:program:edit')"
              link
              size="small"
              @click="openEdit(row as Program)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              v-if="userStore.hasPerm('nad:program:remove')"
              link
              size="small"
              type="danger"
              @click="onDelete(row as Program)"
            >
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <p
        v-else
        class="muted"
      >
        {{ t('program.none') }}
      </p>
    </div>

    <ProgramDrawer
      v-model="drawerOpen"
      :program="editing"
      :locked-university="{ id: university.id, name: university.name }"
      @saved="load"
    />
  </div>
</template>

<style scoped src="./detail.css" />
