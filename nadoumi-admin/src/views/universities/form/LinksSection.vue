<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Delete } from '@element-plus/icons-vue'
import { UNIVERSITY_LINK_KINDS, type UniversityLinkKind } from '@/api/university'
import FormSection from '@/components/ui/FormSection.vue'
import { useUniversityForm } from '../universityForm'

const { t } = useI18n()
const form = useUniversityForm()

// one link per kind: offer only the kinds not used yet
const freeKinds = computed(() => UNIVERSITY_LINK_KINDS.filter(k => !form.links.some(l => l.kind === k)))

function add() {
  const kind = freeKinds.value[0]
  if (kind) form.links.push({ kind, value: '' })
}

function placeholder(kind: UniversityLinkKind): string {
  return kind === 'WECHAT' ? t('university.links.wechatPlaceholder') : 'https://'
}
</script>

<template>
  <FormSection
    :title="t('university.links.title')"
    :description="t('university.links.hint')"
  >
    <el-form-item :label="t('university.links.address')">
      <el-input
        v-model="form.address"
        maxlength="300"
      />
    </el-form-item>
    <div
      v-for="(l, i) in form.links"
      :key="i"
      class="repeat"
    >
      <el-select
        v-model="l.kind"
        style="width: 160px"
      >
        <el-option
          v-for="k in UNIVERSITY_LINK_KINDS"
          :key="k"
          :value="k"
          :label="t(`university.links.kind.${k}`)"
          :disabled="form.links.some(o => o !== l && o.kind === k)"
        />
      </el-select>
      <el-input
        v-model="l.value"
        maxlength="500"
        :placeholder="placeholder(l.kind)"
      />
      <el-button
        :icon="Delete"
        text
        @click="form.links.splice(i, 1)"
      />
    </div>
    <el-button
      size="small"
      :icon="Plus"
      :disabled="!freeKinds.length"
      @click="add"
    >
      {{ t('university.links.add') }}
    </el-button>
  </FormSection>
</template>
