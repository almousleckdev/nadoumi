import { ref } from 'vue'
import { listUsers } from '@/api/system'
import { listUniversities } from '@/api/university'
import { listPrograms } from '@/api/program'
import { listScholarships } from '@/api/scholarship'
import type { useDashboardAccess } from './useDashboardAccess'

const STUDENT_USER_TYPE = '10'

type CountKey = 'students' | 'universities' | 'programmes' | 'scholarships'
type Access = ReturnType<typeof useDashboardAccess>

export function usePlatformCounts(access: Access) {
  const counts = ref<Record<CountKey, number | null>>({
    students: null, universities: null, programmes: null, scholarships: null,
  })

  async function grab(key: CountKey, request: Promise<{ total?: number, totalElements?: number }>) {
    try {
      const page = await request
      counts.value[key] = page.total ?? page.totalElements ?? 0
    }
    catch {
      counts.value[key] = null
    }
  }

  async function load() {
    const jobs: Promise<void>[] = []
    if (access.canSeeStudents.value) jobs.push(grab('students', listUsers({ userType: STUDENT_USER_TYPE, pageNum: 1, pageSize: 1 })))
    if (access.canSeeUniversities.value) jobs.push(grab('universities', listUniversities({ page: 0, size: 1 })))
    if (access.canSeeCatalog.value) jobs.push(grab('programmes', listPrograms({ page: 0, size: 1 })))
    if (access.canSeeScholarships.value) jobs.push(grab('scholarships', listScholarships({ page: 0, size: 1 })))
    await Promise.all(jobs)
  }

  return { counts, load }
}
