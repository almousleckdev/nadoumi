<script setup lang="ts">
import type { ApplyStep } from '~/constants/apply'
import { APPLY_STEPS, OPTIONAL_APPLY_STEPS, isApplyStep } from '~/constants/apply'
import type { SelfApplicantBody } from '~/composables/useApplicant'
import type { ApplyTarget } from '~/types/apply'
import type { ApplicantDto, StudentApplicationDto } from '~/types/catalog'
import type { OnboardingStepView } from '~/types/onboarding'
import { parseApplyQuery } from '~/utils/applyView'

// The application is its own step-by-step flow, outside the dashboard: a fixed shell where the step list and the
// action bar stay put and only the content scrolls. Everything is saved on the server as it goes, so a student can
// leave and continue later, on any device.
// Signed in is all it asks: the account's own onboarding is not a prerequisite for applying for someone else, and for
// themselves the student completes it in the steps below.
definePageMeta({ layout: 'onboarding', middleware: ['auth'] })

const { t } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { listMine, get: getApplicant, update: updateApplicant, completeOnboarding } = useApplicant()
const { list: listApplications, get: getApplication } = useApplications()
const { load: loadTarget } = useApplyTarget()
const { applicants: sessionApplicants, refresh: refreshSession } = useSession()
const { busy, error, run } = useAsyncAction()

const applicationId = computed(() => {
  const raw = route.params.id
  const id = Number(Array.isArray(raw) ? raw[0] : raw)
  return Number.isInteger(id) && id > 0 ? id : null
})

const applicants = ref<ApplicantDto[]>([])
const applications = ref<StudentApplicationDto[]>([])
const application = ref<StudentApplicationDto | null>(null)
const applicant = ref<ApplicantDto | null>(null)
const target = ref<ApplyTarget | null>(null)
const loadError = ref('')
const submittedReference = ref<string | null>(null)
const mainEl = ref<HTMLElement | null>(null)

const progress = useApplyProgress(() => applicant.value?.id ?? null, applicationId)

async function load() {
  loadError.value = ''
  try {
    const [mine, drafts] = await Promise.all([listMine(), listApplications()])
    applicants.value = mine
    applications.value = drafts
    if (applicationId.value !== null) {
      application.value = await getApplication(applicationId.value)
      applicant.value = await getApplicant(application.value.applicantId)
      target.value = await loadTarget(application.value.programId !== null
        ? { kind: 'programme', slug: String(application.value.programId) }
        : { kind: 'scholarship', slug: String(application.value.scholarshipId) })
      await progress.refresh()
    }
    else {
      const query = parseApplyQuery(route.query)
      target.value = query ? await loadTarget(query) : null
    }
  }
  catch {
    loadError.value = t('errors.loadSection')
  }
}
await load()

/** The server says where a returning student stands; a `?step=` in the address (a refresh, the back button) wins. */
const current = ref<ApplyStep>(applicationId.value === null
  ? 'start'
  : isApplyStep(route.query.step) ? route.query.step : progress.firstOpenStep())

const currentIndex = computed(() => APPLY_STEPS.indexOf(current.value))
const started = computed(() => applicationId.value !== null && application.value !== null)
const isOptional = computed(() => OPTIONAL_APPLY_STEPS.includes(current.value))

const stepViews = computed<OnboardingStepView[]>(() => APPLY_STEPS.map(step => ({
  key: step,
  label: t(`applyFlow.steps.${step}`),
  optional: OPTIONAL_APPLY_STEPS.includes(step),
  done: step === 'start' ? started.value : started.value ? progress.isDone(step) : false,
})))

const studentName = computed(() => applicant.value ? `${applicant.value.givenName} ${applicant.value.familyName}`.trim() : null)
const reference = computed(() => application.value?.reference ?? null)
// an agent completing someone else's details: the forms say "you", so the page says whom they mean
const onBehalf = computed(() =>
  applicant.value !== null && sessionApplicants.value.find(a => a.applicantId === applicant.value!.id)?.accessRole === 'AGENT')
const canAdvance = computed(() => started.value && (current.value === 'start' || isOptional.value || progress.isDone(current.value)))

function moveTo(step: ApplyStep) {
  if (!started.value && step !== 'start') return
  current.value = step
  mainEl.value?.scrollTo({ top: 0, behavior: 'smooth' })
  if (applicationId.value !== null) navigateTo({ query: { ...route.query, step } }, { replace: true })
}
const moveToIndex = (index: number) => moveTo(APPLY_STEPS[index] as ApplyStep)
const next = () => { if (currentIndex.value < APPLY_STEPS.length - 1) moveToIndex(currentIndex.value + 1) }
const back = () => { if (currentIndex.value > 0) moveToIndex(currentIndex.value - 1) }

