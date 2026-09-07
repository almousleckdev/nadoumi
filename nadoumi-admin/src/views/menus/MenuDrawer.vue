<template>
  <Drawer
    :model-value="modelValue"
    :title="t(isEdit ? 'menus.edit' : 'menus.new')"
    :saving="saving"
    size="480"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
    @save="save"
    @closed="reset"
  >
    <el-form
      v-if="modelValue"
      ref="formRef"
      v-loading="loading"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item :label="t('menus.parent')">
        <el-tree-select
          v-model="form.parentId"
          :data="parentTree"
          :props="{ label: 'label', children: 'children' }"
          value-key="id"
          node-key="id"
          check-strictly
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('menus.type')">
        <el-radio-group v-model="form.menuType">
          <el-radio value="M">
            {{ t('menus.typeMap.M') }}
          </el-radio>
          <el-radio value="C">
            {{ t('menus.typeMap.C') }}
          </el-radio>
          <el-radio value="F">
            {{ t('menus.typeMap.F') }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item
        :label="t('menus.name')"
        prop="menuName"
      >
        <el-input v-model="form.menuName" />
      </el-form-item>
      <el-form-item :label="t('menus.orderNum')">
        <el-input-number
          v-model="form.orderNum"
          :min="0"
        />
      </el-form-item>
      <el-form-item
        v-if="form.menuType !== 'F'"
        :label="t('menus.icon')"
      >
        <el-input
          v-model="form.icon"
          placeholder="Odometer"
        />
      </el-form-item>
      <el-form-item
        v-if="form.menuType !== 'F'"
        :label="t('menus.path')"
      >
        <el-input v-model="form.path" />
      </el-form-item>
      <el-form-item
        v-if="form.menuType === 'C'"
        :label="t('menus.component')"
      >
        <el-input v-model="form.component" />
      </el-form-item>
      <el-form-item
        v-if="form.menuType !== 'M'"
        :label="t('menus.perms')"
      >
        <el-input
          v-model="form.perms"
          placeholder="system:user:list"
        />
      </el-form-item>
      <el-form-item
        v-if="form.menuType !== 'F'"
        :label="t('menus.visible')"
      >
        <el-switch
          v-model="form.visible"
          active-value="0"
          inactive-value="1"
        />
      </el-form-item>
      <el-form-item :label="t('menus.statusLabel')">
        <el-switch
          v-model="form.status"
          active-value="0"
          inactive-value="1"
        />
      </el-form-item>
    </el-form>
  </Drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import Drawer from '@/components/ui/Drawer.vue'
import { getMenu, createMenu, updateMenu, menuTreeselect, type SysMenu } from '@/api/system'

const props = defineProps<{ modelValue: boolean, menuId?: number, parentId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [v: boolean], 'saved': [] }>()
const { t } = useI18n()

const isEdit = computed(() => props.menuId != null)
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const parentTree = ref<Array<{ id: number, label: string, children?: unknown[] }>>([])

type MenuForm = Partial<SysMenu> & { menuType: 'M' | 'C' | 'F', parentId: number }
const blank = (): MenuForm => ({
  parentId: props.parentId ?? 0,
  menuType: 'C', menuName: '', orderNum: 0, icon: '#', path: '', component: '',
  perms: '', visible: '0', status: '0', isFrame: '1', isCache: '0',
})
const form = reactive<MenuForm>(blank())
const rules = { menuName: [{ required: true, trigger: 'blur', message: t('common.required') }] }

function mapTree(nodes: SysMenu[]): Array<{ id: number, label: string, children?: unknown[] }> {
  return (nodes || []).map(n => ({
    id: (n as unknown as { id?: number }).id ?? n.menuId,
    label: (n as unknown as { label?: string }).label ?? n.menuName,
    children: n.children ? mapTree(n.children) : undefined,
  }))
}

async function open() {
  Object.assign(form, blank())
  loading.value = true
  try {
    const tree = await menuTreeselect()
    parentTree.value = [
      { id: 0, label: t('menus.root'), children: mapTree((tree as unknown as { data: SysMenu[] }).data) },
    ]
    if (isEdit.value) {
      const res = await getMenu(props.menuId!)
      Object.assign(form, res.data)
    }
  }
  finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value) await updateMenu(form)
    else await createMenu(form)
    ElMessage.success(t('common.saved'))
    emit('saved')
  }
  finally {
    saving.value = false
  }
}

function reset() { Object.assign(form, blank()) }
watch(() => props.modelValue, (o) => { if (o) open() })
</script>
