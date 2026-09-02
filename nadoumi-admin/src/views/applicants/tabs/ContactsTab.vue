<template>
  <div>
    <div
      v-if="canEdit"
      class="tab-toolbar"
    >
      <el-button
        size="small"
        :icon="Plus"
        @click="openAdd"
      >
        {{ t('applicant.addContact') }}
      </el-button>
    </div>

    <StatePanel
      :loading="list.loading.value"
      :error="list.error.value"
      :empty="list.items.value.length === 0"
      :empty-title="t('applicant.noContacts')"
      @retry="list.reload"
    >
      <el-table :data="list.items.value">
        <el-table-column
          :label="t('applicant.relation')"
          width="140"
        >
          <template #default="{ row }">
            {{ titleCase(row.relation) }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('applicant.contactName')"
          prop="name"
          min-width="170"
        />
        <el-table-column
          :label="t('applicant.email')"
          prop="email"
          min-width="190"
        />
        <el-table-column
          :label="t('applicant.phone')"
          prop="phone"
          width="150"
        />
        <el-table-column
          v-if="canEdit"
          width="110"
          align="right"
        >
          <template #default="{ row }">
            <el-button
              link
              size="small"
              @click="openEdit(row as Contact)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              link
              size="small"
              type="danger"
              @click="onDelete(row as Contact)"
            >
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </StatePanel>

    <Drawer
      v-model="open"
      :title="editing ? t('applicant.editContact') : t('applicant.addContact')"
      :saving="saving"
      @save="save"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <el-form-item
          :label="t('applicant.relation')"
          prop="relation"
        >
          <el-select
            v-model="form.relation"
            style="width: 100%"
          >
            <el-option
              v-for="r in RELATIONS"
              :key="r"
              :label="titleCase(r)"
              :value="r"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="t('applicant.contactName')"
          prop="name"
        >
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item
          :label="t('applicant.email')"
          prop="email"
        >
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item
          :label="t('applicant.phone')"
          prop="phone"
        >
          <el-input v-model="form.phone" />
        </el-form-item>
      </el-form>
    </Drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listContacts, addContact, updateContact, deleteContact,
  type Contact, type ContactInput,
} from '@/api/applicant'
import { useResourceList } from '@/composables/useResourceList'
import { useConfirm } from '@/composables/useConfirm'
import StatePanel from '@/components/ui/StatePanel.vue'
import Drawer from '@/components/ui/Drawer.vue'

const props = defineProps<{ id: string, canEdit: boolean }>()
const emit = defineEmits<{ count: [n: number] }>()

const { t } = useI18n()
const { confirm } = useConfirm()
const list = useResourceList<Contact>(() => listContacts(props.id))

const RELATIONS = ['GUARDIAN', 'EMERGENCY', 'OTHER']

watch(list.items, v => emit('count', v.length))
onMounted(list.load)

function titleCase(s: string) {
  return s ? s.charAt(0) + s.slice(1).toLowerCase() : s
}

const open = ref(false)
const saving = ref(false)
const editing = ref<Contact | null>(null)
const formRef = ref<FormInstance>()
const blank = { relation: 'GUARDIAN', name: '', email: '', phone: '' }
const form = reactive({ ...blank })
const rules = {
  relation: [{ required: true, message: t('applicant.required') }],
  name: [{ required: true, trigger: 'blur', message: t('applicant.required') }],
  email: [{ type: 'email' as const, trigger: 'blur', message: t('applicant.emailInvalid') }],
}

function openAdd() {
  editing.value = null
  Object.assign(form, blank)
  open.value = true
}
function openEdit(row: Contact) {
  editing.value = row
  Object.assign(form, {
    relation: row.relation, name: row.name,
    email: row.email ?? '', phone: row.phone ?? '',
  })
  open.value = true
}

function payload(): ContactInput {
  return {
    relation: form.relation,
    name: form.name.trim(),
    email: form.email || null,
    phone: form.phone || null,
  }
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editing.value) await updateContact(props.id, editing.value.id, payload())
    else await addContact(props.id, payload())
    ElMessage.success(t('common.saved'))
    open.value = false
    await list.reload()
  }
  finally {
    saving.value = false
  }
}

async function onDelete(row: Contact) {
  const ok = await confirm({
    message: t('applicant.deleteContactConfirm', { name: row.name }),
    confirmText: t('common.delete'),
    tone: 'danger',
  })
  if (!ok) return
  await deleteContact(props.id, row.id)
  ElMessage.success(t('common.deleted'))
  await list.reload()
}
</script>

<style scoped>
.tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
</style>
