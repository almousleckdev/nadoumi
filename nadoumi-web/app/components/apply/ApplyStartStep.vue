<script setup lang="ts">
import type { ApplyTarget } from '~/types/apply'
import type { ApplicantDto, StudentApplicationDto } from '~/types/catalog'
import { findDraft, startBody } from '~/utils/applyView'

/**
 * Step one of the application: what it is for, who it is for (a student the account already acts for, or a new one)
 * and, for a programme, the intake. Starting saves a draft and opens the rest of the steps.
 */
const props = defineProps<{ target: ApplyTarget, applicants: ApplicantDto[], applications: StudentApplicationDto[] }>()
const emit = defineEmits<{ started: [applicationId: number], 'student-added': [applicant: ApplicantDto] }>()

const { t } = useI18n()
const { activeApplicantId } = useSession()
const { start } = useApplications()
const { busy, error, run } = useAsyncAction()

const applicantId = ref<number | null>(
  props.applicants.find(a => a.id === activeApplicantId.value)?.id ?? props.applicants[0]?.id ?? null)
const intakeId = ref('')
const intakeMissing = ref(false)
const addingStudent = ref(false)

const needsIntake = computed(() => props.target.kind === 'programme')
const intakes = computed(() => props.target.kind === 'programme' ? props.target.intakes : [])
const noIntakeOpen = computed(() => needsIntake.value && intakes.value.length === 0)
const intakeOptions = computed(() => intakes.value.map((i: { id: number, term: string }) => ({ value: String(i.id), label: t(`program.intake.${i.term}`, i.term) })))
if (intakes.value.length === 1) intakeId.value = String(intakes.value[0]!.id)

const draft = computed(() => applicantId.value === null ? undefined : findDraft(props.applications, props.target, applicantId.value))

function onStudentAdded(applicant: ApplicantDto) {
  addingStudent.value = false
  applicantId.value = applicant.id
  emit('student-added', applicant)
}

async function onStart() {
  if (applicantId.value === null) return
  if (draft.value) return emit('started', draft.value.id)
  intakeMissing.value = needsIntake.value && !intakeId.value
  if (intakeMissing.value) return
  const created = await run(() => start(startBody(props.target, applicantId.value!, intakeId.value ? Number(intakeId.value) : null)))
  if (created) emit('started', created.id)
}
</script>

<template>
  <div class="grid gap-6">
    <div class="rounded-xl border border-slate-200 p-5">
      <p class="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">{{ t('applyFlow.context.applyingFor') }}</p>
      <h3 class="mt-1 font-display text-xl font-bold text-slate-900" data-test="target-title">{{ target.title }}</h3>
      <p v-if="target.kind === 'programme' && target.universityName" class="mt-1 text-slate-600" data-test="target-university">
        {{ target.universityName }}
      </p>
    </div>

    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

    <section class="grid gap-3">
      <h3 class="font-display text-lg font-semibold text-slate-900">{{ t('applyFlow.start.whoTitle') }}</h3>
      <p class="text-sm text-slate-600">{{ t('applyFlow.start.whoHint') }}</p>
      <ApplyStudentPicker v-if="applicants.length" v-model="applicantId" :applicants="applicants" />
      <ApplyNewStudentForm v-if="addingStudent" @created="onStudentAdded" @cancel="addingStudent = false" />
      <div v-else>
        <NButton variant="secondary" size="sm" data-test="add-student" @click="addingStudent = true">
          {{ t('applyFlow.start.anotherStudent') }}
        </NButton>
      </div>
    </section>

    <section v-if="needsIntake" class="grid gap-3">
      <h3 class="font-display text-lg font-semibold text-slate-900">{{ t('apply.intakeTitle') }}</h3>
      <NAlert v-if="noIntakeOpen" tone="warning">{{ t('apply.noIntake') }}</NAlert>
      <NField v-else for="apply-intake" :label="t('apply.intakeLabel')" :error="intakeMissing ? t('validation.required') : ''" required>
        <NSelect id="apply-intake" v-model="intakeId" :options="intakeOptions" :placeholder="t('apply.intakePlaceholder')" :invalid="intakeMissing" />
      </NField>
    </section>

    <div class="flex flex-wrap items-center gap-3">
      <NButton :loading="busy" :disabled="applicantId === null || noIntakeOpen" data-test="start" @click="onStart">
        {{ draft ? t('apply.continueDraft') : t('applyFlow.start.begin') }}
      </NButton>
      <p v-if="draft" class="text-sm text-slate-500">{{ t('apply.draftExists') }}</p>
    </div>
  </div>
</template>
