<script setup lang="ts">
const { t } = useI18n()
const { applicants, activeApplicantId, setActiveApplicant } = useSession()
</script>

<template>
  <NDropdown :label="`${t('dashboard.applicantLabel')} #${activeApplicantId ?? ''}`">
    <button
      v-for="a in applicants"
      :key="a.applicantId"
      type="button"
      class="block w-full px-3 py-2 text-start text-sm hover:bg-slate-50"
      :class="a.applicantId === activeApplicantId ? 'font-semibold text-brand-700' : 'text-slate-700'"
      @click="setActiveApplicant(a.applicantId)"
    >
      {{ t('dashboard.applicantLabel') }} #{{ a.applicantId }} · {{ a.accessRole }}
    </button>
  </NDropdown>
</template>
