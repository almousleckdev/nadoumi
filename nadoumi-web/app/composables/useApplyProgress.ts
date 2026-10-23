import type { ApplyStep } from '~/constants/apply'
import { firstOpenStep, openSteps, stepDone, type ProgressFacts } from '~/utils/applyProgress'
import { allMandatoryAttached } from '~/utils/checklistView'

/**
 * Where an application stands, step by step. Profile and form sections come from the server (so progress survives a
 * closed browser and a different device); the documents, the signature and the payment report in from their own cards.
 */
export function useApplyProgress(applicantId: MaybeRefOrGetter<number | null>, applicationId: MaybeRefOrGetter<number | null>) {
  const onboarding = useOnboardingProgress(applicantId)
  const { readiness } = useApplicantFamily()
  const { checklist } = useApplications()
  // composables are called here, in setup: calling them inside `refresh`, after an await, loses the Nuxt context on the server
  const signature = useApplicantSignature(() => toValue(applicantId) ?? 0)
  const payment = useApplicationPayment(() => toValue(applicationId) ?? 0)

  const missingFormSections = ref<string[] | null>(null)
  const documentsReady = ref(false)
  const signatureReady = ref(false)
  const feeSettled = ref(false)

  async function refresh() {
    const id = toValue(applicantId)
    if (id == null) return
    const applicationNo = toValue(applicationId)
    const [, form, items, signed, fee] = await Promise.all([
      onboarding.refresh(),
      readiness(id).catch(() => null),
      applicationNo == null ? null : checklist(applicationNo).catch(() => null),
      signature.status().catch(() => null),
      applicationNo == null ? null : payment.overview().catch(() => null),
    ])
    missingFormSections.value = form?.missing ?? null
    if (items) documentsReady.value = allMandatoryAttached(items)
    if (signed) signatureReady.value = signed.signed
    if (fee) feeSettled.value = fee.settled
  }

  const facts = computed<ProgressFacts>(() => ({
    hasApplicant: toValue(applicantId) !== null,
    sectionComplete: onboarding.isComplete,
    missingFormSections: missingFormSections.value,
    documentsReady: documentsReady.value,
    signatureReady: signatureReady.value,
    feeSettled: feeSettled.value,
  }))

  const isDone = (step: ApplyStep) => stepDone(step, facts.value)
  const firstOpen = () => firstOpenStep(facts.value)
  const open = computed<ApplyStep[]>(() => openSteps(facts.value))

  return { onboarding, documentsReady, signatureReady, feeSettled, refresh, isDone, firstOpenStep: firstOpen, openSteps: open }
}
