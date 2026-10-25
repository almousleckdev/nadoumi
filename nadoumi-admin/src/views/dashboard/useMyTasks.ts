import { computed } from 'vue'
import { listTasks, type Task } from '@/api/hr'
import { openTasks } from './dashboardMath'
import { useGatedLoader } from './useGatedLoader'
import type { useDashboardAccess } from './useDashboardAccess'

const TASK_SAMPLE_SIZE = 50
const TASKS_SHOWN = 6

type Access = ReturnType<typeof useDashboardAccess>

/** The caller's own open tasks, soonest due first, with how many are already overdue. */
export function useMyTasks(access: Access, userId: () => number | null, today: () => string) {
  const loader = useGatedLoader(
    [] as Task[],
    () => access.canSeeTasks.value && userId() !== null,
    // `mine` on this endpoint means created by me; what a person has to do is the work assigned to them
    async () => openTasks((await listTasks({ assigneeUserId: userId() ?? undefined, page: 1, size: TASK_SAMPLE_SIZE })).content),
  )

  const sorted = computed(() => [...loader.data.value].sort((a, b) =>
    (a.dueDate ?? '9999-12-31').localeCompare(b.dueDate ?? '9999-12-31')))
  const overdue = computed(() => loader.data.value.filter(task => task.dueDate !== null && task.dueDate < today()).length)

  return { ...loader, shown: computed(() => sorted.value.slice(0, TASKS_SHOWN)), total: computed(() => loader.data.value.length), overdue }
}