async function onStarted(id: number) {
  await navigateTo(localePath({ path: `/apply/${id}`, query: { step: 'profile' } }))
}

async function onStudentAdded(student: ApplicantDto) {
  applicants.value = [...applicants.value, student]
  await refreshSession() // the session now lists the new student, with the caller as their agent
}

async function saveProfile(body: SelfApplicantBody) {
  if (!applicant.value) return
  const id = applicant.value.id
  const saved = await run(async () => {
    applicant.value = await updateApplicant(id, body)
    return true
  }, t('onboarding.saved'))
  await progress.refresh()
  if (saved) next()
}

/** Before the review the server must hold the student as onboarded: it is what lets the application be submitted. */
async function ensureOnboarded() {
  const id = applicant.value?.id
  if (id === undefined || applicant.value?.onboardingComplete || !progress.onboarding.status.value?.ready) return
  if (await completeOnboarding(id).catch(() => null)) applicant.value = await getApplicant(id)
}
watch(current, (step: ApplyStep) => { if (step === 'review') ensureOnboarded() })
if (current.value === 'review') await ensureOnboarded()

function onSubmitted(submitted: StudentApplicationDto) {
  submittedReference.value = submitted.reference
}

useSeo(t('applyFlow.title'), t('applyFlow.blurb'))
</script>

