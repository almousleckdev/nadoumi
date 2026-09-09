<script setup lang="ts">
import { problemMessage } from '~/composables/useApi'
import { CONTACT, telHref } from '~/data/contact'

const { t, locale } = useI18n()
useSeo(t('contact.title'), t('contact.lead'))

const { publicPost } = useApi()

const form = reactive({
  firstName: '', lastName: '', email: '', phone: '',
  category: '', subject: '', message: '', website: '',
})
const errors = reactive<Record<string, string>>({})
const state = ref<'idle' | 'sending' | 'sent' | 'error'>('idle')
const errorMsg = ref('')

const categories = ['GENERAL', 'SCHOLARSHIPS', 'UNIVERSITIES', 'APPLICATIONS', 'PARTNERSHIPS', 'OTHER']
const emailOk = (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)

function validate(): boolean {
  errors.firstName = form.firstName.trim() ? '' : t('validation.required')
  errors.lastName = form.lastName.trim() ? '' : t('validation.required')
  errors.email = !form.email.trim()
    ? t('validation.required')
    : emailOk(form.email.trim()) ? '' : t('validation.email')
  errors.message = form.message.trim() ? '' : t('validation.required')
  return !errors.firstName && !errors.lastName && !errors.email && !errors.message
}

async function submit() {
  if (state.value === 'sending') return
  if (!validate()) return
  state.value = 'sending'
  errorMsg.value = ''
  try {
    await publicPost('contact', {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      email: form.email.trim(),
      phone: form.phone.trim() || undefined,
      category: form.category || undefined,
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
    <PageHero :title="t('contact.title')" :subtitle="t('contact.lead')" />

    <NContainer>
      <div class="grid gap-10 py-12 lg:grid-cols-[20rem_minmax(0,1fr)] lg:gap-16">
        <!-- contact info -->
        <div class="space-y-4">
          <div class="rounded-xl border border-slate-200 bg-white p-5">
            <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('contact.emailTitle') }}</h2>
            <ul class="mt-2 space-y-1 text-sm">
              <li v-for="e in CONTACT.emails" :key="e">
                <a :href="`mailto:${e}`" class="text-brand-700 hover:text-brand-800">{{ e }}</a>
              </li>
            </ul>
            <p class="mt-2 text-sm text-slate-500">{{ t('contact.responseNote') }}</p>
          </div>
          <div class="rounded-xl border border-slate-200 bg-white p-5">
            <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('contact.phoneTitle') }}</h2>
            <ul class="mt-2 space-y-1 text-sm">
              <li v-for="p in CONTACT.phones" :key="p">
                <a :href="telHref(p)" class="text-slate-700 hover:text-slate-900">{{ p }}</a>
              </li>
            </ul>
          </div>
          <div class="rounded-xl border border-slate-200 bg-white p-5">
            <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('contact.officeTitle') }}</h2>
            <p class="mt-2 text-sm text-slate-700">{{ CONTACT.officeEn }}</p>
            <p class="mt-1 text-sm text-slate-500">{{ CONTACT.officeCn }}</p>
          </div>
          <div class="rounded-xl border border-slate-200 bg-white p-5">
            <h2 class="font-display text-sm font-semibold uppercase tracking-wide text-slate-500">{{ t('contact.hoursTitle') }}</h2>
            <p class="mt-2 text-sm text-slate-700">{{ CONTACT.hours }}</p>
          </div>
        </div>

        <!-- form -->
        <div>
          <NAlert v-if="state === 'sent'" tone="success" :title="t('contact.successTitle')">
            {{ t('contact.sent') }}
          </NAlert>

          <form v-else class="grid gap-5" novalidate @submit.prevent="submit">
            <NAlert v-if="state === 'error'" tone="danger" :title="t('contact.errorTitle')">
              {{ errorMsg }}
            </NAlert>

            <div class="grid gap-5 sm:grid-cols-2">
              <NField :label="t('contact.firstName')" for="c-first" :error="errors.firstName" required>
                <NInput id="c-first" v-model="form.firstName" :invalid="!!errors.firstName" autocomplete="given-name" :maxlength="80" />
              </NField>
              <NField :label="t('contact.lastName')" for="c-last" :error="errors.lastName" required>
                <NInput id="c-last" v-model="form.lastName" :invalid="!!errors.lastName" autocomplete="family-name" :maxlength="80" />
              </NField>
            </div>

            <div class="grid gap-5 sm:grid-cols-2">
              <NField :label="t('contact.email')" for="c-email" :error="errors.email" required>
                <NInput id="c-email" v-model="form.email" type="email" :invalid="!!errors.email" autocomplete="email" :maxlength="190" />
              </NField>
              <NField :label="t('contact.phone')" for="c-phone" :hint="t('contact.optional')">
                <NInput id="c-phone" v-model="form.phone" type="tel" autocomplete="tel" :maxlength="40" />
              </NField>
            </div>

            <div class="grid gap-5 sm:grid-cols-2">
              <NField :label="t('contact.category')" for="c-cat" :hint="t('contact.optional')">
                <NSelect
                  id="c-cat"
                  v-model="form.category"
                  :placeholder="t('contact.categoryPlaceholder')"
                  :options="categories.map(c => ({ value: c, label: t(`contact.cat.${c}`) }))"
                />
              </NField>
              <NField :label="t('contact.subject')" for="c-subject" :hint="t('contact.optional')">
                <NInput id="c-subject" v-model="form.subject" :maxlength="160" />
              </NField>
            </div>

            <NField :label="t('contact.message')" for="c-message" :error="errors.message" required>
              <NTextarea id="c-message" v-model="form.message" :rows="6" :invalid="!!errors.message" :maxlength="4000" />
            </NField>

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
          </form>
        </div>
      </div>
    </NContainer>
  </div>
</template>
