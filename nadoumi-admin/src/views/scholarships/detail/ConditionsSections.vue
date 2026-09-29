<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ScholarshipView } from '@/api/scholarship'
import FormSection from '@/components/ui/FormSection.vue'

const props = defineProps<{ view: ScholarshipView }>()

const { t } = useI18n()

const prose = computed(() => [
  { label: t('scholarship.benefits'), value: props.view.benefits },
  { label: t('scholarship.requirements'), value: props.view.requirements },
  { label: t('scholarship.policy'), value: props.view.policy },
].filter(p => p.value) as { label: string, value: string }[])
</script>

<template>
  <FormSection
    v-if="view.coverage.length"
    :title="t('scholarship.secCoverage')"
  >
    <ul class="list">
      <li
        v-for="(c, i) in view.coverage"
        :key="i"
      >
        <strong>{{ t(`scholarship.coverageKind.${c.kind}`, c.kind) }}</strong>
        <span
          v-if="c.detail"
          class="muted"
        > · {{ c.detail }}</span>
      </li>
    </ul>
  </FormSection>

  <FormSection
    v-if="view.renewalConditions"
    :title="t('scholarship.renewalConditions')"
  >
    <p class="prose">
      {{ view.renewalConditions }}
    </p>
  </FormSection>

  <FormSection
    v-if="view.intakes.length"
    :title="t('scholarship.secIntakes')"
  >
    <ul class="list">
      <li
        v-for="(it, i) in view.intakes"
        :key="i"
      >
        {{ t(`scholarship.intake.${it.term}`) }}
        <span
          v-if="it.applicationClose"
          class="muted"
        > · {{ t('scholarship.closes') }} {{ it.applicationClose }}</span>
      </li>
    </ul>
  </FormSection>

  <FormSection
    v-if="view.documentRequirements.length"
    :title="t('scholarship.secDocuments')"
  >
    <ul class="list">
      <li
        v-for="(d, i) in view.documentRequirements"
        :key="i"
      >
        {{ t(`scholarship.doc.${d.docType}`, d.docType) }}
        <el-tag
          size="small"
          :type="d.mandatory ? 'danger' : 'info'"
          effect="plain"
        >
          {{ d.mandatory ? t('scholarship.mandatory') : t('scholarship.optionalDoc') }}
        </el-tag>
        <span
          v-if="d.note"
          class="muted"
        > · {{ d.note }}</span>
      </li>
    </ul>
  </FormSection>

  <FormSection
    v-for="p in prose"
    :key="p.label"
    :title="p.label"
  >
    <p class="prose">
      {{ p.value }}
    </p>
  </FormSection>
</template>

<style scoped src="./detail.css" />
