<script setup lang="ts">
import type { DocumentVerificationDto } from '~/types/verify'
import { DATE_TIME, formatTimestamp } from '~/utils/dates'

/**
 * The page a document's QR code opens. It says whether Nadoumi issued a document with that number and code and, for a
 * valid pair, shows the student's name, the programme and the fee line so the paper can be checked against the person.
 * A bad or missing code is just "not valid" and shows nothing.
 */
const { t, te, locale } = useI18n()
const route = useRoute()
const { publicGet } = useApi()

const number = String(route.query.number ?? '').slice(0, 40)
const code = String(route.query.code ?? '').slice(0, 20)
const asked = Boolean(number && code)

const result = ref<DocumentVerificationDto | null>(null)
const failed = ref(false)
const pending = ref(asked)

onMounted(async () => {
  if (!asked) return
  try {
    result.value = await publicGet<DocumentVerificationDto>('documents/verify', { number, code })
  }
  catch {
    failed.value = true
  }
  finally {
    pending.value = false
  }
})

const rows = computed(() => {
  const d = result.value?.details
  if (!d) return []
  const money = d.feeAmount == null ? '' : new Intl.NumberFormat(locale.value, { style: 'currency', currency: d.currency ?? 'CNY' }).format(d.feeAmount)
  const intake = d.intake && te(`program.intake.${d.intake}`) ? t(`program.intake.${d.intake}`) : d.intake
  return [
    { key: 'student', label: t('verify.student'), value: d.applicantName, strong: true },
    { key: 'reference', label: t('verify.application'), value: d.applicationReference },
    { key: 'opportunity', label: t('verify.opportunity'), value: d.opportunity },
    { key: 'university', label: t('verify.university'), value: d.universityName },
    { key: 'intake', label: t('verify.intake'), value: intake },
    { key: 'fee', label: t('verify.fee'), value: money, price: true },
    { key: 'feeStatus', label: t('verify.feeStatus'), value: t(`verify.feeStatuses.${d.feeStatus}`) },
    { key: 'feePaidAt', label: t('verify.feePaidAt'), value: formatTimestamp(d.feePaidAt, locale.value, DATE_TIME) },
  ].filter(r => r.value)
})

useSeo(t('verify.title'), t('verify.blurb'))
useHead({ meta: [{ name: 'robots', content: 'noindex' }] })
</script>

<template>
  <NContainer class="py-12">
    <div class="mx-auto grid max-w-xl gap-4">
      <h1 class="font-display text-2xl font-bold text-slate-900">{{ t('verify.title') }}</h1>
      <p class="text-slate-600">{{ t('verify.blurb') }}</p>

      <NAlert v-if="!asked" tone="warning">{{ t('verify.missing') }}</NAlert>
      <NSkeleton v-else-if="pending" class="h-32 w-full rounded-xl" />
      <NAlert v-else-if="failed" tone="danger" data-test="failed">{{ t('verify.failed') }}</NAlert>

      <div v-else-if="result?.valid" class="rounded-xl border border-slate-200 bg-white p-5" data-test="valid">
        <p class="font-display text-lg font-semibold text-slate-900">{{ t('verify.valid') }}</p>
        <dl v-if="rows.length" class="mt-4 grid gap-3 text-sm" data-test="details">
          <div v-for="r in rows" :key="r.key" class="flex flex-wrap justify-between gap-x-4 border-b border-slate-100 pb-2 last:border-0">
            <dt class="text-slate-500">{{ r.label }}</dt>
            <dd class="text-end font-medium" :class="[r.strong ? 'font-display text-base font-semibold text-slate-900' : 'text-slate-900', r.price ? 'text-red-600' : '']">{{ r.value }}</dd>
          </div>
        </dl>
        <dl class="mt-4 grid gap-3 border-t border-slate-200 pt-4 text-sm">
          <div class="flex flex-wrap justify-between gap-x-4">
            <dt class="text-slate-500">{{ t('verify.kind') }}</dt>
            <dd class="font-medium text-slate-900">{{ t(`verify.kinds.${result.kind}`) }}</dd>
          </div>
          <div class="flex flex-wrap justify-between gap-x-4">
            <dt class="text-slate-500">{{ t('verify.number') }}</dt>
            <dd class="font-mono text-slate-900">{{ result.number }}</dd>
          </div>
          <div class="flex flex-wrap justify-between gap-x-4">
            <dt class="text-slate-500">{{ t('verify.issuedOn') }}</dt>
            <dd class="text-slate-900">{{ result.details?.issuedAt ? formatTimestamp(result.details.issuedAt, locale, DATE_TIME) : result.issuedOn }}</dd>
          </div>
        </dl>
        <p class="mt-4 text-xs text-slate-500">{{ t('verify.privacy') }}</p>
      </div>

      <NAlert v-else tone="danger" data-test="invalid">{{ t('verify.invalid') }}</NAlert>
    </div>
  </NContainer>
</template>
