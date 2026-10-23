<script setup lang="ts">
import type { ApplyStep } from '~/constants/apply'
import type { StudentApplicationDto } from '~/types/catalog'

/**
 * The last step: what is still open, a look at the application form exactly as Nadoumi will receive it, the
 * application fee, and Submit. Submit stays locked, with the reason shown, until every step is done and the fee is paid.
 */
const props = defineProps<{ applicationId: number, openSteps: ApplyStep[], feeSettled: boolean }>()
const emit = defineEmits<{ goto: [step: ApplyStep], settled: [value: boolean], submitted: [application: StudentApplicationDto] }>()

const { t } = useI18n()
const { formPreview, submit } = useApplications()
const { busy, error, run } = useAsyncAction()
const { file: previewFile, open: previewOpen, showBlob } = useDocumentPreview()
const previewing = ref(false)

async function openPreview() {
  previewing.value = true
  try {
    showBlob(t('applyFlow.review.previewTitle'), await formPreview(props.applicationId))
  }
  catch {
    error.value = t('applyFlow.review.previewFailed')
  }
  finally {
    previewing.value = false
  }
}

const ready = computed(() => props.openSteps.length === 0 && props.feeSettled)
const hint = computed(() => {
  if (props.openSteps.length) return t('applyFlow.review.finishSteps')
  if (!props.feeSettled) return t('payment.payToSubmit')
  return t('applyFlow.review.readyHint')
})

async function onSubmit() {
  const submitted = await run(() => submit(props.applicationId))
  if (submitted) emit('submitted', submitted)
}
</script>

<template>
  <div class="grid gap-6">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

    <section class="grid gap-3" data-test="review-status">
      <h3 class="font-display text-lg font-semibold text-slate-900">{{ t('applyFlow.review.checkTitle') }}</h3>
      <NAlert v-if="!openSteps.length" tone="success">{{ t('applyFlow.review.allDone') }}</NAlert>
      <template v-else>
        <p class="text-sm text-slate-600">{{ t('applyFlow.review.stillOpen') }}</p>
        <ul class="grid gap-2">
          <li v-for="step in openSteps" :key="step" class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-3 py-2">
            <span class="text-sm font-medium text-slate-900">{{ t(`applyFlow.steps.${step}`) }}</span>
            <NButton variant="secondary" size="sm" :data-test="`open-${step}`" @click="emit('goto', step)">{{ t('applyFlow.review.open') }}</NButton>
          </li>
        </ul>
      </template>
    </section>

    <section class="grid gap-3">
      <h3 class="font-display text-lg font-semibold text-slate-900">{{ t('applyFlow.review.previewTitle') }}</h3>
      <p class="text-sm text-slate-600">{{ t('applyFlow.review.previewBlurb') }}</p>
      <div>
        <NButton variant="secondary" :loading="previewing" data-test="preview-form" @click="openPreview">{{ t('applyFlow.review.previewButton') }}</NButton>
      </div>
    </section>

    <PaymentPanel :application-id="applicationId" @settled="emit('settled', $event)" />

    <section class="grid gap-3 border-t border-slate-200 pt-5">
      <p class="text-sm text-slate-600" data-test="submit-hint">{{ hint }}</p>
      <div>
        <NButton :loading="busy" :disabled="!ready" data-test="submit" @click="onSubmit">{{ t('applyFlow.review.submit') }}</NButton>
      </div>
    </section>

    <DocumentViewer v-model="previewOpen" :file="previewFile" />
  </div>
</template>
