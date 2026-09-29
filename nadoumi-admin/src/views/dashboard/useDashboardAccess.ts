import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

export function useDashboardAccess() {
  const userStore = useUserStore()

  const canSeeApplicants = computed(() => userStore.hasPerm('nad:applicant:list'))
  const canSeeFinance = computed(() => userStore.hasPerm('nad:finance:view'))
  const canSeeTasks = computed(() => userStore.hasPerm('nad:task:list'))
  const canSeeCatalog = computed(() => userStore.hasPerm('nad:program:list'))
  const canSeeUniversities = computed(() => userStore.hasPerm('nad:university:list'))
  const canSeeScholarships = computed(() => userStore.hasPerm('nad:scholarship:list'))
  const canSeeStudents = computed(() => userStore.hasPerm('system:user:list'))
  const isOwner = computed(() => userStore.hasPerm('*:*:*'))

  const showPlatform = computed(() =>
    canSeeStudents.value || canSeeApplicants.value || canSeeUniversities.value
    || canSeeCatalog.value || canSeeScholarships.value)
  const nothingVisible = computed(() => !showPlatform.value && !canSeeFinance.value && !canSeeTasks.value)

  return {
    canSeeApplicants, canSeeFinance, canSeeTasks, canSeeCatalog, canSeeUniversities,
    canSeeScholarships, canSeeStudents, isOwner, showPlatform, nothingVisible,
  }
}
