<script setup lang="ts">
import { problemMessage } from '~/composables/useApi'

const { t, locale } = useI18n()
useSeo(t('contact.title'), t('contact.subtitle'))

const { publicPost } = useApi()

const form = reactive({ name: '', email: '', subject: '', message: '', website: '' })
const errors = reactive<Record<string, string>>({})
const state = ref<'idle' | 'sending' | 'sent' | 'error'>('idle')
const errorMsg = ref('')

const emailOk = (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)

function validate(): boolean {
  errors.name = form.name.trim() ? '' : t('validation.required')
  errors.email = !form.email.trim()
    ? t('validation.required')
    : emailOk(form.email.trim()) ? '' : t('validation.email')
  errors.message = form.message.trim() ? '' : t('validation.required')
  return !errors.name && !errors.email && !errors.message
}

async function submit() {
  if (state.value === 'sending') return
  if (!validate()) return
  state.value = 'sending'
  errorMsg.value = ''
  try {
    await publicPost('contact', {
      name: form.name.trim(),
      email: form.email.trim(),
      subject: form.subject.trim() || undefined,
      message: form.message.trim(),
      locale: locale.value,
      website: form.website,
    })
    state.value = 'sent'
  }
  catch (e) {
    state.value = 'error'
    errorMsg.value = problemMessage(e, t('contact.errorBody'))
  }
}
</script>

<template>
  <div>
    <PageHero :title="t('contact.title')" :subtitle="t('contact.subtitle')" />
    <NContainer>
      <div class="max-w-xl py-12">
        <p class="text-slate-700">{{ t('contact.intro') }}</p>

        <NAlert v-if="state === 'sent'" tone="success" :title="t('contact.successTitle')" class="mt-6">
          {{ t('contact.sent') }}
        </NAlert>

        <form v-else class="mt-6 grid gap-5" novalidate @submit.prevent="submit">
          <NAlert v-if="state === 'error'" tone="danger" :title="t('contact.errorTitle')">
            {{ errorMsg }}
          </NAlert>

          <NField :label="t('contact.name')" for="c-name" :error="errors.name" required>
            <NInput id="c-name" v-model="form.name" :invalid="!!errors.name" autocomplete="name" :maxlength="120" />
          </NField>

          <NField :label="t('contact.email')" for="c-email" :error="errors.email" required>
            <NInput id="c-email" v-model="form.email" type="email" :invalid="!!errors.email" autocomplete="email" :maxlength="190" />
          </NField>

          <NField :label="t('contact.subject')" for="c-subject" :hint="t('contact.subjectOptional')">
            <NInput id="c-subject" v-model="form.subject" :maxlength="160" />
          </NField>

          <NField :label="t('contact.message')" for="c-message" :error="errors.message" required>
            <NTextarea id="c-message" v-model="form.message" :rows="6" :invalid="!!errors.message" :maxlength="4000" />
          </NField>

          <!-- honeypot: hidden from real users -->
          <div class="hidden" aria-hidden="true">
            <label>Leave this field empty
              <input v-model="form.website" type="text" tabindex="-1" autocomplete="off">
            </label>
          </div>

          <div>
            <NButton type="submit" :loading="state === 'sending'">
              {{ state === 'sending' ? t('contact.sending') : t('contact.submit') }}
            </NButton>
          </div>

          <p class="text-sm text-slate-500">{{ t('contact.email_help') }}</p>
        </form>
      </div>
    </NContainer>
  </div>
</template>