<template>
  <ApplySubmitted v-if="submittedReference !== null" :reference="submittedReference" />
  <div v-else-if="loadError" class="mx-auto max-w-xl px-4 py-16">
    <NAlert tone="danger">
      {{ loadError }}
      <button type="button" class="ms-2 font-medium underline" @click="load()">{{ t('common.retry') }}</button>
    </NAlert>
  </div>
  <div v-else-if="!target" class="mx-auto grid max-w-xl gap-4 px-4 py-16">
    <NAlert tone="warning">{{ parseApplyQuery(route.query) || applicationId !== null ? t('apply.notFound') : t('apply.noTarget') }}</NAlert>
    <NButton :to="localePath('/programs')" class="justify-self-start">{{ t('applyFlow.browse') }}</NButton>
  </div>
  <div v-else class="flex h-dvh overflow-hidden">
    <SidebarFrame class="hidden w-[268px] shrink-0 lg:flex">
      <ApplyContext
        :title="target.title"
        :university-name="target.kind === 'programme' ? target.universityName : null"
        :student-name="studentName"
        :reference="reference"
      />
      <OnboardingStepper :steps="stepViews" :current="currentIndex" @select="moveToIndex" />
      <div class="shrink-0 border-t border-white/10 px-5 py-3">
        <NuxtLink :to="localePath('/dashboard/applications')" class="text-sm text-[var(--sidebar-fg)] no-underline hover:text-white" data-test="save-exit">
          {{ t('applyFlow.exit') }}
        </NuxtLink>
      </div>
    </SidebarFrame>

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="shrink-0 border-b border-slate-200 bg-white px-4 py-3 lg:hidden">
        <OnboardingProgress :steps="stepViews" :current="currentIndex" @select="moveToIndex" />
      </header>

      <main ref="mainEl" class="min-h-0 flex-1 overflow-y-auto">
        <div class="mx-auto w-full max-w-3xl px-4 py-6 sm:px-8 lg:py-10">
          <NAlert v-if="onBehalf && started && current !== 'start' && current !== 'review'" tone="info" class="mb-4" data-test="on-behalf">
            {{ t('applyFlow.onBehalf', { name: studentName ?? '' }) }}
          </NAlert>
          <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>

          <Transition name="step" mode="out-in">
            <OnboardingStep
              :key="current"
              :eyebrow="t('onboarding.stepCount', { n: currentIndex + 1, total: APPLY_STEPS.length })"
              :optional="isOptional"
              :title="t(`applyFlow.steps.${current}`)"
              :blurb="t(`applyFlow.blurbs.${current}`)"
            >
              <ApplyStartStep
                v-if="current === 'start' && !started"
                :target="target"
                :applicants="applicants"
                :applications="applications"
                @started="onStarted"
                @student-added="onStudentAdded"
              />
              <div v-else-if="current === 'start'" class="grid gap-3" data-test="started-summary">
                <p class="text-slate-700">{{ t('applyFlow.start.savedAs', { reference: reference ?? '' }) }}</p>
              </div>

              <template v-else-if="applicant">
                <ProfileForm
                  v-if="current === 'profile'"
                  :model-value="applicant"
                  :busy="busy"
                  :submit-label="t('profileForm.saveContinue')"
                  @submit="saveProfile"
                  @email-verified="applicant = $event"
                />
                <div v-else-if="current === 'identity'" class="grid gap-5">
                  <PhotoUploadCard :applicant-id="applicant.id" @changed="progress.refresh" />
                  <PassportUploadCard :applicant-id="applicant.id" @changed="progress.refresh" @edit-profile="moveTo('profile')" />
                </div>
                <SuspenseBoundary v-else-if="current === 'address'">
                  <div class="grid gap-8">
                    <section><h3 class="mb-3 font-display text-lg font-semibold text-slate-900">{{ t('applyFlow.sections.currentAddress') }}</h3>
                      <ResidenceSection :applicant-id="applicant.id" @changed="progress.refresh" /></section>
                    <section><h3 class="mb-3 font-display text-lg font-semibold text-slate-900">{{ t('applyFlow.sections.permanentAddress') }}</h3>
                      <PersonalDetailsSection :applicant-id="applicant.id" @changed="progress.refresh" /></section>
                    <section><h3 class="mb-3 font-display text-lg font-semibold text-slate-900">{{ t('applyFlow.sections.contacts') }}</h3>
                      <ContactSection :applicant-id="applicant.id" @changed="progress.refresh" /></section>
                  </div>
                </SuspenseBoundary>
                <SuspenseBoundary v-else-if="current === 'education'"><EducationSection :applicant-id="applicant.id" @changed="progress.refresh" /></SuspenseBoundary>
                <SuspenseBoundary v-else-if="current === 'work'"><WorkSection :applicant-id="applicant.id" @changed="progress.refresh" /></SuspenseBoundary>
                <SuspenseBoundary v-else-if="current === 'family'"><FamilyMemberSection :applicant-id="applicant.id" @changed="progress.refresh" /></SuspenseBoundary>
                <SuspenseBoundary v-else-if="current === 'support'">
                  <div class="grid gap-8">
                    <section><h3 class="mb-1 font-display text-lg font-semibold text-slate-900">{{ t('family.supporter.title') }}</h3>
                      <p class="mb-3 text-sm text-slate-500">{{ t('family.supporter.hint') }}</p>
                      <PersonSection :applicant-id="applicant.id" role="supporter" @changed="progress.refresh" /></section>
                    <section><h3 class="mb-1 font-display text-lg font-semibold text-slate-900">{{ t('family.chinaEmergency.title') }}</h3>
                      <p class="mb-3 text-sm text-slate-500">{{ t('family.chinaEmergency.hint') }}</p>
                      <PersonSection :applicant-id="applicant.id" role="china-emergency" @changed="progress.refresh" /></section>
                  </div>
                </SuspenseBoundary>
                <SuspenseBoundary v-else-if="current === 'study'"><InterestsSection :applicant-id="applicant.id" @changed="progress.refresh" /></SuspenseBoundary>
                <DocumentChecklistCard
                  v-else-if="current === 'documents' && applicationId !== null"
                  :application-id="applicationId"
                  :applicant-id="applicant.id"
                  @ready="progress.documentsReady.value = $event"
                />
                <SignatureCard v-else-if="current === 'signature'" :applicant-id="applicant.id" @ready="progress.signatureReady.value = $event" />
                <ApplyReviewStep
                  v-else-if="current === 'review' && applicationId !== null"
                  :application-id="applicationId"
                  :open-steps="progress.openSteps.value"
                  :fee-settled="progress.feeSettled.value"
                  @goto="moveTo"
                  @settled="progress.feeSettled.value = $event"
                  @submitted="onSubmitted"
                />
              </template>
            </OnboardingStep>
          </Transition>
        </div>
      </main>

      <footer v-if="started && current !== 'review'" class="shrink-0 border-t border-slate-200 bg-white">
        <div class="mx-auto flex w-full max-w-3xl items-center justify-between gap-3 px-4 py-3 sm:px-8">
          <NButton variant="ghost" :disabled="currentIndex === 0" data-test="back" @click="back">{{ t('applyFlow.back') }}</NButton>
          <NButton :disabled="!canAdvance" data-test="next" @click="next">{{ t('applyFlow.next') }}</NButton>
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
