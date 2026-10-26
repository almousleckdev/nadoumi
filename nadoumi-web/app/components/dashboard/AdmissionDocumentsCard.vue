<script setup lang="ts">
import type { LetterDto, LetterKind } from '~/types/applicationExtras'

/**
 * The admission letter and the JW202 form. They are released only after the Nadoumi service fee is confirmed: the server
 * answers 402 for a locked file, and a locked row never shows a file name. This card also hosts the service-fee payment.
 */
const props = defineProps<{ applicationId: number }>()

const { t } = useI18n()
const { letters, letter } = useApplicationExtras(props.applicationId)
const toast = useToast()

const KINDS: readonly LetterKind[] = ['ADMISSION_LETTER', 'JW202']

const list = ref<LetterDto[]>([])
const opening = ref<LetterKind | null>(null)

async function load() {
  try {
    list.value = await letters()
  }
  catch {
    list.value = []
  }
}

async function open(kind: LetterKind) {
  opening.value = kind
  try {
    const url = URL.createObjectURL(await letter(kind))
    window.open(url, '_blank', 'noopener')
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  }
  catch {
    toast.error(t('errors.loadSection'))
    await load()
  }
  finally {
    opening.value = null
  }
}

const rows = computed(() => KINDS.map(kind => ({ kind, letter: list.value.find((l: LetterDto) => l.kind === kind) ?? null })))

onMounted(load)
</script>

<template>
  <div class="grid gap-6" data-test="admission-documents">
    <PaymentPanel :application-id="applicationId" variant="service" submitted @settled="load" />
    <SectionCard :title="t('dashboard.applications.admission.title')">
      <p class="mb-3 text-sm text-slate-600">{{ t('dashboard.applications.admission.help') }}</p>
      <ul class="grid gap-2">
        <li v-for="row in rows" :key="row.kind" class="flex flex-wrap items-center justify-between gap-3 rounded-md border border-slate-200 p-3" :data-test="`letter-${row.kind}`">
          <div class="min-w-0">
            <p class="text-sm font-medium text-slate-900">{{ t(`dashboard.applications.admission.kind.${row.kind}`) }}</p>
            <p class="text-xs text-slate-500">
              <template v-if="!row.letter">{{ t('dashboard.applications.admission.notYet') }}</template>
              <template v-else-if="row.letter.locked">{{ t('dashboard.applications.admission.locked') }}</template>
              <template v-else>{{ row.letter.fileName }}</template>
            </p>
          </div>
          <NButton
            v-if="row.letter && !row.letter.locked"
            size="sm"
            variant="secondary"
            :loading="opening === row.kind"
            :data-test="`open-${row.kind}`"
            @click="open(row.kind)"
          >{{ t('dashboard.applications.admission.open') }}</NButton>
        </li>
      </ul>
    </SectionCard>
  </div>
</template>
