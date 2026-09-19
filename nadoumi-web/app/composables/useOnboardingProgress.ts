import type { OnboardingSectionDto, OnboardingStatusDto } from '~/types/catalog'

/** The server's view of which onboarding sections are complete; the source of truth for gating Next. */
export function useOnboardingProgress(applicantId: MaybeRefOrGetter<number | null>) {
  const { onboardingStatus } = useApplicant()
  const status = ref<OnboardingStatusDto | null>(null)

  async function refresh() {
    const id = toValue(applicantId)
    if (id == null) return
    status.value = await onboardingStatus(id).catch(() => null)
  }

  const isComplete = (key: string): boolean =>
    status.value?.sections.find((section: OnboardingSectionDto) => section.key === key)?.complete === true

  return { status, refresh, isComplete }
}
