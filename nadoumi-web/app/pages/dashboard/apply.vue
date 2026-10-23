<script setup lang="ts">
import type { ApplyTarget } from '~/types/apply'
import type { ApplicantDto, StudentApplicationDto } from '~/types/catalog'
import { findDraft, parseApplyQuery, startBody } from '~/utils/applyView'

definePageMeta({ layout: 'dashboard', middleware: ['auth', 'onboarding'] })

const { t } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { activeApplicantId } = useSession()
const { load: loadTarget } = useApplyTarget()
const { list, start } = useApplications()
const { listMine } = useApplicant()
const { busy, error, run } = useAsyncAction()

const query = parseApplyQuery(route.query)
const target = ref<ApplyTarget | null>(null)
const applicants = ref<ApplicantDto[]>([])
const applications = ref<StudentApplicationDto[]>([])
const pending = ref(true)
const loadError = ref('')

const applicantId = ref<number | null>(null)
const intakeId = ref('')
const intakeMissing = ref(false)

async function load() {
  if (!query) {
    pending.value = false
    return
  }
  try {
    const [opportunity, students, mine] = await Promise.all([loadTarget(query), listMine(), list()])
    target.value = opportunity
    applicants.value = students
    applications.value = mine
    applicantId.value = students.find(a => a.id === activeApplicantId.value)?.id ?? students[0]?.id ?? null
    if (opportunity?.kind === 'programme' && opportunity.intakes.length === 1) {
      intakeId.value = String(opportunity.intakes[0]!.id)
    }
  }
  catch {
    loadError.value = t('errors.loadSection')
  }
  finally {
    pending.value = false
  }
}
await load()

const needsIntake = computed(() => target.value?.kind === 'programme')
const noIntakeOpen = computed(() => target.value?.kind === 'programme' && target.value.intakes.length === 0)
const draft = computed(() =>
  target.value && applicantId.value !== null ? findDraft(applications.value, target.value, applicantId.value) : undefined)
const intakeOptions = computed(() =>
  target.value?.kind === 'programme'
    ? target.value.intakes.map((i: { id: number, term: string }) => ({ value: String(i.id), label: t(`program.intake.${i.term}`, i.term) }))
    : [])
const backTo = computed(() => localePath(query ? `/${query.kind === 'programme' ? 'programs' : 'scholarships'}/${query.slug}` : '/'))

async function onStart() {
  if (!target.value || applicantId.value === null) return
  intakeMissing.value = needsIntake.value && !intakeId.value
  if (intakeMissing.value) return
  const created = await run(() => start(startBody(target.value!, applicantId.value!, intakeId.value ? Number(intakeId.value) : null)))
  if (created) await navigateTo(localePath(`/dashboard/applications/${created.id}`))
}

useSeo(t('apply.title'), t('apply.blurb'))
</script>

<template>
  <div class="mx-auto grid w-full max-w-2xl gap-6">
    <NuxtLink :to="backTo" class="text-sm font-medium text-slate-700 no-underline hover:underline">
      {{ t('apply.back') }}
    </NuxtLink>

    <NAlert v-if="!query" tone="warning">{{ t('apply.noTarget') }}</NAlert>
    <NAlert v-else-if="loadError" tone="danger">
      {{ loadError }}
      <button type="button" class="ms-2 font-medium underline" @click="load()">{{ t('common.retry') }}</button>
    </NAlert>
    <NAlert v-else-if="!pending && !target" tone="warning">{{ t('apply.notFound') }}</NAlert>
    <NSkeleton v-else-if="pending" class="h-48 w-full rounded-xl" />

    <template v-else-if="target">
      <header>
        <p class="text-sm text-slate-500">{{ t('apply.title') }}</p>
        <h1 class="font-display text-2xl font-bold text-slate-900" data-test="target-title">{{ target.title }}</h1>
        <p v-if="target.kind === 'programme' && target.universityName" class="mt-1 text-slate-600" data-test="target-university">
          {{ target.universityName }}
        </p>
      </header>

      <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

      <SectionCard :title="t('apply.studentTitle')">
        <p v-if="!applicants.length" class="text-sm text-slate-500">{{ t('apply.noStudents') }}</p>
        <ApplyStudentPicker v-else v-model="applicantId" :applicants="applicants" />
      </SectionCard>

      <SectionCard v-if="needsIntake" :title="t('apply.intakeTitle')">
        <NAlert v-if="noIntakeOpen" tone="warning">{{ t('apply.noIntake') }}</NAlert>
        <NField v-else for="apply-intake" :label="t('apply.intakeLabel')" :error="intakeMissing ? t('validation.required') : ''" required>
          <NSelect
            id="apply-intake"
            v-model="intakeId"
            :options="intakeOptions"
            :placeholder="t('apply.intakePlaceholder')"
            :invalid="intakeMissing"
          />
        </NField>
      </SectionCard>

      <SectionCard :title="t('apply.nextTitle')">
        <p class="text-sm text-slate-600">{{ t('apply.nextBody') }}</p>
        <div class="mt-4 flex flex-wrap items-center gap-3">
          <NButton v-if="draft" :to="localePath(`/dashboard/applications/${draft.id}`)" data-test="continue-draft">
            {{ t('apply.continueDraft') }}
          </NButton>
          <NButton
            v-else
            :loading="busy"
            :disabled="applicantId === null || noIntakeOpen"
            data-test="start"
            @click="onStart"
          >
            {{ t('apply.start') }}
          </NButton>
        </div>
        <p v-if="draft" class="mt-2 text-sm text-slate-500">{{ t('apply.draftExists') }}</p>
      </SectionCard>
    </template>
  </div>
</template>
