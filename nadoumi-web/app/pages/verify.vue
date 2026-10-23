<script setup lang="ts">
import type { DocumentVerificationDto } from '~/types/verify'

/**
 * The page a document's QR code opens. It says whether Nadoumi issued a document with that number and code, and
 * what kind it is. It never shows a name, an amount or an application; a bad or missing code is just "not valid".
 */
const { t } = useI18n()
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
        <dl class="mt-3 grid gap-2 text-sm">
          <div>
            <dt class="text-slate-500">{{ t('verify.kind') }}</dt>
            <dd class="font-medium text-slate-900">{{ t(`verify.kinds.${result.kind}`) }}</dd>
          </div>
          <div>
            <dt class="text-slate-500">{{ t('verify.number') }}</dt>
            <dd class="font-mono text-slate-900">{{ result.number }}</dd>
          </div>
          <div>
            <dt class="text-slate-500">{{ t('verify.issuedOn') }}</dt>
            <dd class="text-slate-900">{{ result.issuedOn }}</dd>
          </div>
        </dl>
        <p class="mt-3 text-xs text-slate-500">{{ t('verify.privacy') }}</p>
      </div>

      <NAlert v-else tone="danger" data-test="invalid">{{ t('verify.invalid') }}</NAlert>
    </div>
  </NContainer>
</template>
