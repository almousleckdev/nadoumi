<script setup lang="ts">
import type { ApplicantDto, EducationDto } from '~/types/catalog'
import type { SelfApplicantBody, EducationBody } from '~/composables/useApplicant'

definePageMeta({ layout: 'onboarding', middleware: ['auth', 'onboarding'] })
const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId, refresh } = useSession()
const { markComplete } = useOnboarding()
const {
  listMine, get, create, update, completeOnboarding, onboardingStatus,
  listEducation, addEducation, updateEducation, deleteEducation,
} = useApplicant()

const STEPS = [
  { key: 'personal', label: () => t('onboarding.steps.personal') },
  { key: 'identity', label: () => t('onboarding.steps.identity') },
  { key: 'education', label: () => t('onboarding.steps.education') },
  { key: 'interests', label: () => t('onboarding.steps.interests') },
  { key: 'location', label: () => t('onboarding.steps.location') },
  { key: 'contact', label: () => t('onboarding.steps.contact') },
  { key: 'review', label: () => t('onboarding.steps.review') },
] as const
const current = ref(0)
const stepKey = computed(() => STEPS[current.value]!.key)
const progressSteps = computed(() => STEPS.map(s => ({ key: s.key, label: s.label() })))

const applicant = ref<ApplicantDto | null>(null)
const education = ref<EducationDto[]>([])
const { busy, notice, error, run } = useAsyncAction()

async function load() {
  const mine = await listMine().catch(() => [])
  const chosen = mine.find(a => a.id === activeApplicantId.value) ?? mine[0] ?? null
  applicant.value = chosen ? await get(chosen.id) : null
  if (applicant.value) education.value = await listEducation(applicant.value.id).catch(() => [])
}
await load()

async function saveIdentity(body: SelfApplicantBody) {
  const saved = await run(async () => {
    if (applicant.value) applicant.value = await update(applicant.value.id, body)
    else { applicant.value = await create(body); await refresh() }
    return true
  }, t('onboarding.saved'))
  if (saved) next()
}

function onEmailVerified(updated: ApplicantDto) {
  applicant.value = updated
}

function runEdu(fn: () => Promise<unknown>) {
  if (!applicant.value) return
  return run(async () => { await fn(); education.value = await listEducation(applicant.value!.id).catch(() => []) })
}
const onEduAdd = (b: EducationBody) => runEdu(() => addEducation(applicant.value!.id, b))
const onEduUpdate = (id: number, b: EducationBody) => runEdu(() => updateEducation(applicant.value!.id, id, b))
const onEduRemove = (id: number) => runEdu(() => deleteEducation(applicant.value!.id, id))

function next() {
  if (current.value < STEPS.length - 1) current.value++
}
function back() {
  if (current.value > 0) current.value--
}
async function finish() {
  if (!applicant.value) return
  const id = applicant.value.id
  const status = await run(() => completeOnboarding(id))
  if (!status) {
    await showMissingSections(id)
    return
  }
  markComplete()
  await navigateTo(localePath('/dashboard'))
}

/** The server refused Finish: name what is missing and take the student back to it. */
async function showMissingSections(id: number) {
  const status = await onboardingStatus(id).catch(() => null)
  const missing = status?.sections.filter(s => !s.complete) ?? []
  if (missing.length === 0) return
  error.value = t('onboarding.incomplete', {
    sections: missing.map(s => t(`onboarding.sectionName.${s.key}`)).join(', '),
  })
  if (missing.some(s => s.key === 'PROFILE')) current.value = 0
}

useSeo(t('onboarding.title'), t('onboarding.intro'))
</script>

<template>
  <div class="mx-auto max-w-2xl">
    <header class="mb-6">
      <h1 class="font-display text-2xl font-bold text-slate-900">{{ t('onboarding.title') }}</h1>
      <p class="mt-1 text-sm text-slate-500">{{ t('onboarding.intro') }}</p>
    </header>

    <OnboardingProgress :steps="progressSteps" :current="current" />

    <NCard>
      <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
      <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>

      <OnboardingStep
        v-if="stepKey === 'personal'"
        :title="t('onboarding.personal.title')"
        :blurb="t('onboarding.personal.blurb')"
      >
        <ProfileForm
          :model-value="applicant"
          :busy="busy"
          :submit-label="t('profileForm.saveContinue')"
          @submit="saveIdentity"
          @email-verified="onEmailVerified"
        />
      </OnboardingStep>

      <OnboardingStep
        v-else-if="stepKey === 'identity'"
        :title="t('onboarding.identity.title')"
        :blurb="t('onboarding.identity.blurb')"
      >
        <div class="grid gap-4">
          <ProfilePhotoUploadCard />
          <PassportUploadCard />
        </div>
      </OnboardingStep>

      <OnboardingStep
        v-else-if="stepKey === 'education'"
        :title="t('onboarding.education.title')"
        :blurb="t('onboarding.education.blurb')"
      >
        <NAlert v-if="!applicant" tone="warning">{{ t('dashboard.createProfileBlurb') }}</NAlert>
        <EducationList
          v-else
          :items="education"
          :busy="busy"
          @add="onEduAdd"
          @update="onEduUpdate"
          @remove="onEduRemove"
        />
      </OnboardingStep>

      <OnboardingStep
        v-else-if="stepKey === 'interests'"
        :title="t('onboarding.interests.title')"
        :blurb="t('onboarding.interests.blurb')"
        planned
      >
        <p class="text-sm text-slate-500">
          {{ t('onboarding.interests.blurb') }}
        </p>
      </OnboardingStep>

      <OnboardingStep
        v-else-if="stepKey === 'location'"
        :title="t('onboarding.location.title')"
        :blurb="t('onboarding.location.blurb')"
        planned
      >
        <p class="text-sm text-slate-500">Are you currently in China? Yes or No</p>
      </OnboardingStep>

      <OnboardingStep
        v-else-if="stepKey === 'contact'"
        :title="t('onboarding.contact.title')"
        :blurb="t('onboarding.contact.blurb')"
        planned
      >
        <p class="text-sm text-slate-500">Guardian / emergency contact</p>
      </OnboardingStep>

      <OnboardingStep v-else :title="t('onboarding.review.title')" :blurb="t('onboarding.review.blurb')">
        <dl class="grid gap-2 text-sm">
          <div class="flex justify-between gap-4">
            <dt class="text-slate-500">{{ t('dashboard.givenName') }}</dt>
            <dd class="font-medium">{{ applicant?.givenName ?? '' }} {{ applicant?.familyName ?? '' }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-slate-500">{{ t('dashboard.nationality') }}</dt>
            <dd class="font-medium">{{ applicant?.nationality ?? '' }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-slate-500">{{ t('onboarding.steps.education') }}</dt>
            <dd class="font-medium">{{ education.length }}</dd>
          </div>
        </dl>
        <p class="mt-4 text-sm text-slate-500">{{ t('onboarding.review.done') }}</p>
      </OnboardingStep>

      <div class="mt-6 flex items-center justify-between gap-3 border-t border-slate-100 pt-4">
        <NButton variant="ghost" size="sm" :disabled="current === 0" @click="back">{{ t('onboarding.back') }}</NButton>
        <NButton v-if="stepKey === 'review'" size="sm" :loading="busy" @click="finish">{{ t('onboarding.finish') }}</NButton>
        <NButton v-else-if="stepKey !== 'personal'" size="sm" @click="next">{{ t('onboarding.next') }}</NButton>
      </div>
    </NCard>
  </div>
</template>
