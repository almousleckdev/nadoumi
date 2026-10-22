<script setup lang="ts">
import { computed, ref, type Component, type ComputedRef } from 'vue'
import type { ApplicantDto } from '~/types/catalog'
import type { SelfApplicantBody } from '~/composables/useApplicant'
import ContactSection from '~/components/applicant/ContactSection.vue'
import EducationSection from '~/components/applicant/EducationSection.vue'
import InterestsSection from '~/components/applicant/InterestsSection.vue'
import ResidenceSection from '~/components/applicant/ResidenceSection.vue'
import WorkSection from '~/components/applicant/WorkSection.vue'
import { ONBOARDING_STEPS, STEP_SECTIONS, type OnboardingStep } from '~/constants/onboarding'
import type { OnboardingStepView } from '~/types/onboarding'
import { resumeStepIndex } from '~/utils/onboardingResume'

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
const sectionStep = computed(() => SECTION_STEPS[stepKey.value] ?? null)
const mainEl = ref<HTMLElement | null>(null)


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
// a returning student continues where they stopped; the server's section status is the record of that
current.value = resumeStepIndex(applicant.value !== null, progress.isComplete)

/** A step is done once the server reports all its sections complete; steps without sections are not tracked. */
const progressSteps = computed<OnboardingStepView[]>(() => ONBOARDING_STEPS.map((key) => {
  const sections = STEP_SECTIONS[key]
  return {
    key, label: t(`onboarding.steps.${key}`), optional: sections.length === 0 && key !== 'review',
    done: sections.length ? sections.every(progress.isComplete) : null,
  }
}))

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
  mainEl.value?.scrollTo({ top: 0, behavior: 'smooth' })
}
const goTo = (step: OnboardingStep) => moveTo(ONBOARDING_STEPS.indexOf(step))
const next = () => { if (current.value < ONBOARDING_STEPS.length - 1) moveTo(current.value + 1) }
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
  <div v-else class="flex h-dvh overflow-hidden">
    <SidebarFrame class="hidden w-[244px] shrink-0 lg:flex">
      <OnboardingStepper :steps="progressSteps" :current="current" @select="moveTo" />
    </SidebarFrame>

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="shrink-0 border-b border-slate-200 bg-white px-4 py-3 lg:hidden">
        <OnboardingProgress :steps="progressSteps" :current="current" @select="moveTo" />
      </header>

      <main ref="mainEl" class="min-h-0 flex-1 overflow-y-auto">
       <div class="mx-auto w-full max-w-3xl px-4 py-6 sm:px-8 lg:py-10">
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
       </div>
      </main>

      <footer v-if="stepKey !== 'personal'" class="shrink-0 border-t border-slate-200 bg-white">
        <div class="mx-auto flex w-full max-w-3xl items-center justify-end gap-3 px-4 py-3 sm:px-8">
          <NButton v-if="stepKey === 'review'" :loading="busy" @click="finish">{{ t('onboarding.finish') }}</NButton>
          <NButton v-else :disabled="!canAdvance" @click="next">{{ t('onboarding.next') }}</NButton>
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
