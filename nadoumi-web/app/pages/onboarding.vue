<script setup lang="ts">
import { computed, ref, type Component, type ComputedRef } from 'vue'
import logoUrl from '~/assets/images/logo.jpg'
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
const progressSteps = computed(() => ONBOARDING_STEPS.map(key => ({
  key, label: t(`onboarding.steps.${key}`), optional: STEP_SECTIONS[key].length === 0 && key !== 'review',
})))
/** The furthest step reached so far: finished steps can be revisited from the sidebar, later ones cannot. */
const reached = ref(0)
const sectionStep = computed(() => SECTION_STEPS[stepKey.value] ?? null)

const applicant = ref<ApplicantDto | null>(null)
const finishing = ref(false)
const { busy, error, run } = useAsyncAction()
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

const moveTo = (index: number) => {
  current.value = index
  reached.value = Math.max(reached.value, index)
  if (import.meta.client) window.scrollTo({ top: 0, behavior: 'smooth' })
}
const goTo = (step: OnboardingStep) => moveTo(ONBOARDING_STEPS.indexOf(step))
const next = () => { if (current.value < ONBOARDING_STEPS.length - 1) moveTo(current.value + 1) }
const back = () => { if (current.value > 0) moveTo(current.value - 1) }
const eyebrow = computed(() => t('onboarding.eyebrow', { n: current.value + 1, total: ONBOARDING_STEPS.length }))
const isOptionalStep = computed(() => STEP_SECTIONS[stepKey.value].length === 0 && stepKey.value !== 'review')

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
  <div v-else class="lg:grid lg:min-h-screen lg:grid-cols-[19rem_minmax(0,1fr)]">
    <aside class="relative hidden overflow-hidden bg-[var(--ink-900)] text-white lg:flex lg:flex-col lg:p-8">
      <div class="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-brand-900/40" aria-hidden="true" />
      <div class="relative flex flex-1 flex-col lg:sticky lg:top-8 lg:max-h-[calc(100vh-4rem)]">
        <div class="flex items-center gap-3">
          <img :src="logoUrl" alt="Nadoumi" class="h-10 w-10 rounded-xl bg-white object-contain p-1">
          <span class="font-display text-lg font-bold tracking-tight">Nadoumi</span>
        </div>
        <h1 class="mt-8 font-display text-xl font-bold leading-snug">{{ t('onboarding.title') }}</h1>
        <p class="mt-2 text-sm leading-relaxed text-slate-300">{{ t('onboarding.intro') }}</p>
        <div class="mt-8">
          <OnboardingStepper :steps="progressSteps" :current="current" :reached="reached" @select="moveTo" />
        </div>
        <div class="mt-auto rounded-xl bg-white/5 p-4 text-sm">
          <p class="font-semibold">{{ t('onboarding.helpTitle') }}</p>
          <p class="mt-1 text-slate-300">{{ t('onboarding.helpBody') }}</p>
        </div>
      </div>
    </aside>

    <div class="flex min-h-screen flex-col">
      <header class="border-b border-slate-200 bg-white px-4 py-3 lg:hidden">
        <div class="mb-3 flex items-center gap-2.5">
          <img :src="logoUrl" alt="Nadoumi" class="h-8 w-8 rounded-lg object-contain">
          <span class="font-display text-base font-bold text-slate-900">{{ t('onboarding.title') }}</span>
        </div>
        <OnboardingProgress :steps="progressSteps" :current="current" />
      </header>

      <main class="mx-auto w-full max-w-3xl flex-1 px-4 pb-28 pt-6 sm:px-8 lg:pt-12">
        <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>

        <Transition name="step" mode="out-in">
          <div :key="stepKey">
            <OnboardingStep v-if="stepKey === 'personal'" :eyebrow="eyebrow" :title="t('onboarding.personal.title')" :blurb="t('onboarding.personal.blurb')">
              <ProfileForm
                :model-value="applicant"
                :busy="busy"
                :submit-label="t('profileForm.saveContinue')"
                @submit="saveProfile"
                @email-verified="applicant = $event"
              />
            </OnboardingStep>

            <OnboardingStep v-else-if="stepKey === 'identity'" :eyebrow="eyebrow" :title="t('onboarding.identity.title')" :blurb="t('onboarding.identity.blurb')">
              <div class="grid gap-5">
                <PhotoUploadCard v-if="applicant" :applicant-id="applicant.id" @changed="progress.refresh" />
                <PassportUploadCard
                  v-if="applicant"
                  :applicant-id="applicant.id"
                  :profile="{ givenName: applicant.givenName, familyName: applicant.familyName, dob: applicant.dob }"
                  @changed="progress.refresh"
                  @edit-profile="goTo('personal')"
                />
              </div>
            </OnboardingStep>

            <OnboardingStep v-else-if="stepKey === 'review'" :eyebrow="eyebrow" :title="t('onboarding.review.title')" :blurb="t('onboarding.review.blurb')">
              <SuspenseBoundary>
                <ReviewStep v-if="applicant" :applicant="applicant" :status="progress.status.value" @edit="goTo" />
              </SuspenseBoundary>
            </OnboardingStep>

            <OnboardingStep v-else :eyebrow="eyebrow" :optional="isOptionalStep" :title="t(`onboarding.${stepKey}.title`)" :blurb="t(`onboarding.${stepKey}.blurb`)">
              <NAlert v-if="!applicant" tone="warning">{{ t('dashboard.createProfileBlurb') }}</NAlert>
              <SuspenseBoundary v-else>
                <component :is="sectionStep" :key="stepKey" :applicant-id="applicant.id" @changed="progress.refresh" />
              </SuspenseBoundary>
            </OnboardingStep>
          </div>
        </Transition>
      </main>

      <footer class="sticky bottom-0 z-10 border-t border-slate-200 bg-white/90 backdrop-blur">
        <div class="mx-auto flex w-full max-w-3xl items-center justify-between gap-3 px-4 py-3 sm:px-8">
          <NButton variant="ghost" :disabled="current === 0" @click="back">{{ t('onboarding.back') }}</NButton>
          <NButton v-if="stepKey === 'review'" :loading="busy" @click="finish">{{ t('onboarding.finish') }}</NButton>
          <NButton v-else-if="stepKey !== 'personal'" :disabled="!canAdvance" @click="next">{{ t('onboarding.next') }}</NButton>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.step-enter-active, .step-leave-active { transition: opacity 0.22s ease, transform 0.22s ease; }
.step-enter-from { opacity: 0; transform: translateY(14px); }
.step-leave-to { opacity: 0; transform: translateY(-8px); }
@media (prefers-reduced-motion: reduce) {
  .step-enter-active, .step-leave-active { transition: opacity 0.01s; }
  .step-enter-from, .step-leave-to { transform: none; }
}
</style>
