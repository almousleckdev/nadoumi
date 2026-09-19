<script setup lang="ts">
import type { Component, ComputedRef } from 'vue'
import type { ApplicantDto } from '~/types/catalog'
import type { SelfApplicantBody } from '~/composables/useApplicant'
import ContactSection from '~/components/applicant/ContactSection.vue'
import EducationSection from '~/components/applicant/EducationSection.vue'
import InterestsSection from '~/components/applicant/InterestsSection.vue'
import ResidenceSection from '~/components/applicant/ResidenceSection.vue'
import WorkSection from '~/components/applicant/WorkSection.vue'
import { ONBOARDING_STEPS, STEP_SECTIONS, type OnboardingStep } from '~/constants/onboarding'

definePageMeta({ layout: 'onboarding', middleware: ['auth', 'onboarding'] })
const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId, refresh } = useSession()
const { markComplete } = useOnboarding()
const { listMine, get, create, update, completeOnboarding, onboardingStatus } = useApplicant()

/** The steps that are a plain section component; personal, identity and review are laid out below. */
const SECTION_STEPS: Partial<Record<OnboardingStep, Component>> = {
  education: EducationSection, interests: InterestsSection, location: ResidenceSection,
  contact: ContactSection, work: WorkSection,
}

const current = ref(0)
const stepKey: ComputedRef<OnboardingStep> = computed(() => ONBOARDING_STEPS[current.value] as OnboardingStep)
const progressSteps = computed(() => ONBOARDING_STEPS.map(key => ({ key, label: t(`onboarding.steps.${key}`) })))
const sectionStep = computed(() => SECTION_STEPS[stepKey.value] ?? null)

const applicant = ref<ApplicantDto | null>(null)
const finishing = ref(false)
const { busy, notice, error, run } = useAsyncAction()
const progress = useOnboardingProgress(() => applicant.value?.id ?? null)

async function load() {
  const mine = await listMine().catch(() => [])
  const chosen = mine.find(a => a.id === activeApplicantId.value) ?? mine[0] ?? null
  applicant.value = chosen ? await get(chosen.id) : null
}
await load()
await progress.refresh()

async function saveProfile(body: SelfApplicantBody) {
  const saved = await run(async () => {
    if (applicant.value) applicant.value = await update(applicant.value.id, body)
    else { applicant.value = await create(body); await refresh() }
    return true
  }, t('onboarding.saved'))
  // the profile feeds the passport comparison, so re-read what the server considers complete
  await progress.refresh()
  if (saved) next()
}

/** A step may be left once the server says every section it owns is complete. */
const canAdvance = computed(() => STEP_SECTIONS[stepKey.value].every(progress.isComplete))

const goTo = (step: OnboardingStep) => { current.value = ONBOARDING_STEPS.indexOf(step) }
const next = () => { if (current.value < ONBOARDING_STEPS.length - 1) current.value++ }
const back = () => { if (current.value > 0) current.value-- }

async function finish() {
  if (!applicant.value) return
  const id = applicant.value.id
  const status = await run(() => completeOnboarding(id))
  if (!status) {
    await showMissingSections(id)
    return
  }
  // the server has recorded it; from here closing the tab still leaves the student onboarded
  markComplete()
  finishing.value = true
}

const toDashboard = () => navigateTo(localePath('/dashboard'))

/** The server refused Finish: name what is missing and take the student back to the first such step. */
async function showMissingSections(id: number) {
  const status = await onboardingStatus(id).catch(() => null)
  const missing = status?.sections.filter(s => !s.complete) ?? []
  if (missing.length === 0) return
  progress.status.value = status
  error.value = t('onboarding.incomplete', {
    sections: missing.map(s => t(`onboarding.sectionName.${s.key}`)).join(', '),
  })
  const firstStep = ONBOARDING_STEPS.find(step => STEP_SECTIONS[step].some(key => missing.some(s => s.key === key)))
  if (firstStep) goTo(firstStep)
}

useSeo(t('onboarding.title'), t('onboarding.intro'))
</script>

<template>
  <OnboardingFinishing v-if="finishing" @done="toDashboard" />
  <div v-else class="mx-auto max-w-2xl">
    <header class="mb-6">
      <h1 class="font-display text-2xl font-bold text-slate-900">{{ t('onboarding.title') }}</h1>
      <p class="mt-1 text-sm text-slate-500">{{ t('onboarding.intro') }}</p>
    </header>

    <OnboardingProgress :steps="progressSteps" :current="current" />

    <NCard>
      <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
      <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>

      <OnboardingStep v-if="stepKey === 'personal'" :title="t('onboarding.personal.title')" :blurb="t('onboarding.personal.blurb')">
        <ProfileForm
          :model-value="applicant"
          :busy="busy"
          :submit-label="t('profileForm.saveContinue')"
          @submit="saveProfile"
          @email-verified="applicant = $event"
        />
      </OnboardingStep>

      <OnboardingStep v-else-if="stepKey === 'identity'" :title="t('onboarding.identity.title')" :blurb="t('onboarding.identity.blurb')">
        <div class="grid gap-4">
          <PhotoUploadCard v-if="applicant" :applicant-id="applicant.id" @changed="progress.refresh" />
          <PassportUploadCard
            v-if="applicant"
            :applicant-id="applicant.id"
            :profile="{ givenName: applicant.givenName, familyName: applicant.familyName, dob: applicant.dob }"
            @changed="progress.refresh"
            @edit-profile="goTo('personal')"
          />
          <NAlert v-if="!canAdvance" tone="warning">{{ t('onboarding.identity.blocked') }}</NAlert>
        </div>
      </OnboardingStep>

      <OnboardingStep v-else-if="stepKey === 'review'" :title="t('onboarding.review.title')" :blurb="t('onboarding.review.blurb')">
        <SuspenseBoundary>
          <ReviewStep v-if="applicant" :applicant="applicant" :status="progress.status.value" @edit="goTo" />
        </SuspenseBoundary>
      </OnboardingStep>

      <OnboardingStep v-else :title="t(`onboarding.${stepKey}.title`)" :blurb="t(`onboarding.${stepKey}.blurb`)">
        <NAlert v-if="!applicant" tone="warning">{{ t('dashboard.createProfileBlurb') }}</NAlert>
        <SuspenseBoundary v-else>
          <component :is="sectionStep" :key="stepKey" :applicant-id="applicant.id" @changed="progress.refresh" />
        </SuspenseBoundary>
      </OnboardingStep>

      <div class="mt-6 flex items-center justify-between gap-3 border-t border-slate-100 pt-4">
        <NButton variant="ghost" size="sm" :disabled="current === 0" @click="back">{{ t('onboarding.back') }}</NButton>
        <NButton v-if="stepKey === 'review'" size="sm" :loading="busy" @click="finish">{{ t('onboarding.finish') }}</NButton>
        <NButton v-else-if="stepKey !== 'personal'" size="sm" :disabled="!canAdvance" @click="next">{{ t('onboarding.next') }}</NButton>
      </div>
    </NCard>
  </div>
</template>
