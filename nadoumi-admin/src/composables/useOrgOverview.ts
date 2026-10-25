import { ref } from 'vue'
import { getOrgOverview, type OrgOverview } from '@/api/org'

type Kind = keyof OrgOverview

/** Head counts for the roles, departments and posts screens. A failed load leaves the counts empty rather than breaking the page. */
export function useOrgOverview() {
  const overview = ref<OrgOverview>({ roles: {}, departments: {}, posts: {} })

  async function load() {
    try {
      overview.value = await getOrgOverview()
    }
    catch {
      overview.value = { roles: {}, departments: {}, posts: {} }
    }
  }

  const members = (kind: Kind, id: number | string): number => overview.value[kind][String(id)] ?? 0

  return { overview, load, members }
}
